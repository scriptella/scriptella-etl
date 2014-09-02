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
package scriptella.core;

import scriptella.AbstractTestCase;

import java.util.logging.Logger;

/**
 * Tests for {@link scriptella.core.DriverFactory}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DriverFactoryTest extends AbstractTestCase {
    private static final Logger logger = Logger.getLogger(DriverFactoryTest.class.getName());
    public static final String SUN_JDBC_ODBC_JDBC_ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";
    private static boolean skipJdbcOdbc;

    static {
        try {
            Class.forName(SUN_JDBC_ODBC_JDBC_ODBC_DRIVER);
        } catch (ClassNotFoundException e) {
            skipJdbcOdbc = true;
            logger.warning(SUN_JDBC_ODBC_JDBC_ODBC_DRIVER + " not available starting from JDK8 - skipping related tests");
        }
    }

    /**
     * Tests correct handling of drivers in bootstrap classpath.
     */
    public void testGetBootstrapDriver() throws ClassNotFoundException {
        if (!skipJdbcOdbc) {
            //JDBC-ODBC
            DriverFactory.getDriver("sun.jdbc.odbc.JdbcOdbcDriver", null);
        }
    }

    /**
     * Tests correct handling of drivers in classpath(e.g. lib directory).
     */
    public void testClassPathDriver() throws ClassNotFoundException {
        DriverFactory.getDriver("org.hsqldb.jdbcDriver", getClass().getClassLoader());
        if (!skipJdbcOdbc) {
            //Bootstrap classes should also be loaded using the classloader scriptella jars.
            DriverFactory.getDriver("sun.jdbc.odbc.JdbcOdbcDriver", getClass().getClassLoader());
        }
    }
}
