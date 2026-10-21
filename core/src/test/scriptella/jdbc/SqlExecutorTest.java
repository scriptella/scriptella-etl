/*
 * Copyright 2026 The Scriptella Project Team.
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
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.Resource;
import scriptella.spi.support.MapParametersCallback;

import java.io.FilterReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SqlExecutorTest extends DBTestCase {
    public void testRepeatedValuesAndPartialBatch() throws Exception {
        for (int batchSize : new int[] {0, 3}) {
            Connection nativeConnection = getConnection("plans" + batchSize);
            nativeConnection.createStatement().execute("create table t(id int, value varchar, duplicate varchar)");
            JdbcConnection connection = connection(nativeConnection, batchSize);
            CountingResource resource = new CountingResource(
                    "insert /* ?ignored */ into t values (?id, ?value, ?value);" +
                    "update t set duplicate=?value where id=?id", true);
            SqlExecutor executor = new SqlExecutor(resource, connection);
            for (int i = 0; i < 5; i++) {
                executor.execute(parameters(i, i == 2 ? null : "row" + i));
            }
            executor.cache.flush(); // The connection normally flushes before closing executors.
            executor.close();
            assertEquals(1, resource.opens);
            Statement statement = nativeConnection.createStatement();
            ResultSet rows = statement.executeQuery("select * from t order by id");
            for (int i = 0; i < 5; i++) {
                assertTrue(rows.next());
                assertEquals(i, rows.getInt(1));
                assertEquals(i == 2 ? null : "row" + i, rows.getString(2));
                assertEquals(rows.getString(2), rows.getString(3));
            }
            assertFalse(rows.next());
            rows.close();
            statement.close();
        }
    }

    public void testFallbackAndNonCachedResource() throws Exception {
        Connection nativeConnection = getConnection("fallbackPlans");
        nativeConnection.createStatement().execute("create table t(id int, value varchar, duplicate varchar)");
        JdbcConnection connection = connection(nativeConnection, 0);
        CountingResource dynamic = new CountingResource(
                "insert into $table values (?{id+1}, '?quoted', ?value)", true);
        SqlExecutor executor = new SqlExecutor(dynamic, connection);
        executor.execute(parameters(0, "first"));
        executor.execute(parameters(1, "second"));
        executor.close();
        assertEquals(1, dynamic.opens);
        CountingResource streaming = new CountingResource("insert into t values (?id, ?value, ?value)", false);
        executor = new SqlExecutor(streaming, connection);
        executor.execute(parameters(3, "third"));
        executor.execute(parameters(4, null));
        executor.close();
        assertEquals(2, streaming.opens);
        ResultSet rows = nativeConnection.createStatement().executeQuery("select * from t order by id");
        assertTrue(rows.next());
        assertEquals(1, rows.getInt(1));
        assertEquals("?quoted", rows.getString(2));
        assertEquals("first", rows.getString(3));
        assertTrue(rows.next());
        assertEquals(2, rows.getInt(1));
        assertEquals("second", rows.getString(3));
        assertTrue(rows.next());
        assertEquals(3, rows.getInt(1));
        assertEquals("third", rows.getString(3));
        assertTrue(rows.next());
        assertEquals(4, rows.getInt(1));
        assertNull(rows.getString(3));
        assertFalse(rows.next());
        rows.close();
    }

    public void testFailureLifecycle() throws Exception {
        Connection nativeConnection = getConnection("failedPlans");
        nativeConnection.createStatement().execute("create table t(id int primary key, value varchar)");
        JdbcConnection connection = connection(nativeConnection, 0);
        CountingResource resource = new CountingResource("insert into t values (?id, ?value)", true);
        SqlExecutor executor = new SqlExecutor(resource, connection);
        ParametersCallback failing = new ParametersCallback() {
            public Object getParameter(String name) {
                if ("value".equals(name)) {
                    throw new IllegalArgumentException("parameter failure");
                }
                return 99;
            }
        };
        try {
            executor.execute(failing);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("parameter failure", expected.getMessage());
        }
        executor.execute(parameters(1, "first"));
        assertEquals(2, resource.opens);
        try {
            executor.execute(parameters(1, "duplicate"));
            fail();
        } catch (JdbcException expected) {
            assertEquals("insert into t values (?, ?). Parameters: [1, duplicate]", expected.getErrorStatement());
        }
        try {
            executor.execute(failing);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("parameter failure", expected.getMessage());
        }
        executor.execute(parameters(2, null));
        assertEquals(2, resource.opens); // Successful parse retained across later failures.
        executor.close();
        ResultSet rows = nativeConnection.createStatement().executeQuery("select * from t order by id");
        assertTrue(rows.next());
        assertEquals(1, rows.getInt(1));
        assertEquals("first", rows.getString(2));
        assertTrue(rows.next());
        assertEquals(2, rows.getInt(1));
        assertNull(rows.getString(2));
        assertFalse(rows.next());
        rows.close();
    }

    public void testFirstJdbcFailureReopensWholeScript() throws Exception {
        Connection nativeConnection = getConnection("firstFailurePlans");
        JdbcConnection connection = connection(nativeConnection, 0);
        CountingResource resource = new CountingResource(
                "insert into t values (?id, ?value); insert into t values (?id, ?value)", true);
        SqlExecutor executor = new SqlExecutor(resource, connection);
        try {
            executor.execute(parameters(0, "missing table"));
            fail();
        } catch (JdbcException expected) {
            assertTrue(expected.getErrorStatement().startsWith("insert into t values (?, ?)"));
        }
        nativeConnection.createStatement().execute("create table t(id int, value varchar)");
        executor.execute(parameters(1, "first"));
        executor.execute(parameters(2, "second"));
        executor.close();
        assertEquals(2, resource.opens);
        ResultSet rows = nativeConnection.createStatement().executeQuery("select count(*) from t");
        assertTrue(rows.next());
        assertEquals(4, rows.getInt(1));
        rows.close();
    }

    public void testSubstitutionDisabled() throws Exception {
        Connection nativeConnection = getConnection("disabledPlans");
        nativeConnection.createStatement().execute("create table t(value varchar)");
        SqlExecutor executor = new SqlExecutor(new CountingResource("insert into t values ('$value ?id')", true),
                connection(nativeConnection, 0));
        executor.setSubstitution(false);
        ParametersCallback unsupported = new ParametersCallback() {
            public Object getParameter(String name) {
                throw new AssertionError("Substitution is disabled");
            }
        };
        executor.execute(unsupported);
        executor.execute(unsupported);
        executor.close();
        ResultSet rows = nativeConnection.createStatement().executeQuery("select value from t");
        for (int i = 0; i < 2; i++) {
            assertTrue(rows.next());
            assertEquals("$value ?id", rows.getString(1));
        }
        assertFalse(rows.next());
        rows.close();
    }

    private JdbcConnection connection(Connection connection, int batchSize) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JdbcConnection.STATEMENT_BATCH_SIZE, batchSize);
        return new JdbcConnection(connection, new MockConnectionParameters(properties, null));
    }

    private ParametersCallback parameters(int id, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("id", id);
        values.put("value", value);
        values.put("table", "t");
        return new MapParametersCallback(values);
    }

    private static class CountingResource implements Resource {
        private final String sql;
        private final boolean cached;
        private int opens;

        CountingResource(String sql, boolean cached) {
            this.sql = sql;
            this.cached = cached;
        }

        public Reader open() {
            opens++;
            StringReader reader = new StringReader(sql);
            return cached ? reader : new FilterReader(reader) { };
        }
    }
}
