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

import scriptella.AbstractTestCase;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockConnectionParameters;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Collections;
import java.util.Properties;

/**
 * Tests actionable and credential-safe JDBC driver diagnostics.
 */
public class GenericDriverTest extends AbstractTestCase {

    public void testSingleMissingDriver() {
        JdbcException exception = expectLoadFailure("missing.jdbc.Driver");
        assertTrue(exception.getMessage().contains("missing.jdbc.Driver"));
        assertTrue(exception.getMessage().contains("connection classpath"));
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    public void testMultipleMissingDriversAndFallback() {
        JdbcException exception = expectLoadFailure("missing.jdbc.First", "missing.jdbc.Second");
        assertTrue(exception.getMessage().contains("missing.jdbc.First"));
        assertTrue(exception.getMessage().contains("missing.jdbc.Second"));

        new TestDriver().load("missing.jdbc.Driver", String.class.getName());
    }

    public void testBrokenDriverPreservesCause() {
        JdbcException exception = expectLoadFailure(BrokenDriver.class.getName());
        assertTrue(exception.getMessage().contains(BrokenDriver.class.getName()));
        assertTrue(exception.getMessage().contains("initialized or linked"));
        assertTrue(exception.getCause() instanceof ExceptionInInitializerError);
    }

    public void testVmErrorIsNotReportedAsMissingDriver() {
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(original) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("unrecoverable.jdbc.Driver".equals(name)) {
                    throw new OutOfMemoryError("test VM error");
                }
                return super.loadClass(name, resolve);
            }
        });
        try {
            new TestDriver().load("unrecoverable.jdbc.Driver");
            fail("OutOfMemoryError expected");
        } catch (OutOfMemoryError expected) {
            assertEquals("test VM error", expected.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public void testUrlRejectionIsCredentialSafeAndPreservesSqlChain() {
        String password = "connection-password";
        String token = "private-token";
        String url = "jdbc:example://db.invalid/test?password=url-password&token=" + token;
        SQLException rejected = new SQLTransientConnectionException(
                "Rejected password=url-password token=" + token + " password=" + password,
                "08001", 101, new IOException("TLS authentication token=" + token));
        rejected.setNextException(new SQLException("token=" + token, "08002", 102));
        RejectingDriver driver = new RejectingDriver(rejected);

        try {
            driver.connect(new SensitiveParameters(url, password));
            fail("JdbcException expected");
        } catch (JdbcException e) {
            assertTrue(e.getMessage().contains("jdbc:example:"));
            assertTrue(e.getMessage().contains("may not support this URL"));
            assertCredentialSafe(e.getMessage(), url, password, token);

            SQLException cause = (SQLException) e.getCause();
            assertTrue(cause instanceof SQLTransientConnectionException);
            assertEquals("08001", cause.getSQLState());
            assertEquals(101, cause.getErrorCode());
            assertCredentialSafe(cause.getMessage(), url, password, token);
            assertNotNull(cause.getCause());
            assertTrue(cause.getCause() instanceof IOException);
            assertTrue(cause.getCause().getMessage().contains("TLS authentication"));
            assertCredentialSafe(cause.getCause().getMessage(), url, password, token);
            assertNotNull(cause.getNextException());
            assertEquals("08002", cause.getNextException().getSQLState());
            assertEquals(102, cause.getNextException().getErrorCode());
            assertCredentialSafe(cause.getNextException().getMessage(), url, password, token);
        }
    }

    private JdbcException expectLoadFailure(String... drivers) {
        try {
            new TestDriver().load(drivers);
            fail("JdbcException expected");
            return null;
        } catch (JdbcException e) {
            return e;
        }
    }

    private void assertCredentialSafe(String message, String... sensitiveValues) {
        for (String sensitiveValue : sensitiveValues) {
            assertFalse(message.contains(sensitiveValue));
        }
    }

    private static class TestDriver extends GenericDriver {
        void load(String... drivers) {
            loadDrivers(drivers);
        }
    }

    private static class RejectingDriver extends GenericDriver {
        private final SQLException rejection;

        RejectingDriver(SQLException rejection) {
            this.rejection = rejection;
        }

        @Override
        protected JdbcConnection connect(ConnectionParameters parameters, Properties props) throws SQLException {
            throw rejection;
        }
    }

    private static class SensitiveParameters extends MockConnectionParameters {
        private final String password;

        SensitiveParameters(String url, String password) {
            super(Collections.<String, Object>emptyMap(), url);
            this.password = password;
        }

        @Override
        public String getPassword() {
            return password;
        }
    }

    private static class BrokenDriver {
        static {
            failInitialization();
        }

        private static void failInitialization() {
            throw new IllegalStateException("broken driver");
        }
    }
}
