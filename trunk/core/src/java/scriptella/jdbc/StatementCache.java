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

import scriptella.util.IOUtils;
import scriptella.util.LRUMap;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static scriptella.util.CollectionUtils.isEmpty;

/**
 * Statements cache for {@link JdbcConnection}.
 * TODO Extract statement handling policy interface and provide 2 implementations for normal and batched mode. (+1 for testing)
 * TODO Use wrapper class for Jdbc ConnectionParameters to store typesafe parameters and overridable factories
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class StatementCache implements Closeable {
    private Map<String, StatementWrapper> map;
    private final Connection connection;
    private final JdbcTypesConverter converter;
    private int batchSize;
    private StatementWrapper.Batched sharedBatchedStatement; //see getter for description
    private int fetchSize;

    /**
     * Creates a statement cache for specified connection.
     *
     * @param connection connection to create cache for.
     * @param size       cache size, 0 or negative means disable cache.
     * @param batchSize  size of prepared statements batch.
     * @param fetchSize  see {@link java.sql.Statement#setFetchSize(int)}
     */
    public StatementCache(Connection connection, final int size, final int batchSize, final int fetchSize) {
        this.connection = connection;
        this.batchSize = batchSize;
        this.converter = new JdbcTypesConverter();
        this.fetchSize = fetchSize;
        if (size > 0) { //if cache is enabled
            map = new CacheMap(size);
        }
    }

    /**
     * Prepares a statement.
     * <p>The sql is used as a key to lookup a {@link StatementWrapper},
     * if cache miss the statement is created and put to cache.
     *
     * @param sql    statement SQL.
     * @param params parameters for SQL.
     * @return a wrapper for specified SQL.
     * @throws SQLException if DB reports an error
     * @see StatementWrapper
     */
    public StatementWrapper<?> prepare(final String sql, final List<Object> params) throws SQLException {
        //In batch mode always use Batched statement for sql without parameters
        if (isBatchMode() && isEmpty(params)) {
            StatementWrapper.Batched batchedSt = getSharedBatchStatement();
            batchedSt.setSql(sql);
            return batchedSt;
        }
        StatementWrapper<?> sw = map == null ? null : map.get(sql);

        if (sw == null) { //If not cached
            if (isEmpty(params)) {
                sw = create(sql);
            } else {
                sw = prepare(sql);
            }
            put(sql, sw);
        } else if (sw instanceof StatementWrapper.Simple) {
            //if simple statement is obtained second time - use prepared to improve performance
            sw.close(); //closing unused statement
            put(sql, sw = prepare(sql));
        }
        sw.setParameters(params);
        return sw;
    }

    /**
     * Testable template method to create simple statement
     */
    protected StatementWrapper create(final String sql) throws SQLException {
        Statement statement = connection.createStatement();
        if (fetchSize != 0) {
            statement.setFetchSize(fetchSize);
        }
        return new StatementWrapper.Simple(statement, sql, converter);
    }

    /**
     * Testable template method to create prepared statement
     */
    protected StatementWrapper.Prepared prepare(final String sql) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        if (fetchSize != 0) {
            preparedStatement.setFetchSize(fetchSize);
        }
        if (isBatchMode()) {
            return new StatementWrapper.BatchedPrepared(preparedStatement, converter, batchSize);
        } else {
            return new StatementWrapper.Prepared(preparedStatement, converter);
        }
    }

    private boolean isBatchMode() {
        return batchSize > 0;
    }

    /**
     * Returns an instance of StatementWrapper.Batched shared on the instance level.
     * <p>Since each ETL element has its own cache, we are using a shared statement.
     * This is critical in batch mode to allow grouping different statements in one batch.
     *
     * @return instance of shared statement.
     * @throws SQLException if error occurs
     */
    protected StatementWrapper.Batched getSharedBatchStatement() throws SQLException {
        if (sharedBatchedStatement == null) {
            sharedBatchedStatement = new StatementWrapper.Batched(connection.createStatement(), converter, batchSize);
        }
        return sharedBatchedStatement;
    }

    private void put(String key, StatementWrapper entry) {
        if (map != null) {
            map.put(key, entry);
        }
    }

    /**
     * Notifies cache that specified statement is no longer in use.
     * Close method is invoked on statements pending release after removing from cache.
     *
     * @param sw released statement.
     */
    public void releaseStatement(StatementWrapper sw) {
        if (sw == null) {
            throw new IllegalArgumentException("Released statement cannot be null");
        }
        //if caching disabled or simple statement - close it
        if (map == null) {
            sw.close();
        } else {
            sw.clear();
        }
    }

    public void close() {
        if (map != null) {
            //closing statements
            IOUtils.closeSilently(map.values());
            map = null;
        }
    }

    /**
     * Flushes pending batches.
     *
     * @throws SQLException if DB error occurs.
     */
    public void flush() throws SQLException {
        if (isBatchMode()) {
            if (sharedBatchedStatement != null) {
                sharedBatchedStatement.flush();
            }
            if (map != null) {
                for (StatementWrapper sw : map.values()) {
                    sw.flush();
                }
            }
        }
    }

    /**
     * LRU Map implementation for statement cache.
     */
    private static class CacheMap extends LRUMap<String, StatementWrapper> {
        private static final long serialVersionUID = 1;

        public CacheMap(int size) {
            super(size);
        }

        protected void onEldestEntryRemove(Map.Entry<String, StatementWrapper> eldest) {
            eldest.getValue().close();
        }
    }
}
