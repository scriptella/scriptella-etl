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

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Statements cache for {@link JdbcConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class StatementCache implements Closeable {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Logger LOG = Logger.getLogger(StatementCache.class.getName());
    private Map<String, StatementWrapper> map;
    private List<StatementWrapper> disposeQueue = new ArrayList<StatementWrapper>();
    private final Connection connection;

    /**
     * Creates a statement cache for specified connection.
     * @param connection connection to create cache for.
     * @param size cache size, 0 or negative means disable cache.
     */
    public StatementCache(Connection connection, final int size) {
        this.connection = connection;
        if (size > 0) { //if cache is enabled
            this.map = new LinkedHashMap<String, StatementWrapper>(size, DEFAULT_LOAD_FACTOR, true) {
                protected boolean removeEldestEntry(Map.Entry<String, StatementWrapper> eldest) {
                    boolean remove = size() > size;
                    if (remove) {
                        disposeQueue.add(eldest.getValue());
                    }

                    return remove;
                }
            };
        }
    }

    /**
     * Prepares a statement.
     * <p>The sql is used as a key to lookup a {@link StatementWrapper},
     * if cache miss the statement is created and put to cache.
     *
     * @param sql statement SQL.
     * @param params parameters for SQL.
     * @param converter types converter to use.
     * @return a wrapper for specified SQL.
     * @throws SQLException
     * @see StatementWrapper
     */
    public StatementWrapper prepare(final String sql, final List<Object> params, final JdbcTypesConverter converter) throws SQLException {
        StatementWrapper sw = map == null ? null : map.get(sql);

        if (sw == null) { //If not cached
            if (params==null || params.isEmpty()) {
                sw = create(sql, converter);
            } else {
                sw = prepare(sql, converter);
            }
            put(sql, sw);
        } else if (sw instanceof StatementWrapper.Simple) {
            //If simple statement is obtained second time - use prepared to improve performance
            disposeQueue.add(sw);
            put(sql, sw = prepare(sql, converter));
        }
        sw.setParameters(params);
        return sw;
    }

    /**
     * Testable template method to create simple statement
     */
    protected StatementWrapper.Simple create(final String sql, final JdbcTypesConverter converter) throws SQLException {
        return new StatementWrapper.Simple(connection.createStatement(), sql, converter);
    }

    /**
     * Testable template method to create prepared statement
     */
    protected StatementWrapper.Prepared prepare(final String sql, final JdbcTypesConverter converter) throws SQLException {
        return new StatementWrapper.Prepared(connection.prepareStatement(sql), converter);
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
            close(disposeQueue);
        }
    }

    protected void close(Collection<StatementWrapper> list) {
        if (!list.isEmpty()) {
            for (StatementWrapper st: list) {
                st.close();
            }
            list.clear();
        }
    }

    public void close() {
        if (map != null) {
            //closing statements
            close(disposeQueue);
            close(map.values());
            map = null;
            disposeQueue.clear();
        }
    }

}
