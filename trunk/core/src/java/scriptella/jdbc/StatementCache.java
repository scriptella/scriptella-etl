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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Statements cache for {@link JDBCConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class StatementCache implements Closeable {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Logger LOG = Logger.getLogger(StatementCache.class.getName());
    private Map<String, StatementWrapper.Prepared> map;
    private List<StatementWrapper.Prepared> disposeQueue = new LinkedList<StatementWrapper.Prepared>();
    private final Connection connection;

    /**
     * Creates a statement cache for specified connection.
     * @param connection connection to create cache for.
     * @param size cache size, 0 or negative means disable cache.
     */
    public StatementCache(Connection connection, final int size) {
        this.connection = connection;
        if (size > 0) { //if cache is enabled
            this.map = new LinkedHashMap<String, StatementWrapper.Prepared>(size, DEFAULT_LOAD_FACTOR, true) {
                protected boolean removeEldestEntry(Map.Entry<String, StatementWrapper.Prepared> eldest) {
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
    public StatementWrapper prepare(final String sql, final List<Object> params, final JDBCTypesConverter converter) throws SQLException {
        if (params==null || params.isEmpty()) {
            return create(sql, converter);
        }
        StatementWrapper.Prepared sw = map == null ? null : map.get(sql);

        if (sw == null) { //If not cached
            put(sql, sw = prepare(sql, converter));
        }
        sw.setParameters(params);
        sw.lock();
        return sw;
    }

    /**
     * Testable template method to create simple statement
     */
    protected StatementWrapper.Simple create(final String sql, final JDBCTypesConverter converter) throws SQLException {
        return new StatementWrapper.Simple(connection.createStatement(), sql, converter);
    }

    /**
     * Testable template method to create prepared statement
     */
    protected StatementWrapper.Prepared prepare(final String sql, final JDBCTypesConverter converter) throws SQLException {
        return new StatementWrapper.Prepared(connection.prepareStatement(sql), converter);
    }


    private void put(String key, StatementWrapper.Prepared entry) {
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

    protected void close(Collection<StatementWrapper.Prepared> list) {
        for (Iterator<StatementWrapper.Prepared> it = list.iterator(); it.hasNext();) {
            StatementWrapper.Prepared sw = it.next();
            if (!sw.isLocked()) { //If statement is not used - close and remove it
                sw.close();
                it.remove();
            }
        }
    }

    public void close() {
        if (map != null) {
            //closing statements
            close(disposeQueue);
            close(map.values());
            if (!disposeQueue.isEmpty() || !map.isEmpty()) {
                List<StatementWrapper> unclosed = new ArrayList<StatementWrapper>(disposeQueue);
                unclosed.addAll(map.values());
                LOG.info("The following statements were not closed because they are in use " + unclosed);
            }
            map = null;
            disposeQueue.clear();
        }
    }

}
