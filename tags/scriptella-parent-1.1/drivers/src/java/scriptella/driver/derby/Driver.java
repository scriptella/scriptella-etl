/*
 * Copyright 2006-2008 The Scriptella Project Team.
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
package scriptella.driver.derby;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for Derby database.
 *
 * This driver looks in classpath for the following drivers(in search order) :
 * <ul>
 * <li>Derby client driver - <code>org.apache.derby.jdbc.ClientDriver</code></li>
 * <li>Derby embedded driver - <code>org.apache.derby.jdbc.EmbeddedDriver</code></li>
 * </ul>
 *
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String DERBY_CLIENT_DRIVER_NAME = "org.apache.derby.jdbc.ClientDriver";
    public static final String DERBY_EMBEDDED_DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";


    public Driver() {
        loadDrivers(DERBY_CLIENT_DRIVER_NAME, DERBY_EMBEDDED_DRIVER_NAME);
    }

}