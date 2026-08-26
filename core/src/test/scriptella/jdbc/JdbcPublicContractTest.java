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
package scriptella.jdbc;

import scriptella.DBTestCase;
import scriptella.execution.EtlExecutorException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Public, H2-only contract tests for shared JDBC behavior.
 */
public class JdbcPublicContractTest extends DBTestCase {
    public void testParameterBindingAndQueryValueFlow() throws Exception {
        final Connection setupConnection = getConnection("jdbc_public_value_flow");
        execute(setupConnection,
                "CREATE TABLE Source (ID INT, NAME VARCHAR(100), AMOUNT DECIMAL(10, 2))",
                "CREATE TABLE Target (ID INT, NAME VARCHAR(100), AMOUNT DECIMAL(10, 2))",
                "INSERT INTO Source VALUES (1, 'München', 12.50)",
                "INSERT INTO Source VALUES (2, 'O''Brien', -3.75)");
        setupConnection.commit();

        newEtlExecutor("JdbcPublicContractValueFlowTest.xml").execute();

        try (Statement statement = setupConnection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT ID, NAME, AMOUNT FROM Target ORDER BY ID")) {
            assertRow(rows, 1, "München", "12.50");
            assertRow(rows, 2, "O'Brien", "-3.75");
            assertFalse("Only the queried source rows should be inserted", rows.next());
        }
    }

    public void testSuccessfulTransactionIsVisibleThroughNewConnection() throws Exception {
        final Connection setupConnection = getConnection("jdbc_public_commit");
        execute(setupConnection, "CREATE TABLE ContractResult (ID INT PRIMARY KEY, VALUE VARCHAR(100))");
        setupConnection.commit();

        newEtlExecutor("JdbcPublicContractCommitTest.xml").execute();

        final Connection verificationConnection = getConnection("jdbc_public_commit");
        assertNotSame(setupConnection, verificationConnection);
        assertEquals("committed", queryString(verificationConnection,
                "SELECT VALUE FROM ContractResult WHERE ID = 1"));
    }

    public void testFailingTransactionRollsBackPartialWork() throws Exception {
        final Connection setupConnection = getConnection("jdbc_public_rollback");
        execute(setupConnection, "CREATE TABLE ContractResult (ID INT PRIMARY KEY, VALUE VARCHAR(100))");
        setupConnection.commit();

        try {
            newEtlExecutor("JdbcPublicContractRollbackTest.xml").execute();
            fail("The duplicate primary key should fail the ETL execution");
        } catch (EtlExecutorException expected) {
            assertTrue("The failure should prove that the first insert succeeded",
                    hasSqlState(expected, "23505"));
        }

        final Connection verificationConnection = getConnection("jdbc_public_rollback");
        assertEquals(0, queryInt(verificationConnection,
                "SELECT COUNT(*) FROM ContractResult"));
    }

    private static void assertRow(final ResultSet rows, final int id,
                                  final String name, final String amount) throws SQLException {
        assertTrue("Expected row " + id, rows.next());
        assertEquals(id, rows.getInt("ID"));
        assertEquals(name, rows.getString("NAME"));
        assertEquals(new BigDecimal(amount), rows.getBigDecimal("AMOUNT"));
    }

    private static void execute(final Connection connection, final String... sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String command : sql) {
                statement.execute(command);
            }
        }
    }

    private static int queryInt(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String queryString(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static boolean hasSqlState(final Throwable failure, final String expectedSqlState) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException
                    && expectedSqlState.equals(((SQLException) cause).getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
