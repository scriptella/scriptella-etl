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
package scriptella.driver.hsqldb;

import scriptella.jdbc.JdbcConnection;
import scriptella.jdbc.JdbcUtils;
import scriptella.spi.ConnectionParameters;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hsqldb connection wrapper.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class HsqlConnection extends JdbcConnection {
    /**
     * True if SHUTDOWN command should be executed before last connection closed. Default value is true.
     * In 1.7.2, in-process databases are no longer closed when the last connection to the database
     * is explicitly closed via JDBC, a SHUTDOWN is required
     */
    public static final String SHUTDOWN_ON_EXIT = "shutdown_on_exit";

    private static final Logger LOG = Logger.getLogger(HsqlConnection.class.getName());
    private boolean shutdownOnExit;

    /**
     * Creates a wrapper for HSQL connection.
     *
     * @param con
     */
    HsqlConnection(Connection con, ConnectionParameters parameters) {
        super(con, parameters);
    }

    @Override
    protected void init(ConnectionParameters parameters) {
        super.init(parameters);
        shutdownOnExit = parameters.getBooleanProperty(SHUTDOWN_ON_EXIT, true)
                && isInprocess(parameters.getUrl());
    }

    void shutdown() {
        assert shutdownOnExit;
        Connection con = getNativeConnection();
        Statement st = null;
        try {
            if (con == null || con.isClosed()) {
                LOG.info("Unable to correctly shutdown in-process HSQLDB. Connection has already already been closed");
                return;
            }
            st = con.createStatement();
            st.execute("SHUTDOWN");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Problem occured while trying to shutdown in-process HSQLDB", e);
        } finally {
            JdbcUtils.closeSilent(st);
            JdbcUtils.closeSilent(con);
        }
    }

    public void close() {
        if (shutdownOnExit) {
            HsqlConnection previous = Driver.setLastConnection(this);
            //discards previous connection
            if (previous != null) {
                previous.shutdownOnExit = false;
                previous.close();
            }
        } else {
            super.close();
        }
    }

    private static boolean isInprocess(String url) {
        //Returning false for server modes
        if (url.startsWith("jdbc:hsqldb:http:")) {
            return false;
        }
        if (url.startsWith("jdbc:hsqldb:https:")) {
            return false;
        }
        if (url.startsWith("jdbc:hsqldb:hsql")) {
            return false;
        }
        return !url.startsWith("jdbc:hsqldb:hsqls");
    }


}
