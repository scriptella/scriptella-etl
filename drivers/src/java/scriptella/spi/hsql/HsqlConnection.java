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
package scriptella.spi.hsql;

import scriptella.jdbc.JDBCConnection;
import scriptella.jdbc.JDBCUtils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hsqldb connection wrapper.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class HsqlConnection extends JDBCConnection {
    private static final Logger LOG = Logger.getLogger(HsqlConnection.class.getName());
    private boolean shutdownOnExit;

    public HsqlConnection(Connection con, boolean shutdownOnExit) {
        super(con);
        this.shutdownOnExit = shutdownOnExit;
    }

    void shutdown() {
        assert shutdownOnExit;
        try {
            Connection con = getNativeConnection();
            assert con != null; //we are going to close, so con!=null
            if (con.isClosed()) {
                LOG.info("Unable to correctly shutdown in-process HSQLDB. Connection has already already been closed");
            }
            Statement st = con.createStatement();
            st.execute("SHUTDOWN");
            JDBCUtils.closeSilent(st);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Problem occured while trying to shutdown in-process HSQLDB", e);
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


}
