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

import scriptella.configuration.*;
import scriptella.execution.ScriptsContext;
import scriptella.interactive.ProgressCallback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A helper class for running sql elements.
 * <p>Available from {@link ScriptsContext}
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLEngine {
    private static ClassLoader driversClassLoader;
    Map<String, ConnectionFactory> connections = new HashMap<String, ConnectionFactory>();
    private List<SQLExecutableElement> executors;
    private List<Location> locations;

    void registerConnection(final String id, final ConnectionFactory con) {
        connections.put(id, con);
    }

    ConnectionFactory getConnection(final String id) {
        if (connections.size() == 1) {
            return connections.values().iterator().next();
        } else {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Connection id must be specified");
            }

            return connections.get(id);
        }
    }

    public static ClassLoader getDriversClassLoader() {
        return driversClassLoader;
    }

    public static void setDriversClassLoader(
            final ClassLoader driversClassLoader) {
        SQLEngine.driversClassLoader = driversClassLoader;
    }

    public void init(final ConfigurationEl configuration,
                     final ScriptsContext ctx) {
        final List<ConnectionEl> connections = configuration.getConnections();

        for (ConnectionEl c : connections) {
            c.setCatalog(ctx.substituteProperties(c.getCatalog()));
            c.setDriver(ctx.substituteProperties(c.getDriver()));
            c.setPassword(ctx.substituteProperties(c.getPassword()));
            c.setUser(ctx.substituteProperties(c.getUser()));
            c.setSchema(ctx.substituteProperties(c.getSchema()));
            c.setUrl(ctx.substituteProperties(c.getUrl()));

            final ConnectionFactory con = new ConnectionFactory(c);

            if (driversClassLoader != null) {
                con.setDriverClassLoader(driversClassLoader);
            }

            con.getConnection();
            registerConnection(c.getId(), con);
        }

        ctx.getProgressCallback().step(5, "Registering database connections");

        ctx.getProgressCallback().step(5, "Parsing row sets");

        final List<SQLBasedElement> scripts = configuration.getSqlElements();
        executors = new ArrayList<SQLExecutableElement>(scripts.size());
        locations = new ArrayList<Location>(scripts.size());

        for (SQLBasedElement s : scripts) {
            locations.add(s.getLocation());

            if (s instanceof QueryEl) {
                executors.add(QueryExecutor.prepare((QueryEl) s));
            } else if (s instanceof ScriptEl) {
                executors.add(ScriptExecutor.prepare((ScriptEl) s));
            }
        }
    }

    public void execute(final ScriptsContext ctx) {
        final ProgressCallback progress = ctx.getProgressCallback()
                .fork(executors.size());
        SQLContext sqlContext = new SQLContext(ctx);

        for (int i = 0, n = executors.size(); i < n; i++) {
            SQLExecutableElement exec = executors.get(i);
            exec.execute(sqlContext);
            progress.step(1, locations.get(i).toString());
        }
    }

    public void close() {
        if (connections != null) {
            for (ConnectionFactory connectionFactory : connections.values()) {
                connectionFactory.close();
            }

            connections = null;
        }
    }

    public void commit() {
        if (connections != null) {
            for (ConnectionFactory connectionFactory : connections.values()) {
                if (connectionFactory != null) {
                    try {
                        connectionFactory.getConnection().commit();
                    } catch (SQLException e) {
                        throw new JDBCException("Unable to commit tx", e);
                    }
                }
            }
        }
    }

    public void rollback() {
        if (connections != null) {
            for (ConnectionFactory connectionFactory : connections.values()) {
                connectionFactory.rollback();
                connectionFactory.close();
            }
        }
    }
}
