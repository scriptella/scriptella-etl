/*
 * Copyright 2006-2009 The Scriptella Project Team.
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

import scriptella.core.EtlCancelledException;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SQL statements executor.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class SqlExecutor extends SqlParserBase implements Closeable {
    private static final Logger LOG = Logger.getLogger(SqlExecutor.class.getName());
    protected final Resource resource;
    protected final JdbcConnection connection;
    protected final JdbcTypesConverter converter;
    protected final StatementCache cache;
    private QueryCallback callback;
    private ParametersCallback paramsCallback;
    private List<Object> params = new ArrayList<Object>();
    private int updateCount;//number of updated rows
    private final AbstractConnection.StatementCounter counter;
    private SqlTokenizer cachedTokenizer;

    public SqlExecutor(final Resource resource, final JdbcConnection connection) {
        this.resource = resource;
        this.connection = connection;
        converter = new JdbcTypesConverter();
        cache = new StatementCache(connection.getNativeConnection(), connection.statementCacheSize);
        counter = connection.getStatementCounter();
    }

    final void execute(final ParametersCallback parametersCallback) {
        execute(parametersCallback, null);
    }

    final void execute(final ParametersCallback parametersCallback, final QueryCallback queryCallback) {
        paramsCallback = parametersCallback;
        callback = queryCallback;
        updateCount = 0;
        SqlTokenizer tok = cachedTokenizer;
        boolean cache = false;
        if (tok==null) { //If not cached
            try {
                final Reader reader = resource.open();
                tok = new SqlReaderTokenizer(reader, connection.separator,
                        connection.separatorSingleLine, connection.keepformat);
                cache = reader instanceof StringReader;
                if (cache) { //If resource is a String - allow caching
                    tok = new CachedSqlTokenizer(tok);
                }
            } catch (IOException e) {
                throw new JdbcException("Failed to open resource", e);
            }
        }
        parse(tok);
        //We should remember cached tokenizer only if all statements were parsed
        //i.e. no errors occured
        if (cache) {
            cachedTokenizer=tok;
        }
        

    }

    int getUpdateCount() {
        return updateCount;
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
        EtlCancelledException.checkEtlCancelled();
        StatementWrapper sw = null;
        try {
            sw = cache.prepare(sql, params, converter);
            int updatedRows = -1;
            if (callback != null) {
                sw.query(callback, paramsCallback);
            } else {
                updatedRows = sw.update();
            }
            logExecutedStatement(sql, params, updatedRows);
            if (connection.autocommitSize > 0 && (counter.statements % connection.autocommitSize == 0)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Committing transaction after " + connection.autocommitSize + " statements");
                }
                connection.commit();
            }
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

    private void logExecutedStatement(final String sql, final List<?> parameters, final int updateCount) {
        counter.statements++;
        if (updateCount > 0) {
            this.updateCount += updateCount;
        }
        SQLWarning warnings = null;
        try {
            warnings = connection.getNativeConnection().getWarnings();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to obtain SQL warnings", e);

        }
        Level level = warnings == null ? Level.FINE : Level.INFO;
        if (warnings != null) { //If warnings present - use INFO priority
            level = Level.INFO;
        }

        if (LOG.isLoggable(level)) {
            StringBuilder sb = new StringBuilder("     Executed statement ");
            sb.append(StringUtils.consoleFormat(sql));
            if (!parameters.isEmpty()) {
                sb.append(", Parameters: ").append(parameters);
            }
            if (updateCount >= 0) {
                sb.append(". Update count: ").append(updateCount);
            }
            if (warnings != null) { //Iterate warnings
                sb.append(". SQL Warnings:");
                do {
                    sb.append("\n * ").append(warnings);
                    warnings = warnings.getNextWarning();
                } while (warnings != null);
                try {
                    connection.getNativeConnection().clearWarnings();
                } catch (Exception e) { //catch everything because drivers may violate rules and throw any exception
                    LOG.log(Level.WARNING, "Failed to clear SQL warnings", e);
                }
            }

            LOG.log(level, sb.toString());
        }
    }


    public void close() {
        cache.close();
    }
}
