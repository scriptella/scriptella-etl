/*
 * Copyright 2006 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.sql;

import scriptella.configuration.ExternalResource;
import scriptella.expressions.Expression;
import scriptella.expressions.FileParameter;
import scriptella.expressions.ParametersCallback;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLSupport {
    private static final Logger LOG = Logger.getLogger(SQLSupport.class.getName());
    protected ExternalResource externalResource;

    public SQLSupport(ExternalResource externalResource) {
        if (externalResource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        this.externalResource = externalResource;
    }

    public SQLSupport(final String sql) {
        this(new ExternalResource() {
            public Reader open() {
                return new StringReader(sql);
            }
        });
    }

    protected int parseAndExecute(final Connection connection,
                                  final SQLContext sqlContext, final QueryCallback callBack) {
        Parser parser = new Parser(connection, callBack, sqlContext);

        try {
            final Reader reader = externalResource.open();
            parser.parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        //notify statistic interceptor on number of executed statements
        //Performance Note: this solution may be retrofitted to avoid usage of ThreadLocals
        StatisticInterceptor.statementsExecuted(parser.executedCount);
        return parser.updates ? parser.result : (-1);
    }

    private class Parser extends SQLParserBase {
        int result = 0;
        boolean updates = false;
        Connection con;
        QueryCallback callback;
        ParametersCallback paramsCallback;
        List params = new ArrayList();
        private List<FileParameter> files;
        private int executedCount;//number of executed statements

        public Parser(Connection con, QueryCallback callback,
                      ParametersCallback params) {
            this.con = con;
            this.callback = callback;
            paramsCallback = params;
        }

        @Override
        protected String handleParameter(final String name,
                                         final boolean expression, boolean jdbcParam) {
            Object p;

            if (expression) {
                p = Expression.compile(name).evaluate(paramsCallback);
            } else {
                p = paramsCallback.getParameter(name);
            }

            if (jdbcParam) { //if insert as prepared stmt parameter
                params.add(p);
                return "?";
            } else { //otherwise return string representation.
                //todo we need to defines rules for toString transformations
                return p == null ? "" : p.toString();
            }
        }

        @Override
        public void statementParsed(final String sql) {
            int v0 = executeStatement(sql);

            if (v0 >= 0) {
                updates = true;
                result += v0;
            }
        }

        int executeStatement(final String sql) {
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement(sql);

                for (int i = 0, n = params.size(); i < n; i++) {
                    Object o = params.get(i);
                    setObject(o, ps, i + 1);
                }
                int updateCount = -1;
                if (ps.execute()) {
                    if (callback == null) {
                        LOG.warning("Missing callback for query with resultset");
                    } else {
                        ResultSetAdapter r = null;
                        try {
                            r = new ResultSetAdapter(ps.getResultSet(), paramsCallback);
                            while (r.next()) {
                                callback.processRow(r);
                            }
                        } finally {
                            if (r != null) {
                                r.close();
                            }
                        }
                    }
                } else {
                    updateCount = ps.getUpdateCount();
                }
                executedCount++;
                return updateCount;
            } catch (SQLException e) {
                throw new JDBCException("Unable to execute statement", e, sql,
                        params);
            } catch (JDBCException e) {
                //if JDBCException has no SQL - attach it
                if (e.getSql() == null) {
                    e.setSql(sql);
                    e.setParameters(params);
                }

                throw e; //rethrow
            } finally {
                releaseStatement(ps);
            }
        }

        private void releaseStatement(final PreparedStatement ps) {
            JDBCUtils.closeSilent(ps);
            params.clear();

            if (files != null) { //Closing used streams

                for (FileParameter fileParameter : files) {
                    fileParameter.close();
                }

                files = null;
            }
        }

        private void setObject(final Object o, final PreparedStatement ps,
                               final int index) throws SQLException {
            //todo Extract conversion strategy
            if (o instanceof FileParameter) {
                FileParameter f = (FileParameter) o;
                f.insert(ps, index);

                if (files == null) {
                    files = new ArrayList<FileParameter>(2);
                }

                files.add(f);
            } else {
                ps.setObject(index, o);
            }
        }
    }
}
