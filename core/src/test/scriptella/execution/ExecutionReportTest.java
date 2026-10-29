package scriptella.execution;

import junit.framework.TestCase;
import scriptella.jdbc.JdbcException;
import scriptella.configuration.Location;
import scriptella.spi.Connection;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.io.File;

public class ExecutionReportTest extends TestCase {
    public void testSafeFailureAndAtomicReplacement() throws Exception {
        ExecutionReport report = new ExecutionReport();
        report.complete(null, new IllegalArgumentException("password=secret SQL select private"));
        String json = report.toJson();
        assertTrue(json.contains("\"outcome\":\"failed\""));
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("select private"));
        assertValidJson(json);
        Path directory = Files.createTempDirectory("report-test");
        Path file = directory.resolve("report.json");
        try {
            Files.write(file, "old".getBytes(StandardCharsets.UTF_8));
            report.write(file);
            assertEquals(json, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    public void testCancellation() {
        ExecutionReport report = new ExecutionReport();
        report.complete(null, new EtlExecutorException(new InterruptedException()));
        assertEquals("cancelled", report.getOutcome());
    }

    public void testSuccess() {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        builder.etlComplete();
        assertTrue(builder.getStatistics().getTotalTime() >= 0);
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        assertEquals("success", report.getOutcome());
        assertTrue(report.getStarted().toString().endsWith("Z"));
        assertTrue(report.getFinished().toString().endsWith("Z"));
        assertTrue(report.getElapsedNanos() >= 0);
        assertTrue(report.toJson().contains("\"elements\":[]"));
    }

    public void testUnsupportedStatementCounterIsNull() {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        builder.elementStarted(new Location("/etl/script[1]"), new TestConnection(false));
        builder.elementExecuted();
        builder.etlComplete();
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        String json = report.toJson();
        assertTrue(json.contains("\"executedStatements\":null"));
        assertTrue(json.contains("\"statements\":null"));
    }

    public void testSupportedZeroStatementCounterRemainsZero() {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        builder.elementStarted(new Location("/etl/script[1]"), new TestConnection(true));
        builder.elementExecuted();
        builder.etlComplete();
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        String json = report.toJson();
        assertTrue(json.contains("\"executedStatements\":0"));
        assertTrue(json.contains("\"statements\":0"));
    }

    public void testLegacyProviderNonzeroCounterRemainsAvailable() {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        TestConnection legacy = new TestConnection(false);
        builder.elementStarted(new Location("/etl/script[1]"), legacy);
        legacy.statements = 3;
        builder.elementExecuted();
        builder.etlComplete();
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        String json = report.toJson();
        assertTrue(json.contains("\"executedStatements\":3"));
        assertTrue(json.contains("\"statements\":3"));
    }

    public void testMixedStatementCounterAvailabilityIsUnknown() {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        Location location = new Location("/etl/script[1]");
        TestConnection supported = new TestConnection(true);
        builder.elementStarted(location, supported);
        supported.statements = 1;
        builder.elementExecuted();
        builder.elementStarted(location, new TestConnection(false));
        builder.elementExecuted();
        builder.etlComplete();
        assertEquals(1, builder.getStatistics().getExecutedStatementsCount());
        assertEquals(1L, builder.getStatistics().getElements().iterator().next().getStatementsCount());
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        String json = report.toJson();
        assertTrue(json.contains("\"executedStatements\":null"));
        assertTrue(json.contains("\"statements\":null"));
    }

    public void testSqlFailureIncludesCodesWithoutSensitiveText() throws Exception {
        SQLException sql = new SQLException("private SQL and bound secret", "23505", 23505);
        EtlExecutorException failure = new EtlExecutorException(
                new JdbcException("unsafe statement", sql, "insert into private_table", null));
        ExecutionReport report = new ExecutionReport();
        report.complete(null, failure);
        String json = report.toJson();
        assertTrue(json.contains("\"sqlState\":\"23505\""));
        assertTrue(json.contains("\"vendorCode\":23505"));
        assertFalse(json.contains("private SQL"));
        assertFalse(json.contains("private_table"));
        assertFalse(json.contains("bound secret"));
        assertEquals("execution", report.getError().getCategory());
        assertEquals("java.sql.SQLException", report.getError().getExceptionType());
        assertValidJson(json);
    }

    public void testExceptionTypeUsesUnderlyingCause() {
        EtlExecutorException failure = new EtlExecutorException(new IllegalStateException("wrapper secret",
                new java.io.FileNotFoundException("/private/input secret")));
        ExecutionReport report = new ExecutionReport();
        report.complete(null, failure);
        assertEquals("java.io.FileNotFoundException", report.getError().getExceptionType());
        String json = report.toJson();
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("/private/input"));
    }

    public void testDocumentedOutcomeExamplesAreValidJson() throws Exception {
        File basedir = new File(System.getProperty("basedir", "core"));
        File examples = new File(basedir, "../docs/reports/examples");
        assertExample(examples.toPath().resolve("success.json"), "success", true);
        assertExample(examples.toPath().resolve("partial-failure.json"), "failed", false);
        assertExample(examples.toPath().resolve("cancelled.json"), "cancelled", false);
    }

    public void testLargeRowCounterRetainsOneAggregate() throws Exception {
        ExecutionStatisticsBuilder builder = new ExecutionStatisticsBuilder();
        builder.etlStarted();
        ExecutionStatistics.ElementInfo query = builder.getQueryRowStatistics(new Location("/etl/query[1]"));
        for (int i = 0; i < 10000; i++) {
            builder.queryRowEmitted(query);
        }
        builder.etlComplete();
        ExecutionReport report = new ExecutionReport();
        report.complete(builder.getStatistics(), null);
        assertEquals(1, builder.getStatistics().getElements().size());
        assertEquals(10000L, builder.getStatistics().getElements().iterator().next().getQueryRows());
        assertFalse(report.toJson().contains("\"rows\":"));
    }

    public void testSuppressedStatisticsSkipOptionalCounters() {
        SilentExecutionStatisticsBuilder builder = new SilentExecutionStatisticsBuilder();
        builder.etlStarted();
        assertNull(builder.getConnectionInfo("db"));
        assertNull(builder.getQueryRowStatistics(new Location("/etl/query[1]")));
        builder.queryRowEmitted(null);
        builder.etlComplete();
        assertTrue(builder.getStatistics().getElements().isEmpty());
        assertTrue(builder.getStatistics().getConnections().isEmpty());
    }

    private static final class TestConnection implements Connection {
        private final boolean supported;
        private long statements;

        private TestConnection(boolean supported) { this.supported = supported; }
        public DialectIdentifier getDialectIdentifier() { return null; }
        public void executeScript(Resource content, ParametersCallback parameters) { }
        public void executeQuery(Resource content, ParametersCallback parameters, QueryCallback callback) { }
        public long getExecutedStatementsCount() { return statements; }
        public boolean isExecutedStatementsCountSupported() { return supported; }
        public void commit() { }
        public void rollback() { throw new UnsupportedOperationException(); }
        public void close() { }
    }

    private static void assertValidJson(String json) throws Exception {
        new JsonValidator(json).parse();
    }

    private static void assertExample(Path path, String expectedOutcome, boolean expectedComplete) throws Exception {
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertValidJson(json);
        for (String field : new String[]{"schemaVersion", "scriptellaVersion", "executionId", "outcome",
                "started", "finished", "elapsedNanos", "complete", "error", "metrics", "elements"}) {
            assertTrue("Missing root field " + field + " in " + path, json.contains("\"" + field + "\":"));
        }
        assertTrue(json.contains("\"schemaVersion\": " + ExecutionReport.SCHEMA_VERSION) ||
                json.contains("\"schemaVersion\":" + ExecutionReport.SCHEMA_VERSION));
        assertTrue(json.contains("\"outcome\": \"" + expectedOutcome + "\"") ||
                json.contains("\"outcome\":\"" + expectedOutcome + "\""));
        assertTrue(json.contains("\"complete\": " + expectedComplete) ||
                json.contains("\"complete\":" + expectedComplete));
    }

    /** Minimal strict JSON grammar check kept dependency-free for the Ant build. */
    private static final class JsonValidator {
        private final String text;
        private int index;

        JsonValidator(String text) { this.text = text; }

        void parse() {
            value();
            whitespace();
            if (index != text.length()) { fail(); }
        }

        private void value() {
            whitespace();
            if (index >= text.length()) { fail(); }
            char c = text.charAt(index);
            if (c == '{') { object(); }
            else if (c == '[') { array(); }
            else if (c == '"') { string(); }
            else if (c == '-' || Character.isDigit(c)) { number(); }
            else if (text.startsWith("true", index)) { index += 4; }
            else if (text.startsWith("false", index)) { index += 5; }
            else if (text.startsWith("null", index)) { index += 4; }
            else { fail(); }
        }

        private void object() {
            index++;
            whitespace();
            if (take('}')) { return; }
            do {
                whitespace();
                string();
                whitespace();
                require(':');
                value();
                whitespace();
            } while (take(','));
            require('}');
        }

        private void array() {
            index++;
            whitespace();
            if (take(']')) { return; }
            do {
                value();
                whitespace();
            } while (take(','));
            require(']');
        }

        private void string() {
            require('"');
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') { return; }
                if (c < 32) { fail(); }
                if (c == '\\') {
                    if (index >= text.length()) { fail(); }
                    char escaped = text.charAt(index++);
                    if (escaped == 'u') {
                        for (int i = 0; i < 4; i++) {
                            if (index >= text.length() || Character.digit(text.charAt(index++), 16) < 0) { fail(); }
                        }
                    } else if ("\"\\/bfnrt".indexOf(escaped) < 0) {
                        fail();
                    }
                }
            }
            fail();
        }

        private void number() {
            take('-');
            digits();
            if (take('.')) { digits(); }
            if (take('e') || take('E')) {
                if (!take('+')) { take('-'); }
                digits();
            }
        }

        private void digits() {
            int start = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) { index++; }
            if (start == index) { fail(); }
        }

        private void whitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) { index++; }
        }

        private boolean take(char c) {
            if (index < text.length() && text.charAt(index) == c) { index++; return true; }
            return false;
        }

        private void require(char c) {
            if (!take(c)) { fail(); }
        }

        private void fail() {
            throw new AssertionError("Invalid JSON at offset " + index);
        }
    }
}
