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
package scriptella.driver.csv;

import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.Connection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.DialectIdentifier;

/**
 * Scriptella driver for CSV files.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends AbstractScriptellaDriver {
    static {
        try {
            Class.forName("au.com.bytecode.opencsv.CSVReader");
        } catch (ClassNotFoundException e) {
            throw new CsvProviderException("opencsv library not found on classpath. " +
                    "Check if connection classpath attribute points to opencsv.jar", e);
        }
    }

    static final DialectIdentifier DIALECT = new DialectIdentifier("CSV", "1.0");
    public Connection connect(ConnectionParameters connectionParameters) {
        return new CsvConnection(connectionParameters);
    }
}
