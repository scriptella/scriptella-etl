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
package scriptella.driver.mssql;

import scriptella.jdbc.GenericDriver;
import scriptella.jdbc.JdbcException;

/**
 * Scriptella Adapter for MSSQL database.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String MSSQL_2005_DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String MSSQL_2000_DRIVER_NAME = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    public static final String MSSQL_TDS_DRIVER_NAME = "net.sourceforge.jtds.jdbc.Driver";

    static {
        //trying to initialize by turn known MSSQL drivers
        try {
            Class.forName(MSSQL_2005_DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(MSSQL_2000_DRIVER_NAME);
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName(MSSQL_TDS_DRIVER_NAME);
                } catch (ClassNotFoundException e2) {
                    throw new JdbcException("Couldn't find corresponding jdbc driver for MS SQL. Please check class path settings", e);
                }
            }
        }
    }

}
