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
package scriptella.driver;

import scriptella.AbstractTestCase;
import scriptella.core.DriverFactory;

/**
 * Tests database adapter aliases and JDBC driver selection without vendor dependencies.
 */
public class DatabaseAdaptersTest extends AbstractTestCase {
    private static String[] candidates;

    public void testAliases() throws ClassNotFoundException {
        ClassLoader loader = getClass().getClassLoader();
        assertEquals(scriptella.driver.mysql.Driver.class, DriverFactory.getDriverClass("mysql", loader));
        assertEquals(scriptella.driver.mariadb.Driver.class, DriverFactory.getDriverClass("mariadb", loader));
        assertEquals(scriptella.driver.postgresql.Driver.class, DriverFactory.getDriverClass("postgresql", loader));
        assertEquals(scriptella.driver.oracle.Driver.class, DriverFactory.getDriverClass("oracle", loader));
        assertEquals(scriptella.driver.mssql.Driver.class, DriverFactory.getDriverClass("mssql", loader));
    }

    public void testMysqlDriverSelection() {
        new CapturingMysqlDriver();
        assertEquals(1, candidates.length);
        assertEquals("com.mysql.cj.jdbc.Driver", candidates[0]);
    }

    public void testMariaDbCandidate() {
        new CapturingMariaDbDriver();
        assertEquals(1, candidates.length);
        assertEquals("org.mariadb.jdbc.Driver", candidates[0]);
    }

    public void testPostgresqlDriverSelection() {
        new CapturingPostgresqlDriver();
        assertEquals(1, candidates.length);
        assertEquals("org.postgresql.Driver", candidates[0]);
    }

    public void testOracleDriverSelection() {
        new CapturingOracleDriver();
        assertEquals(1, candidates.length);
        assertEquals("oracle.jdbc.OracleDriver", candidates[0]);
    }

    public void testSqlServerDriverSelection() {
        new CapturingSqlServerDriver();
        assertEquals(1, candidates.length);
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", candidates[0]);
    }

    private static class CapturingMysqlDriver extends scriptella.driver.mysql.Driver {
        @Override
        protected void loadDrivers(String... drivers) {
            candidates = drivers;
        }
    }

    private static class CapturingMariaDbDriver extends scriptella.driver.mariadb.Driver {
        @Override
        protected void loadDrivers(String... drivers) {
            candidates = drivers;
        }
    }

    private static class CapturingPostgresqlDriver extends scriptella.driver.postgresql.Driver {
        @Override
        protected void loadDrivers(String... drivers) {
            candidates = drivers;
        }
    }

    private static class CapturingOracleDriver extends scriptella.driver.oracle.Driver {
        @Override
        protected void loadDrivers(String... drivers) {
            candidates = drivers;
        }
    }

    private static class CapturingSqlServerDriver extends scriptella.driver.mssql.Driver {
        @Override
        protected void loadDrivers(String... drivers) {
            candidates = drivers;
        }
    }
}
