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

import junit.framework.TestCase;
import scriptella.util.ProxyAdapter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

public class StatementCacheResourceTest extends TestCase {
    public void testCloseSharedBatchWithoutExecutingIt() throws SQLException {
        for (int cacheSize : new int[]{0, 1}) {
            try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:cacheResources", "sa", "")) {
                try (Statement setup = connection.createStatement()) {
                    setup.executeUpdate("create table test (id integer)");
                }
                StatementCache cache = new StatementCache(connection, cacheSize, 10, 0);
                StatementWrapper<?> wrapper = cache.prepare("insert into test values (1)", Collections.emptyList());
                wrapper.update();
                cache.close();
                cache.close();
                assertTrue(wrapper.statement.isClosed());
                assertFalse(connection.isClosed());
                try (Statement query = connection.createStatement();
                     ResultSet rows = query.executeQuery("select count(*) from test")) {
                    assertTrue(rows.next());
                    assertEquals("Closing must discard pending writes", 0, rows.getInt(1));
                }
            }
        }
    }

    public void testFailedFetchSizeClosesStatement() throws SQLException {
        for (boolean prepared : new boolean[]{false, true}) {
            for (Exception failure : new Exception[]{new SQLException("fetch size rejected"),
                    new UnsupportedOperationException("fetch size unsupported")}) {
                final int[] closed = {0};
                PreparedStatement statement = new ProxyAdapter<PreparedStatement>(PreparedStatement.class) {
                    public void setFetchSize(int size) throws SQLException {
                        if (failure instanceof SQLException) {
                            throw (SQLException) failure;
                        }
                        throw (RuntimeException) failure;
                    }

                    public void close() {
                        closed[0]++;
                    }
                }.getProxy();
                Connection connection = new ProxyAdapter<Connection>(Connection.class) {
                    public Statement createStatement() {
                        return statement;
                    }

                    public PreparedStatement prepareStatement(String sql) {
                        return statement;
                    }
                }.getProxy();
                StatementCache cache = new StatementCache(connection, 1, 0, 100);
                try {
                    cache.prepare("select 1", prepared ? Collections.<Object>singletonList(1)
                            : Collections.emptyList());
                    fail("Expected fetch-size setup failure");
                } catch (SQLException | RuntimeException expected) {
                    assertSame(failure, expected);
                } finally {
                    cache.close();
                }
                assertEquals(1, closed[0]);
            }
        }
    }
}
