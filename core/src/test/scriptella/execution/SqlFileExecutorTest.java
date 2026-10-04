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
package scriptella.execution;

import junit.framework.TestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class SqlFileExecutorTest extends TestCase {
    public void testFactoryReturnsReusableNormalExecutor() throws Exception {
        String key = "scriptella.sql.test.value";
        String previous = System.getProperty(key);
        Path file = Files.createTempFile("scriptella-sql-factory", ".sql");
        Class.forName("org.h2.Driver");
        try (Connection observer = DriverManager.getConnection("jdbc:h2:mem:sqlFactory", "sa", "")) {
            observer.createStatement().executeUpdate("CREATE TABLE deployment(payload VARCHAR)");
            System.setProperty(key, "snapshot");
            EtlExecutor executor = EtlExecutor.newSqlFileExecutor(file.toFile(), "jdbc:h2:mem:sqlFactory", "sa", "");
            assertEquals(EtlExecutor.class, executor.getClass());
            assertEquals(1, executor.getConfiguration().getConnections().size());
            assertEquals(1, executor.getConfiguration().getScriptingElements().size());
            assertEquals("snapshot", executor.getConfiguration().getParameters().getParameter(key));
            System.setProperty(key, "changed");
            // The file is loaded at execution time; property values are captured by the factory.
            Files.writeString(file, "INSERT INTO deployment VALUES ('${" + key + "}');");
            executor.setJmxEnabled(true);
            assertEquals(1L, executor.execute().getUpdateCount());
            Files.writeString(file, "INSERT INTO deployment VALUES ('café');");
            assertEquals(1L, executor.call().getUpdateCount());
            try (ResultSet rows = observer.createStatement().executeQuery("SELECT payload FROM deployment")) {
                assertTrue(rows.next());
                assertEquals("snapshot", rows.getString(1));
                assertTrue(rows.next());
                assertEquals("café", rows.getString(1));
                assertFalse(rows.next());
            }
            Thread.currentThread().interrupt();
            try {
                executor.execute();
                fail("Expected cancellation");
            } catch (EtlExecutorException expected) {
                assertTrue(expected.isCancelled());
            } finally {
                Thread.interrupted();
            }
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
            Files.deleteIfExists(file);
        }
    }

    public void testSubstitutionCountsLiteralAndRollback() throws Exception {
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:mem:directSql";
        String previous = System.getProperty("environment");
        System.setProperty("environment", "staging");
        Path file = Files.createTempFile("scriptella-sql", ".sql");
        try (Connection observer = DriverManager.getConnection(url, "sa", "")) {
            Files.writeString(file, "CREATE TABLE deployment(payload VARCHAR);\n" +
                    "INSERT INTO deployment VALUES (?environment);\n" +
                    "INSERT INTO deployment VALUES ('${environment}');");
            ExecutionStatistics result = EtlExecutor.newSqlFileExecutor(file.toFile(), url, "sa", "").execute();
            assertEquals(3L, result.getExecutedStatementsCount());
            assertEquals(2L, result.getUpdateCount());
            try (ResultSet rows = observer.createStatement().executeQuery("SELECT payload FROM deployment")) {
                assertTrue(rows.next());
                assertEquals("staging", rows.getString(1));
                assertTrue(rows.next());
                assertEquals("staging", rows.getString(1));
            }
            Files.writeString(file, "INSERT INTO deployment VALUES ('${environment} ?name');");
            System.setProperty("environment", "changed");
            EtlExecutor.newSqlFileExecutor(file.toFile(), url, "sa", "", null, false).execute();
            try (ResultSet rows = observer.createStatement().executeQuery(
                    "SELECT payload FROM deployment WHERE payload LIKE '$%'")) {
                assertTrue(rows.next());
                assertEquals("${environment} ?name", rows.getString(1));
            }
            Files.writeString(file, "INSERT INTO deployment VALUES ('rolled back'); INVALID SQL;");
            try {
                EtlExecutor.newSqlFileExecutor(file.toFile(), url, "sa", "").execute();
                fail("Expected SQL failure");
            } catch (EtlExecutorException expected) {
                try (ResultSet rows = observer.createStatement().executeQuery("SELECT COUNT(*) FROM deployment")) {
                    assertTrue(rows.next());
                    assertEquals(3, rows.getInt(1));
                }
            }
        } finally {
            if (previous == null) {
                System.clearProperty("environment");
            } else {
                System.setProperty("environment", previous);
            }
            Files.deleteIfExists(file);
        }
    }
}
