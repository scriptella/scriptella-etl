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
package scriptella.driver.postgresql;

import scriptella.jdbc.GenericDriver;
import scriptella.jdbc.JdbcException;

/**
 * Scriptella Adapter for PostgreSQL database.
 *
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * NOTE: Problems during migrating from/to Oracle(and DB2) RDBMS could be occurred due to Not Supported Bit/Boolean 
 * data types equality by Postgre SQL DB
 * 
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String POSTGRESQL_DRIVER_NAME = "org.postgresql.Driver";

    static {
        try {
            Class.forName(POSTGRESQL_DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new JdbcException(POSTGRESQL_DRIVER_NAME + " driver were not found. Please check class path settings", e);
        }
    }

}
