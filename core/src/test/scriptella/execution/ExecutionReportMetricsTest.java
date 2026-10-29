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

import scriptella.DBTestCase;
import scriptella.interactive.ProgressIndicator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class ExecutionReportMetricsTest extends DBTestCase {
    public void testFailureRollsBackAndRetainsFinalizedPartialMetrics() throws Exception {
        Connection observer = getConnection("reportRollback");
        Path etl = writeEtl("reportRollback", "<script connection-id='db'>"
                + "CREATE TABLE t(id INT PRIMARY KEY); INSERT INTO t VALUES (1); INSERT INTO t VALUES (1);"
                + "</script>");
        try {
            ExecutionReport report = EtlExecutor.newExecutor(etl.toFile()).executeReport();
            assertEquals("failed", report.getOutcome());
            assertFalse(report.isComplete());
            assertTrue(report.getStatistics().getTotalTime() >= 0);
            assertTrue(report.toJson().contains("\"affectedRowsReported\":1"));
            java.sql.ResultSet result = observer.createStatement().executeQuery("SELECT COUNT(*) FROM t");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
            assertFalse(report.toJson().contains("INSERT INTO t"));
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    public void testQueryEmissionIsRetainedWhenNestedElementFails() throws Exception {
        Path etl = writeEtl("reportPartialRows", "<script connection-id='db'>"
                + "CREATE TABLE t(id INT); INSERT INTO t VALUES (1);"
                + "</script><query connection-id='db'>SELECT id FROM t"
                + "<script>THIS IS INVALID SQL;</script></query>");
        try {
            ExecutionReport report = EtlExecutor.newExecutor(etl.toFile()).executeReport();
            assertEquals("failed", report.getOutcome());
            ExecutionStatistics.ElementInfo query = findElement(report.getStatistics(), "/etl/query[1]");
            assertEquals(1L, query.getQueryRows());
            assertEquals(1, query.getFailedExecutionCount());
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    public void testCleanupFailureDoesNotReplacePrimaryFailureOrSkipFinalization() throws Exception {
        Path etl = writeEtl("reportCleanupFailure",
                "<script connection-id='db'>CREATE TABLE t(id INT); INVALID SQL;</script>");
        try {
            EtlExecutor base = EtlExecutor.newExecutor(etl.toFile());
            EtlExecutor executor = new EtlExecutor(base.getConfiguration()) {
                void closeAll(EtlContext ctx) {
                    throw new IllegalStateException("cleanup failure");
                }
            };
            try {
                executor.execute();
                fail("Execution failure expected");
            } catch (EtlExecutorException expected) {
                assertNotNull(expected.getStatistics());
                assertTrue(expected.getStatistics().getTotalTime() >= 0);
                assertNotNull(expected.getCause());
                assertTrue(expected.getCause().getSuppressed().length > 0);
                assertFalse(expected.getCause().getMessage().contains("cleanup failure"));
            }
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    public void testDriverSetupFailureFinalizesAndClosesEarlierConnections() throws Exception {
        Path etl = Files.createTempFile("scriptella-report-driver-setup", ".etl.xml");
        String xml = "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'>"
                + "<etl><connection id='first' driver='org.h2.Driver' url='jdbc:h2:mem:setupFirst'/>"
                + "<connection id='missing' driver='scriptella.no.SuchDriver' url='x'/></etl>";
        Files.write(etl, xml.getBytes(StandardCharsets.UTF_8));
        try {
            ExecutionReport report = EtlExecutor.newExecutor(etl.toFile()).executeReport();
            assertEquals("failed", report.getOutcome());
            assertEquals("setup", report.getError().getCategory());
            assertTrue(report.getStatistics().getTotalTime() >= 0);
            assertTrue(report.toJson().contains("\"id\":\"first\",\"openAttempts\":1,\"opened\":1"));
            assertTrue(report.toJson().contains("\"closeAttempts\":1,\"closed\":1,\"closeFailures\":0"));
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    public void testConnectionOpenFailureIsCountedDuringSetup() throws Exception {
        Path etl = Files.createTempFile("scriptella-report-open-failure", ".etl.xml");
        String xml = "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'>"
                + "<etl><connection id='first' driver='org.h2.Driver' url='jdbc:h2:mem:openFailureFirst'/>"
                + "<connection id='failed' driver='org.h2.Driver' url='not-a-jdbc-url'/></etl>";
        Files.write(etl, xml.getBytes(StandardCharsets.UTF_8));
        try {
            ExecutionReport report = EtlExecutor.newExecutor(etl.toFile()).executeReport();
            assertEquals("failed", report.getOutcome());
            assertEquals("setup", report.getError().getCategory());
            assertTrue(report.getStatistics().getTotalTime() >= 0);
            assertTrue(report.toJson().contains("\"id\":\"failed\",\"openAttempts\":1,\"opened\":0,\"openFailures\":1"));
            assertTrue(report.toJson().contains("\"id\":\"first\",\"openAttempts\":1,\"opened\":1,\"openFailures\":0"));
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    public void testCooperativeCancellationReturnsFinalizedReport() {
        EtlExecutor executor = newEtlExecutor("ExecutionStatisticsTest.xml");
        try {
            ExecutionReport report = executor.executeReport(new ProgressIndicator() {
                public void showProgress(double progress, String message) {
                    Thread.currentThread().interrupt();
                }
            });
            assertEquals("cancelled", report.getOutcome());
            assertFalse(report.isComplete());
            assertTrue(report.isFinalized());
            assertTrue(report.getStatistics().getTotalTime() >= 0);
            assertTrue(report.toJson().contains("\"category\":\"cancellation\""));
        } finally {
            Thread.interrupted();
        }
    }

    public void testSuppressStatisticsMakesReportMetricsUnavailable() throws Exception {
        Path etl = writeEtl("reportSuppressed", "<script connection-id='db'>CREATE TABLE t(id INT);</script>");
        try {
            EtlExecutor executor = EtlExecutor.newExecutor(etl.toFile());
            executor.setSuppressStatistics(true);
            ExecutionReport report = executor.executeReport();
            assertEquals("success", report.getOutcome());
            assertNull(report.getStatistics());
            assertTrue(report.toJson().contains("\"metrics\":null"));
        } finally {
            Files.deleteIfExists(etl);
        }
    }

    private Path writeEtl(String database, String body) throws Exception {
        Path file = Files.createTempFile("scriptella-report-", ".etl.xml");
        String url = "jdbc:h2:mem:" + database + ";MODE=LEGACY;NON_KEYWORDS=VALUE";
        String xml = "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'>"
                + "<etl><connection id='db' driver='org.h2.Driver' url='" + url + "' user='sa' password=''/>"
                + body + "</etl>";
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static ExecutionStatistics.ElementInfo findElement(ExecutionStatistics statistics, String id) {
        for (ExecutionStatistics.ElementInfo element : statistics.getElements()) {
            if (id.equals(element.getId())) {
                return element;
            }
        }
        throw new AssertionError("Missing element " + id);
    }

}
