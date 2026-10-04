/*
 * Copyright 2006-2026 The Scriptella Project Team.
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
package scriptella.driver.sqlite;

import scriptella.jdbc.GenericDriver;

/**
 * SQLite adapter using the externally supplied Xerial JDBC driver.
 * See <a href="package-summary.html">the package overview</a> for configuration
 * and numeric precision limitations.
 */
public class Driver extends GenericDriver {
    public static final String SQLITE_DRIVER_NAME = "org.sqlite.JDBC";

    public Driver() {
        loadDrivers(SQLITE_DRIVER_NAME);
    }
}
