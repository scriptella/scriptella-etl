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
package scriptella.jdbc;

import scriptella.core.StatisticInterceptor;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SqlSupport implements Closeable {
    private static final Logger LOG = Logger.getLogger(SqlSupport.class.getName());
    protected final Resource resource;
    protected final JdbcConnection connection;
    protected final JdbcTypesConverter converter;
    protected final StatementCache cache;

    public SqlSupport(Resource resource, JdbcConnection connection) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        this.resource = resource;
        this.connection = connection;
        converter = connection.newConverter();
        cache = connection.newStatementCache();
    }


    protected int parseAndExecute(final Connection connection,
                                  final ParametersCallback parametersCallback, final QueryCallback queryCallback) {
        Parser parser = new Parser(connection, queryCallback, parametersCallback);

        try {
            final Reader reader = resource.open();
            parser.parse(reader);
        } catch (IOException e) {
            throw new JdbcException("Failed to open resource", e);
        }
        //notify statistic interceptor on number of executed statements
        //Performance Note: this solution may be retrofitted to avoid usage of ThreadLocals
        StatisticInterceptor.statementsExecuted(parser.executedCount);
        return parser.updates ? parser.result : (-1);
    }

    private class Parser extends SqlParserBase {
        int result = 0;
        boolean updates = false;
        Connection con;
        QueryCallback callback;
        ParametersCallback paramsCallback;
        List<Object> params = new ArrayList<Object>();
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
                p = connection.getParametersParser().evaluate(name, paramsCallback);
            } else {
                p = paramsCallback.getParameter(name);
            }

            if (jdbcParam) { //if insert as prepared stmt parameter
                params.add(p);
                return "?";
            } else { //otherwise return string representation.
                //todo we need to defines rules for toString transformations
                return p == null ? super.handleParameter(name, expression, jdbcParam) : p.toString();
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
            StatementWrapper sw = null;
            try {
                sw = cache.prepare(sql, params, converter);
                int updateCount = -1;
                if (callback != null) {
                    sw.query(callback, paramsCallback);
                } else {
                    updateCount = sw.update();
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("     Statement" + sql.trim() +
                            (params.isEmpty() ? "" : ", Parameters: " + params) +
                            " executed. " + (updateCount >= 0 ? "Update count=" + updateCount : ""));
                }
                executedCount++;
                return updateCount;
            } catch (SQLException e) {
                throw new JdbcException("Unable to execute statement", e, sql, params);
            } catch (JdbcException e) {
                //if ProviderException has no SQL - attach it
                if (e.getErrorStatement() == null) {
                    e.setErrorStatement(sql, params);
                }
                throw e; //rethrow
            } finally {
                params.clear();
                if (sw != null) {
                    cache.releaseStatement(sw);
                }
            }

        }


    }

    public void close() {
        cache.close();
    }
}
