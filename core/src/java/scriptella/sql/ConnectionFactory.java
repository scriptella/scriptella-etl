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

import scriptella.configuration.ConfigurationException;
import scriptella.configuration.ConnectionEl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionFactory {
    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());
    Connection connection;
    List<Connection> newConnections;
    private ConnectionEl connectionEl;
    private Driver driver;
    private DialectIdentifier dialectIdentifier;

    public ConnectionFactory(ConnectionEl connection) {
        connectionEl = connection;
    }

    public void setDriverClassLoader(final ClassLoader driverClassLoader) {
        try {
            driver = ((Class<Driver>) driverClassLoader.loadClass(connectionEl.getDriver())).newInstance();
        } catch (Exception e) {
            throw new ConfigurationException("Unable to load driver: " + connectionEl.getDriver(), e);
        }
    }

    private Connection getConnection(final ConnectionEl connectionEl) {
        try {
            if (driver != null) {
                Properties info = new Properties();
                final String u = connectionEl.getUser();

                if (u != null) {
                    info.put("user", u);
                }

                final String p = connectionEl.getPassword();

                if (p != null) {
                    info.put("password", p);
                }

                final Connection c = driver.connect(connectionEl.getUrl(), info);
                c.setAutoCommit(false);

                return c;
            } else {
                Class.forName(connectionEl.getDriver());

                final Connection c = DriverManager.getConnection(connectionEl.getUrl(),
                        connectionEl.getUser(), connectionEl.getPassword());
                c.setAutoCommit(false);

                return c;
            }
        } catch (SQLException e) {
            throw new JDBCException("Unable to obtain connection for " +
                    connectionEl + ": " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new JDBCException("Driver " + connectionEl.getDriver() + " could not be found", e);

        }
    }

    public Connection getConnection() {
        if (connection == null) {
            connection = getConnection(connectionEl);
        }

        return connection;
    }

    public DialectIdentifier getDialectIdentifier() {
        if (dialectIdentifier == null) {
            try {
                final DatabaseMetaData metaData = getConnection().getMetaData();
                if (metaData != null) { //Several drivers violate spec and return null
                    dialectIdentifier = new DialectIdentifier(metaData.getDatabaseProductName(),
                            metaData.getDatabaseProductVersion());
                }
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
        }
        return dialectIdentifier;
    }

    public Connection newConnection() {
        final Connection c = getConnection(connectionEl);

        if (newConnections == null) {
            newConnections = new ArrayList<Connection>();
        }

        newConnections.add(c);

        return c;
    }

    public String getCatalog() {
        return connectionEl.getCatalog();
    }

    public String getSchema() {
        return connectionEl.getSchema();
    }

    public List<String> getTables() {
        try {
            return JDBCUtils.getColumn(getConnection().getMetaData()
                    .getTables(getCatalog(),
                    getSchema(), null, new String[]{"TABLE"}), 3);
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public Set<String> getTableColumns(final String tableName) {
        try {
            return new HashSet<String>(JDBCUtils.getColumn(
                    getConnection().getMetaData()
                            .getColumns(getCatalog(), getSchema(), tableName, null),
                    4));
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void rollback() {
        List<Connection> cl = new ArrayList<Connection>();

        if (newConnections != null) {
            cl.addAll(newConnections);
        }

        cl.add(connection);

        for (Connection c : cl) {
            try {
                c.rollback();
            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Unable to rollback transaction for connection " +
                                connectionEl.getId());
            }
        }
    }

    public void close() {
        if (connection != null) {
            JDBCUtils.closeSilent(connection);
        }

        if (newConnections != null) {
            for (Connection c : newConnections) {
                JDBCUtils.closeSilent(c);
            }
        }

        connection = null;
        connectionEl = null;
        newConnections = null;
    }
}
