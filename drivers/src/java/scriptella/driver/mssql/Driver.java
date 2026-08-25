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
 * Uses the current Microsoft JDBC Driver for SQL Server,
 * <code>com.microsoft.sqlserver.jdbc.SQLServerDriver</code>.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String MSSQL_DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    /**
     * @deprecated Use {@link #MSSQL_DRIVER_NAME}. The Microsoft driver class
     *             represented by this constant remains current.
     */
    @Deprecated
    public static final String MSSQL_2005_DRIVER_NAME = MSSQL_DRIVER_NAME;

    public Driver() {
        loadDrivers(MSSQL_DRIVER_NAME);
    }

}
