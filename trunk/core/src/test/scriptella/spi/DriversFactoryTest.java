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
package scriptella.spi;

import scriptella.AbstractTestCase;

/**
 * Tests for {@link DriverFactory}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DriversFactoryTest extends AbstractTestCase {
    /**
     * Tests correct handling of drivers in bootstrap classpath.
     */
    public void testGetBootstrapDriver() throws ClassNotFoundException {
        //JDBC-ODBC
        DriverFactory.getDriver("sun.jdbc.odbc.JdbcOdbcDriver", null);
    }

    /**
     * Tests correct handling of drivers in classpath(e.g. lib directory).
     */
    public void testClassPathDriver() throws ClassNotFoundException {
        DriverFactory.getDriver("org.hsqldb.jdbcDriver", getClass().getClassLoader());
        //Bootstrap classes should also be loaded using the classloader scriptella jars.
        DriverFactory.getDriver("sun.jdbc.odbc.JdbcOdbcDriver", getClass().getClassLoader());
    }


}
