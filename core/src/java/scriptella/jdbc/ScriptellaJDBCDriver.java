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

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.ConnectionParameters;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptellaJDBCDriver extends AbstractScriptellaDriver {
    public JDBCConnection connect(ConnectionParameters params) {
        if (params.getUrl() == null) {
            throw new IllegalArgumentException("URL parameter is required for JDBC driver connection");
        }
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
            throw new JDBCException("Unable to obtain connection for URL " + params.getUrl(), e);
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
    protected JDBCConnection connect(ConnectionParameters parameters, Properties props) throws SQLException {
        return new JDBCConnection(DriverManager.getConnection(parameters.getUrl(), props));
    }


}
