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
package scriptella.jdbc;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.ConnectionParameters;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic adapter for JDBC drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class GenericDriver extends AbstractScriptellaDriver {

    private static final Logger LOG = Logger.getLogger(GenericDriver.class.getName());

    static {
        //Redirects DriverManager's logging
        final Logger LOG = Logger.getLogger("scriptella.DriverManagerLog");
        if (LOG.isLoggable(Level.FINE)) {
            if (DriverManager.getLogWriter() == null) {
                DriverManager.setLogWriter(new PrintWriter(System.out) {
                    public void println(String s) {
                        LOG.fine(s);
                    }
                });

            }
        }
    }

    /**
     * Tries to load one of the specified driver class names.
     *
     * @param drivers database driver candidate names.
     * @throws JdbcException if no drivers were loaded
     */
    protected void loadDrivers(String... drivers) {
        if (drivers.length > 0) {
            final boolean debug = LOG.isLoggable(Level.FINE);
            List<String> missingDrivers = new ArrayList<String>();
            ClassNotFoundException missingCause = null;
            for (String name : drivers) {
                try {
                    loadDriver(name);
                    if (debug) {
                        LOG.fine("Found driver class " + name);
                    }
                    return;
                } catch (ClassNotFoundException e) {
                    missingDrivers.add(name);
                    if (missingCause == null) {
                        missingCause = e;
                    }
                    if (debug) {
                        LOG.log(Level.FINE, "JDBC driver class not found " + name, e);
                    }
                } catch (LinkageError e) {
                    throw brokenDriver(name, e);
                } catch (RuntimeException e) {
                    throw brokenDriver(name, e);
                }
            }
            throw new JdbcException("None of the JDBC driver classes could be found: " + missingDrivers +
                    ". Check the connection classpath.", missingCause);
        }
    }

    private void loadDriver(String name) throws ClassNotFoundException {
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader == null) {
                throw e;
            }
            try {
                Class.forName(name, true, contextLoader);
            } catch (ClassNotFoundException contextException) {
                contextException.addSuppressed(e);
                throw contextException;
            }
        }
    }

    private JdbcException brokenDriver(String name, Throwable cause) {
        return new JdbcException("JDBC driver class " + name +
                " was found but could not be initialized or linked", cause);
    }

    public JdbcConnection connect(ConnectionParameters params) {
        Properties props = new Properties();
        try {
            props.putAll(params.getProperties());
            //according to JDBC spec
            if (params.getUser() != null) {
                props.put("user", params.getUser());
            }
            if (params.getPassword() != null) {
                props.put("password", params.getPassword());
            }
            return connect(params, props);

        } catch (SQLException e) {
            throw new JdbcException("Unable to obtain connection for " + JdbcUtils.getUrlDescription(params.getUrl()) +
                    ". The loaded JDBC driver may not support this URL.", JdbcUtils.sanitize(e, params, props));
        }
    }

    /**
     * Creates Scriptella JDBC connection.
     *
     * @param parameters connection parameters
     * @param props      properties to pass to jdbc driver
     * @return Scriptella JDBC connection.
     * @throws SQLException if DB exception occurs.
     */
    protected JdbcConnection connect(ConnectionParameters parameters, Properties props) throws SQLException {
        return new JdbcConnection(getConnection(parameters.getUrl(), props), parameters);
    }

    /**
     * A helper method for subclasses to avoid direct interaction with DriverManager API.
     * <p>Calls {@link DriverManager#getConnection(String,java.util.Properties)}
     */
    protected Connection getConnection(String url, Properties props) throws SQLException {
        return DriverManager.getConnection(url, props);
    }


}
