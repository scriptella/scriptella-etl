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
package scriptella.tools.launcher;

import junit.framework.TestCase;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlLauncherTest extends TestCase {
    public void testCommonOptionsBeforeAndAfterCommand() throws Exception {
        Path file = Files.createTempFile("scriptella-options", ".sql");
        try {
            Files.writeString(file, "CREATE TABLE deployment(payload VARCHAR); INSERT INTO deployment VALUES ('test');");
            CapturingLauncher quiet = new CapturingLauncher();
            assertEquals(EtlLauncher.ErrorCode.OK, quiet.launch(new String[]{"--quiet", "--no-jmx",
                    "execute-sql", "--url", "jdbc:h2:mem:quiet", file.toString()}));
            assertTrue(quiet.noJmx);
            assertEquals("", quiet.out.toString());

            CapturingLauncher noStat = new CapturingLauncher();
            assertEquals(EtlLauncher.ErrorCode.OK, noStat.launch(new String[]{"execute-sql", "--no-stat", "--no-jmx",
                    "--url", "jdbc:h2:mem:noStat", file.toString()}));
            assertTrue(noStat.noJmx);
            assertTrue(noStat.out.toString().contains("Successfully executed SQL file"));
            assertFalse(noStat.out.toString().contains("JDBC counts"));

            Files.writeString(file, "INVALID SQL;");
            CapturingLauncher debug = new CapturingLauncher();
            assertEquals(EtlLauncher.ErrorCode.FAILED, debug.launch(new String[]{"execute-sql", "--debug", "--no-jmx",
                    "--url", "jdbc:h2:mem:debug", file.toString()}));
            assertTrue(debug.err.toString().contains("SQL execution failed"));
            assertTrue(debug.err.toString().contains("EtlExecutorException"));
            assertTrue(debug.err.toString().contains("\tat "));

            CapturingLauncher prefixDebug = new CapturingLauncher();
            assertEquals(EtlLauncher.ErrorCode.FAILED, prefixDebug.launch(new String[]{"-d", "execute-sql", "-nojmx",
                    "--url", "jdbc:h2:mem:prefixDebug", file.toString()}));
            assertTrue(prefixDebug.err.toString().contains("\tat "));
            CapturingLauncher suffixQuiet = new CapturingLauncher();
            Files.writeString(file, "CREATE TABLE deployment(payload VARCHAR);");
            assertEquals(EtlLauncher.ErrorCode.OK, suffixQuiet.launch(new String[]{"execute-sql", "-q", "-nojmx",
                    "--url", "jdbc:h2:mem:suffixQuiet", file.toString()}));
            assertEquals("", suffixQuiet.out.toString());
            assertEquals(EtlLauncher.ErrorCode.OK,
                    new CapturingLauncher().launch(new String[]{"execute-sql", "--version"}));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public void testEnvironmentSettingsInChildProcess() throws Exception {
        Path file = Files.createTempFile("scriptella-env-launcher", ".sql");
        try {
            Files.writeString(file, "CREATE TABLE deployment(payload VARCHAR NOT NULL CHECK(payload='bound-value')); INSERT INTO deployment VALUES ('${env.SCRIPTELLA_TEST_VALUE}');");
            assertEnvironmentLaunch(file, "jdbc:h2:mem:envLauncher", "credential-password-marker", 0);
            assertEnvironmentLaunch(file, "jdbc:h2:mem:emptyPassword", "", 0);
            assertEnvironmentLaunch(file, "jdbc:missing:credential-url-marker", "credential-password-marker", 1);
            assertEnvironmentLaunch(file, null, "credential-password-marker", 1);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void assertEnvironmentLaunch(Path file, String url, String password, int expectedExit) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), EtlLauncher.class.getName(),
                "execute-sql", "--debug", "--no-jmx", "--url", "${env.SCRIPTELLA_TEST_URL}",
                "--user", "${env.SCRIPTELLA_TEST_USER}", "--password", "${env.SCRIPTELLA_TEST_PASSWORD}",
                "--driver", "${env.SCRIPTELLA_TEST_DRIVER}", file.toString());
        if (url == null) {
            builder.environment().remove("SCRIPTELLA_TEST_URL");
        } else {
            builder.environment().put("SCRIPTELLA_TEST_URL", url);
        }
        builder.environment().put("SCRIPTELLA_TEST_USER", "credential-user-marker");
        builder.environment().put("SCRIPTELLA_TEST_PASSWORD", password);
        builder.environment().put("SCRIPTELLA_TEST_DRIVER", "org.h2.Driver");
        builder.environment().put("SCRIPTELLA_TEST_VALUE", "bound-value");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(output, expectedExit, process.waitFor());
        assertFalse(output, output.contains("credential-password-marker"));
        assertFalse(output, output.contains("credential-user-marker"));
        assertFalse(output, output.contains("credential-url-marker"));
        if (url == null) {
            assertTrue(output, output.contains("Missing environment variable: SCRIPTELLA_TEST_URL"));
        } else {
            assertFalse(output, output.contains(url));
        }
    }

    private static class CapturingLauncher extends EtlLauncher {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        boolean noJmx;

        protected PrintStream getOut() { return new PrintStream(out); }
        protected PrintStream getErr() { return new PrintStream(err); }

        @Override
        public void setNoJmx(boolean noJmx) {
            this.noJmx = noJmx;
            super.setNoJmx(noJmx);
        }
    }

    public void testMalformedDollarUrlsAreInvalidArguments() {
        for (String url : new String[]{"foo$bar", "foo${env.DB_URL}", "$", "$$url",
                "${}", "${env.DB_URL", "${1 +}"}) {
            CapturingLauncher launcher = new CapturingLauncher();
            assertEquals(url, EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                    launcher.launch(new String[]{"execute-sql", "--url", url, "missing.sql"}));
        }
    }

    public void testExecuteSqlAndArguments() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        EtlLauncher launcher = new EtlLauncher() {
            protected PrintStream getOut() { return new PrintStream(output); }
            protected PrintStream getErr() { return new PrintStream(output); }
        };
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"execute-sql", "--help"}));
        assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                launcher.launch(new String[]{"execute-sql", "--url"}));
        Path file = Files.createTempFile("scriptella-launcher", ".sql");
        String previousTable = System.getProperty("table");
        String previousValue = System.getProperty("value");
        try {
            System.setProperty("table", "deployment");
            System.setProperty("value", "hello");
            Files.writeString(file, "CREATE TABLE ${table}(payload VARCHAR); INSERT INTO ${table} VALUES (?value);");
            assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"execute-sql",
                    "--url", "jdbc:h2:mem:launcher", "--driver", "org.h2.Driver",
                    file.toString()}));
            assertTrue(output.toString().contains("2 statements, 1 rows updated"));
            Files.writeString(file, "INVALID SQL;");
            assertEquals(EtlLauncher.ErrorCode.FAILED, launcher.launch(new String[]{"execute-sql",
                    "--url", "jdbc:h2:mem:launcher", file.toString()}));
        } finally {
            if (previousTable == null) {
                System.clearProperty("table");
            } else {
                System.setProperty("table", previousTable);
            }
            if (previousValue == null) {
                System.clearProperty("value");
            } else {
                System.setProperty("value", previousValue);
            }
            Files.deleteIfExists(file);
        }
    }
}
