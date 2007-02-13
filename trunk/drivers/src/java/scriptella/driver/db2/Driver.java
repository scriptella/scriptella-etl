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
package scriptella.driver.db2;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for DB2 database.
 *
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String DB2_DRIVER_NAME = "com.ibm.db2.jcc.DB2Driver";


    public Driver() {
        loadDrivers(DB2_DRIVER_NAME);
    }

    @Override
    protected String[] getDriverNames() {
        return new String[] {DB2_DRIVER_NAME};
    }
}
