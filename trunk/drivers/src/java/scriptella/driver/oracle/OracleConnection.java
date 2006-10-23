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
package scriptella.driver.oracle;

import scriptella.jdbc.JdbcConnection;
import scriptella.jdbc.JdbcException;
import scriptella.spi.ConnectionParameters;

import java.sql.Connection;
import java.text.ParseException;

/**
 * Oracle connection wrapper.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class OracleConnection extends JdbcConnection {
    /**
     * Name plsql property. A slash on a single line is used as statements separator.
     */
    public static final String PLSQL = "plsql";

    /**
     * Creates a wrapper for HSQL connection.
     *
     * @param con
     */
    OracleConnection(Connection con, ConnectionParameters parameters) {
        super(con, parameters);
    }

    @Override
    protected void init(final ConnectionParameters parameters) {
        super.init(parameters);
        try {
            if (parameters.getBooleanProperty(PLSQL, false)) {
                separator = "/";
                separatorSingleLine = true;
            }
        } catch (ParseException e) {
            throw new JdbcException(e.getMessage());
        }
    }

}
