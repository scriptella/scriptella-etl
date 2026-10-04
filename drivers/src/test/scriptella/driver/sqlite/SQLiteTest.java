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

import scriptella.AbstractTestCase;
import scriptella.core.DriverFactory;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

/** File-backed contract, verified through fresh direct JDBC connections. */
public class SQLiteTest extends AbstractTestCase {
    public void testFileBackedContract() throws Exception {
        Path database = Files.createTempFile("scriptella-sqlite-", ".db");
        try {
            assertTrue(DriverFactory.getDriver("sqlite", getClass().getClassLoader()) instanceof Driver);
            execute("seed.xml", database);
            verifyRows(database, "records");
            execute("roundtrip.xml", database);
            verifyRows(database, "copied_records");
            execute("commit.xml", database);
            verifyTransaction(database);
            try {
                execute("rollback.xml", database);
                fail("Duplicate primary key must fail");
            } catch (EtlExecutorException expected) {
                Throwable cause = expected;
                while (cause != null && !(cause instanceof SQLException)) {
                    cause = cause.getCause();
                }
                assertNotNull("Expected an SQL constraint failure", cause);
                assertEquals(19, ((SQLException) cause).getErrorCode());
            }
            verifyTransaction(database);
            verifyRows(database, "records");
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private void execute(String resource, Path database) throws Exception {
        EtlExecutor.newExecutor(getClass().getResource(resource),
                Collections.<String, Object>singletonMap("db.path", database.toString())).execute();
    }

    private void verifyRows(Path database, String table) throws Exception {
        Long[] integers = {9007199254740993L, Long.MAX_VALUE, Long.MIN_VALUE, null};
        Double[] amounts = {123.125, -0.5, 0.0, null};
        String[] labels = {"Hello café — 東京 😀", "naïve Ελληνικά", "", null};
        String[] notes = {null, "", "ordinary text", null};
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id")) {
            assertEquals("3.53.4.0", connection.getMetaData().getDriverVersion());
            for (int i = 0; i < integers.length; i++) {
                assertTrue(rows.next());
                assertEquals(i + 1, rows.getInt(1));
                long integer = rows.getLong(2);
                assertEquals(integers[i], rows.wasNull() ? null : Long.valueOf(integer));
                double amount = rows.getDouble(3);
                assertEquals(amounts[i], rows.wasNull() ? null : Double.valueOf(amount));
                assertEquals(labels[i], rows.getString(4));
                assertEquals(notes[i], rows.getString(5));
            }
            assertFalse(rows.next());
        }
    }

    private void verifyTransaction(Path database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM tx_probe ORDER BY id")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt(1));
            assertEquals("committed", rows.getString(2));
            assertFalse("Partial write must be rolled back", rows.next());
        }
    }
}
