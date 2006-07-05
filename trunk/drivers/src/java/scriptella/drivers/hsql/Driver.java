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
package scriptella.drivers.hsql;

import scriptella.jdbc.JDBCConnection;
import scriptella.jdbc.ScriptellaJDBCDriver;
import scriptella.spi.ConnectionParameters;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scriptella Adapter for HSLQDB database.
 * <p>The primary feature of this driver is {@link HsqlConnection#SHUTDOWN_ON_EXIT}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends ScriptellaJDBCDriver {
    private static final Logger LOG = Logger.getLogger(Driver.class.getName());
    public static final String HSQLDB_DRIVER_NAME = "org.hsqldb.jdbcDriver";

    private static Map<String, HsqlConnection> lastConnections = null; //Send SHUTDOWN on JVM exit to fix
    private static boolean hookAdded = false;


    /**
     * Shutdown hook closing all the databases being used.
     */
    static final Thread HOOK = new Thread("Scriptella HSLQDB Shutdown Fix") {
        public void run() {
            if (lastConnections != null) {
                for (Map.Entry<String, HsqlConnection> entry : lastConnections.entrySet()) {
                    try {
                        entry.getValue().shutdown();
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Problem occured while trying to shutdown in-process HSQLDB database " + entry.getKey(), e);
                    }
                }
                lastConnections = null;
            }
        }
    };

    static {
        try {
            Class.forName(HSQLDB_DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(HSQLDB_DRIVER_NAME + " driver not found. Please check class path settings");
        }
    }

    @Override
    protected JDBCConnection connect(ConnectionParameters parameters, Properties props) throws SQLException {
        return new HsqlConnection(getConnection(parameters.getUrl(), props), parameters);
    }


    /**
     * Sets last connection and returns previous value of lastConnection for connection url.
     * <p>Driver stores map of connections using URLs as keys.
     *
     * @param connection last connection
     * @return previous value of lastConnection field.Null if no connections have been registered.
     * @see #HOOK
     */
    static synchronized HsqlConnection setLastConnection(HsqlConnection connection) {
        if (lastConnections == null) {
            lastConnections = new HashMap<String, HsqlConnection>();
        }
        final HsqlConnection old = lastConnections.put(getConnectionURL(connection), connection);
        if (!hookAdded) {
            Runtime.getRuntime().addShutdownHook(HOOK);
            hookAdded = true;
        }
        return old;
    }

    private static String getConnectionURL(HsqlConnection connection) {
        try {
            return connection.getNativeConnection().getMetaData().getURL();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to read connection meta data", e);
            return "";
        }
    }


}
