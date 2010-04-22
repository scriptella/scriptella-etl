/*
 * Copyright 2006-2010 The Scriptella Project Team.
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
package scriptella.driver.cubrid;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for Cubrid database.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Arnia Software
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String CUBRID_DRIVER_NAME = "cubrid.jdbc.driver.CUBRIDDriver";

    public Driver() {
        loadDrivers(CUBRID_DRIVER_NAME);
    }
}
