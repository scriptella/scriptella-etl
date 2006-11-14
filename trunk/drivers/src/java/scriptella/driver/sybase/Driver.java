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
package scriptella.driver.sybase;

import scriptella.jdbc.GenericDriver;
import scriptella.jdbc.JdbcException;

/**
 * Scriptella Adapter for Sybase database.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String SYBASE_DRIVER_NAME = "com.sybase.jdbc.SybDriver";
    public static final String SYBASE_JDBC2_DRIVER_NAME = "com.sybase.jdbc2.jdbc.SybDriver";
    public static final String SYBASE_JDBC3_DRIVER_NAME = "com.sybase.jdbc3.jdbc.SybDriver";

    static {
        //trying to initialize by turn Sybase drivers
        try {
            Class.forName(SYBASE_JDBC3_DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(SYBASE_JDBC2_DRIVER_NAME);
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName(SYBASE_DRIVER_NAME);
                } catch (ClassNotFoundException e2) {
                    throw new JdbcException(SYBASE_JDBC3_DRIVER_NAME + " driver were not found. Please check class path settings", e);
                }
            }

        }
    }

}