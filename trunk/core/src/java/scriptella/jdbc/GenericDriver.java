/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for JDBC drivers.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class GenericDriver extends AbstractScriptellaDriver {
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

    public JdbcConnection connect(ConnectionParameters params) {
        try {
            Properties props = new Properties();
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
            throw new JdbcException("Unable to obtain connection for URL " + params.getUrl(), e);
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
        return new JdbcConnection(getConnection(parameters.getUrl(), props), parameters );
    }

    /**
     * A helper method for subclasses to avoid direct interaction with DriverManager API.
     * <p>Calls {@link DriverManager#getConnection(String, java.util.Properties)}
     *
     */
    protected Connection getConnection(String url, Properties props) throws SQLException {
        return DriverManager.getConnection(url, props);
    }


}
