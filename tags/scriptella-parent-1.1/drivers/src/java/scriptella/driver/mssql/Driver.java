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
package scriptella.driver.mssql;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for Microsoft SQL Server database JDBC driver.
 *
 * This driver looks in classpath for the following drivers(in search order) :
 * <ul>
 * <li>Microsoft JDBC Driver for MS SQL Server 2005 - <code>com.microsoft.sqlserver.jdbc.SQLServerDriver</code></li>
 * <li>Microsoft JDBC Driver for MS SQL Server 2000 - <code>com.microsoft.jdbc.sqlserver.SQLServerDriver</code></li>
 * <li>jTDS JDBC Driver for Microsoft SQL Server - <code>net.sourceforge.jtds.jdbc.Driver</code></li>
 * </ul>
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String MSSQL_2005_DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String MSSQL_2000_DRIVER_NAME = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    public static final String MSSQL_TDS_DRIVER_NAME = "net.sourceforge.jtds.jdbc.Driver";

    public Driver() {
        loadDrivers(MSSQL_2005_DRIVER_NAME, MSSQL_2000_DRIVER_NAME, MSSQL_TDS_DRIVER_NAME);
    }

}
