/*
 * Copyright 2006-2012 The Scriptella Project Team.
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

import scriptella.configuration.ConfigurationException;
import scriptella.configuration.ConnectionEl;
import scriptella.execution.EtlContext;
import scriptella.execution.ExecutionProgress;
import scriptella.execution.ExecutionStatistics;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ScriptellaDriver;
import scriptella.text.TypedPropertiesSource;
import scriptella.util.UrlPathTokenizer;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages the primary connection and additional connections
 * used by new-tx scripts. Each new-tx script element caches
 * its connection and reuses it across invocations.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionManager {
    private static final Logger LOG = Logger.getLogger(ConnectionManager.class.getName());
    Connection connection;
    List<Connection> newConnections;
    private boolean closeAfterUse;
    private boolean retired;
    private long retiredStatements;
    private long retiredUpdates;
    private ExecutionProgress progress;
    private int progressIndex;
    private ScriptellaDriver driver;
    ConnectionParameters connectionParameters;
    private final ExecutionStatistics.ConnectionInfo statistics;

    public ConnectionManager(EtlContext ctx, ConnectionEl c) {
        this(ctx, c, null);
    }

    public ConnectionManager(EtlContext ctx, ConnectionEl c, ExecutionStatistics.ConnectionInfo statistics) {
        this.statistics = statistics;
        //Obtains a classloader
        ClassLoader cl = getClass().getClassLoader();
        if (c.getClasspath() != null) { //if classpath specified
            //Parse it and create a new classloader
            UrlPathTokenizer tok = new UrlPathTokenizer(ctx.getScriptFileURL());
            try {
                final URL[] urls = tok.split(c.getClasspath());
                if (urls.length > 0) {
                    cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return new DriverClassLoader(urls);
                        }
                    });
                }
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Unable to parse classpath parameter for " + c, e);
            }
        }
        connectionParameters = new ConnectionParameters(c, ctx);
        if (connectionParameters.getProperty("close.after.use") != null) {
            closeAfterUse = connectionParameters.getBooleanProperty("close.after.use");
        } else {
            closeAfterUse = new TypedPropertiesSource(Collections.singletonMap(
                    "scriptella.close.after.use", ctx.getParameter("scriptella.close.after.use")))
                    .getBooleanProperty("scriptella.close.after.use", false);
        }
        try {
            driver = DriverFactory.getDriver(c.getDriver(), cl);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Driver class " + c.getDriver() + " not found for " + connectionParameters +
                    ".Please check if the class name is correct and required libraries available on classpath", e);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to initialize driver for " + connectionParameters + ":" + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        checkNotRetired();
        if (connection == null) {
            connection = connect();
            if (progress != null) {
                progress.setCounter(progressIndex, connection.getProgressCounter());
            }
        }

        return connection;
    }

    /**
     * Creates a new connection and registers it for transaction
     * management and cleanup. Callers may cache and reuse it.
     */
    public Connection newConnection() {
        checkNotRetired();
        final Connection c = connect();

        if (newConnections == null) {
            newConnections = new ArrayList<Connection>();
        }

        newConnections.add(c);
        if (progress != null) {
            progress.addCounter(c.getProgressCounter());
        }

        return c;
    }

    private Connection connect() {
        long started = statistics == null ? 0 : System.nanoTime();
        try {
            Connection created = driver.connect(connectionParameters);
            if (created == null) {
                throw new ConfigurationException("Driver returned null connection for " + connectionParameters);
            }
            if (statistics != null) {
                statistics.recordOpen(true, System.nanoTime() - started);
            }
            return created;
        } catch (RuntimeException e) {
            if (statistics != null) {
                statistics.recordOpen(false, System.nanoTime() - started);
            }
            throw e;
        } catch (Error e) {
            if (statistics != null) {
                statistics.recordOpen(false, System.nanoTime() - started);
            }
            throw e;
        }
    }

    boolean isCloseAfterUse() {
        return closeAfterUse;
    }

    private void checkNotRetired() {
        if (retired) {
            throw new IllegalStateException("Connection was retired by close.after.use");
        }
    }

    /** Called only after the last top-level subtree has finished successfully. */
    void retire() {
        if (!retired) {
            // On commit failure leave ownership intact for normal rollback/cleanup.
            EtlCancelledException.checkEtlCancelled();
            commit();
            retiredStatements = getExecutedStatementsCount();
            retiredUpdates = getUpdateCount();
            close();
            retired = true;
        }
    }

    void enableProgress(ExecutionProgress progress) {
        this.progress = progress;
        progressIndex = progress.addCounter(connection == null ? null : connection.getProgressCounter());
        if (newConnections != null) {
            for (Connection c : newConnections) {
                progress.addCounter(c.getProgressCounter());
            }
        }
    }

    public void rollback() {
        for (Connection c : getAllConnections()) {
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Rolling back " + c);
                }
                c.rollback();
            } catch (UnsupportedOperationException e) {
                String msg = e.getMessage();
                LOG.log(Level.WARNING,
                        "Unable to rollback transaction for connection " + c + (msg == null ? "" : ": " + msg));

            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Unable to rollback transaction for connection " + c, e);
            }
        }
    }

    public void commit() {
        for (Connection c : getAllConnections()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Commiting connection " + c);
            }
            c.commit();
        }
    }


    public void close() {
        for (Connection c : getAllConnections()) {
            if (c != null) {
                boolean closed = false;
                try {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Closing " + c);
                    }
                    c.close();
                    closed = true;
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Problem occured while trying to close connection " + c, e);
                } finally {
                    if (statistics != null) {
                        statistics.recordClose(closed);
                    }
                }
            }
        }

        connection = null;
        newConnections = null;
        connectionParameters = null;
        driver = null;
    }

    /**
     * Returns number of executed statements by managed connections.
     */
    public long getExecutedStatementsCount() {
        long s = retiredStatements;
        if (connection != null) {
            s += connection.getExecutedStatementsCount();

        }
        if (newConnections != null) {
            for (Connection c : newConnections) {
                s += c.getExecutedStatementsCount();
            }
        }
        return s;
    }

    /** Returns update counts reported by all managed connections. */
    public long getUpdateCount() {
        long count = retiredUpdates;
        for (Connection c : getAllConnections()) {
            count += c.getUpdateCount();
        }
        return count;
    }

    /**
     * @return connection and newtx connections
     */
    private List<Connection> getAllConnections() {
        List<Connection> cl = new ArrayList<Connection>();

        if (newConnections != null) {
            cl.addAll(newConnections);
        }

        if (connection != null) {
            cl.add(connection);
        }
        return cl;
    }


}
