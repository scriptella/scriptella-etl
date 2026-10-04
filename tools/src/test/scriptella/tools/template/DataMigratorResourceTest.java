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
package scriptella.tools.template;

import junit.framework.TestCase;
import scriptella.jdbc.JdbcException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataMigratorResourceTest extends TestCase {
    private TrackingDriver driver;

    @Override
    protected void setUp() throws SQLException {
        driver = new TrackingDriver();
        DriverManager.registerDriver(driver);
    }

    @Override
    protected void tearDown() throws SQLException {
        try {
            if (driver.connection != null) {
                driver.connection.close();
            }
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    public void testClosesConnectionAndReadsColumnsOnce() throws Exception {
        StringWriter xml = new StringWriter();
        migrator(xml, false).create(properties());
        assertTrue(driver.connection.isClosed());
        assertEquals(1, driver.columnQueries);
        assertTrue(xml.toString().contains("INSERT INTO REVIEW_TABLE"));
        assertTrue(xml.toString().contains("?ID"));
    }

    public void testClosesConnectionWhenOutputFails() throws Exception {
        try {
            migrator(new StringWriter(), true).create(properties());
            fail("Expected output failure");
        } catch (IOException expected) {
            assertEquals("output failure", expected.getMessage());
        }
        assertTrue(driver.connection.isClosed());
    }

    public void testClosesConnectionWhenMetadataFails() throws Exception {
        driver.failMetadata = true;
        try {
            migrator(new StringWriter(), false).create(properties());
            fail("Expected metadata failure");
        } catch (JdbcException expected) {
            assertNotNull(expected.getCause());
        }
        assertTrue(driver.connection.isClosed());
    }

    private DataMigrator migrator(StringWriter xml, boolean failOutput) {
        return new DataMigrator() {
            @Override
            protected Writer newFileWriter(String name) throws IOException {
                if (failOutput) {
                    throw new IOException("output failure");
                }
                return name.endsWith(".xml") ? xml : new StringWriter();
            }

            @Override
            protected boolean checkFile(String name) {
                return true;
            }
        };
    }

    private Map<String, String> properties() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("driver", TrackingDriver.class.getName());
        properties.put("url", "jdbc:review:migrator");
        properties.put("schema", "PUBLIC");
        return properties;
    }

    public static class TrackingDriver extends org.h2.Driver {
        Connection connection;
        int columnQueries;
        boolean failMetadata;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!"jdbc:review:migrator".equals(url)) {
                return null;
            }
            connection = DriverManager.getConnection("jdbc:h2:mem:reviewMigrator", "sa", "");
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("create table review_table (id integer)");
            }
            DatabaseMetaData delegate = connection.getMetaData();
            DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{DatabaseMetaData.class}, (proxy, method, args) -> {
                        if ("getColumns".equals(method.getName())) {
                            columnQueries++;
                        }
                        return invoke(delegate, method, args);
                    });
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if ("getMetaData".equals(method.getName())) {
                            if (failMetadata) {
                                throw new SQLException("metadata failure");
                            }
                            return metadata;
                        }
                        return invoke(connection, method, args);
                    });
        }

        private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
