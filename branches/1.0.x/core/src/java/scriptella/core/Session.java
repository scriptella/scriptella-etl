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
package scriptella.core;

import scriptella.configuration.ConfigurationEl;
import scriptella.configuration.ConnectionEl;
import scriptella.configuration.Location;
import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;
import scriptella.configuration.ScriptingElement;
import scriptella.execution.EtlContext;
import scriptella.interactive.ProgressCallback;
import scriptella.spi.Connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A helper class for EtlContext to store and work with connections/executors.
 * <p>Available from {@link EtlContext}
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Session {
    Map<String, ConnectionManager> managedConnections = new HashMap<String, ConnectionManager>();
    private List<ExecutableElement> executors;
    private List<Location> locations;

    public Session(final ConfigurationEl configuration, final EtlContext ctx) {
        final List<ConnectionEl> connections = configuration.getConnections();

        ProgressCallback progressCallback = ctx.getProgressCallback().fork(50, connections.size());
        for (ConnectionEl c : connections) {
            final ConnectionManager con = new ConnectionManager(ctx, c);
            if (c.isLazyInit()) {
                progressCallback.step(1, "Lazy-init specified for connection " + (c.getId() == null ? "" : c.getId()) +
                        " - initialization deferred.");
            } else {
                String id = connections.size() > 1 ? ("id=" + c.getId() + ", ") : "";//print an ID if more than one connection
                Connection connection = con.getConnection();
                progressCallback.step(1, "Connection " + id + connection.toString() +
                        ", " + connection.getDialectIdentifier() + " registered");
            }
            managedConnections.put(c.getId(), con);
        }

        final List<ScriptingElement> scripts = configuration.getScriptingElements();
        progressCallback = ctx.getProgressCallback().fork(50, scripts.size());

        executors = new ArrayList<ExecutableElement>(scripts.size());
        locations = new ArrayList<Location>(scripts.size());

        for (ScriptingElement s : scripts) {
            locations.add(s.getLocation());

            if (s instanceof QueryEl) {
                executors.add(QueryExecutor.prepare((QueryEl) s));
            } else if (s instanceof ScriptEl) {
                executors.add(ScriptExecutor.prepare((ScriptEl) s));
            }
            progressCallback.step(1, s.getLocation() + " prepared");
        }

    }

    ConnectionManager getConnection(final String id) {
        if (managedConnections.size() == 1) {
            return managedConnections.values().iterator().next();
        } else {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Connection id must be specified");
            }

            return managedConnections.get(id);
        }
    }

    public void execute(final EtlContext ctx) {
        final ProgressCallback progress = ctx.getProgressCallback()
                .fork(executors.size());
        DynamicContext dynCtx = new DynamicContext(ctx);

        for (int i = 0, n = executors.size(); i < n; i++) {
            ExecutableElement exec = executors.get(i);
            exec.execute(dynCtx);
            progress.step(1, locations.get(i).toString()+" executed");
        }
    }

    public long getExecutedStatementsCount() {
        long s = 0;
        if (managedConnections != null) {
            for (ConnectionManager connectionManager : managedConnections.values()) {
                s += connectionManager.getExecutedStatementsCount();
            }
        }
        return s;
    }

    public void close() {
        if (managedConnections != null) {
            for (ConnectionManager connectionManager : managedConnections.values()) {
                connectionManager.close();
            }
            managedConnections = null;
        }
    }

    public void commit() {
        if (managedConnections != null) {
            for (ConnectionManager connectionManager : managedConnections.values()) {
                if (connectionManager != null) {
                    connectionManager.commit();
                }
            }
        }
    }

    public void rollback() {
        if (managedConnections != null) {
            for (ConnectionManager connectionManager : managedConnections.values()) {
                connectionManager.rollback();
                connectionManager.close();
            }
        }
    }
}
