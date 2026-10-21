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
package scriptella.tools.launcher;

import junit.framework.TestCase;
import scriptella.core.EtlCancelledException;
import scriptella.execution.ExecutionProgress;
import scriptella.spi.AbstractConnection;
import scriptella.spi.AbstractScriptellaDriver;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CliProgressTest extends TestCase {
    private static final ConcurrentLinkedQueue<Gate> GATES = new ConcurrentLinkedQueue<Gate>();
    private final List<Path> files = new ArrayList<Path>();
    private final List<String> databases = new ArrayList<String>();

    public static int pause() {
        Gate gate = GATES.remove();
        gate.entered.countDown();
        try {
            if (!gate.release.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Test gate timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        EtlCancelledException.checkEtlCancelled();
        return 1;
    }

    @Override
    protected void tearDown() throws Exception {
        GATES.clear();
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
        for (String url : databases) {
            try (Connection c = DriverManager.getConnection(url); Statement sql = c.createStatement()) {
                sql.execute("SHUTDOWN");
            }
        }
    }

    public void testNestedBatchMultipleFilesAndExplicitOptions() throws Exception {
        Capture launcher = new Capture();
        Path first = job(false, false);
        Path second = job(false, true);
        Gate a = new Gate();
        Gate b = new Gate();
        GATES.add(a);
        GATES.add(b);
        FutureTask<EtlLauncher.ErrorCode> run = new FutureTask<>(() -> launcher.launch(new String[]{
                "--quiet", "--no-stat", "--no-jmx", "--progress=10s", first.toString(), second.toString()}));
        Thread execution = new Thread(run, "ETL test execution");
        execution.start();
        try {
            assertTrue(a.entered.await(10, TimeUnit.SECONDS));
            assertEquals(10000L, launcher.intervals.get(0).longValue());
            launcher.clock.set(TimeUnit.SECONDS.toNanos(30));
            launcher.reporters.get(0).report();
            assertTrue(launcher.errors.toString(), launcher.errors.toString().contains("[00:30] 8 statements | 0/s"));
            assertTrue(launcher.errors.toString().contains(first.toString()));
            a.release.countDown();
            assertTrue(b.entered.await(10, TimeUnit.SECONDS));
            assertTrue(launcher.schedulers.get(0).isTerminated());
            launcher.clock.addAndGet(TimeUnit.SECONDS.toNanos(30));
            launcher.reporters.get(1).report();
            assertTrue(launcher.errors.toString().contains("partial: some counters unavailable"));
            assertTrue(launcher.errors.toString().contains(second.toString()));
            assertEquals(8L, launcher.progresses.get(1).snapshot().getStatements()); // Per-file reset.
            b.release.countDown();
            assertEquals(EtlLauncher.ErrorCode.OK, run.get(10, TimeUnit.SECONDS));
            assertStopped(launcher);
            assertEquals("", launcher.output.toString());
            for (String url : databases) {
                try (Connection c = DriverManager.getConnection(url); Statement sql = c.createStatement();
                     ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM result")) {
                    assertTrue(rows.next());
                    assertEquals(5, rows.getInt(1)); // Includes final partial batch.
                }
            }
        } finally {
            a.release.countDown();
            b.release.countDown();
            execution.interrupt();
            execution.join(10000);
        }
    }

    public void testReporterStopsOnFailure() throws Exception {
        runStoppedCase(true, false);
    }

    public void testReporterStopsOnCooperativeCancellation() throws Exception {
        runStoppedCase(false, true);
    }

    private void runStoppedCase(boolean fail, boolean cancel) throws Exception {
        Capture launcher = new Capture();
        Path file = job(fail, false);
        Gate gate = new Gate();
        GATES.add(gate);
        FutureTask<EtlLauncher.ErrorCode> run = new FutureTask<>(() -> launcher.launch(new String[]{
                "--quiet", "--no-jmx", "--progress", file.toString()}));
        Thread execution = new Thread(run, "ETL lifecycle test");
        execution.start();
        try {
            assertTrue(gate.entered.await(10, TimeUnit.SECONDS));
            assertEquals(30000L, launcher.intervals.get(0).longValue());
            launcher.reporters.get(0).report();
            if (cancel) {
                execution.interrupt();
            } else {
                gate.release.countDown();
            }
            assertEquals(EtlLauncher.ErrorCode.FAILED, run.get(10, TimeUnit.SECONDS));
            assertStopped(launcher);
            assertTrue(launcher.progresses.get(0).snapshot().getStatements() >= 8);
        } finally {
            gate.release.countDown();
            execution.interrupt();
            execution.join(10000);
        }
    }

    public void testUnavailableCounterNeverCallsDriverFromReporter() throws Exception {
        Capture launcher = new Capture();
        Path file = xml("<connection driver='" + UnavailableDriver.class.getName() + "'/><script>pause</script>");
        Gate gate = new Gate();
        GATES.add(gate);
        FutureTask<EtlLauncher.ErrorCode> run = new FutureTask<>(() -> launcher.launch(new String[]{
                "--quiet", "--no-jmx", "--progress=2m", file.toString()}));
        Thread execution = new Thread(run, "unavailable driver execution");
        execution.start();
        try {
            assertTrue(gate.entered.await(10, TimeUnit.SECONDS));
            assertEquals(120000L, launcher.intervals.get(0).longValue());
            launcher.reporters.get(0).report(); // Test thread must not call the driver.
            assertTrue(launcher.errors.toString().contains("statements unavailable | throughput unavailable"));
            gate.release.countDown();
            assertEquals(EtlLauncher.ErrorCode.OK, run.get(10, TimeUnit.SECONDS));
            assertStopped(launcher);
        } finally {
            gate.release.countDown();
            execution.interrupt();
            execution.join(10000);
        }
    }

    public void testDisabledInvalidAndCheckOptions() throws Exception {
        Path file = xml("<connection driver='org.h2.Driver' url='jdbc:h2:mem:disabledProgress'/>"
                + "<script>CREATE TABLE t(id INT);</script>");
        Capture launcher = new Capture();
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"--quiet", "--no-jmx", file.toString()}));
        assertEquals(0, launcher.reporters.size());
        assertEquals("", launcher.errors.toString());
        for (String option : new String[]{"--progress=", "--progress=0s", "--progress=0.1s", "--progress=-1m"}) {
            assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                    launcher.launch(new String[]{option, file.toString()}));
        }
        assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION,
                launcher.launch(new String[]{"--progress", "--check", file.toString()}));
        assertEquals(0, launcher.reporters.size());
        assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(new String[]{"--help"}));
        assertTrue(launcher.output.toString().contains("--progress[=Ns|Nm]"));
    }

    public void testProgressAndTemplateRejectedInEitherOrder() {
        for (String templateOption : new String[]{"--template", "-t", "-template"}) {
            for (String progressOption : new String[]{"--progress", "--progress=10s", "--progress=2m"}) {
                for (String[] args : new String[][]{
                        {progressOption, templateOption},
                        {templateOption, progressOption},
                        {templateOption, "DataMigrator", progressOption}}) {
                    final boolean[] delegated = {false};
                    Capture launcher = new Capture() {
                        @Override
                        protected ErrorCode template(List<String> arguments) {
                            delegated[0] = true;
                            return ErrorCode.OK;
                        }
                    };
                    assertEquals(EtlLauncher.ErrorCode.UNRECOGNIZED_OPTION, launcher.launch(args));
                    assertTrue(launcher.errors.toString().contains(
                            "--progress requires execution, not template generation"));
                    assertFalse("Must reject before template generation", delegated[0]);
                    assertTrue(launcher.reporters.isEmpty());
                }
            }
        }
    }

    public void testDirectSqlOptionBeforeAndAfterCommand() throws Exception {
        Path file = Files.createTempFile("scriptella-progress", ".sql");
        files.add(file);
        Files.writeString(file, "CREATE TABLE t(id INT);");
        for (String[] args : new String[][]{
                {"--progress=10s", "--quiet", "--no-jmx", "execute-sql", "--url", "jdbc:h2:mem:sqlProgress1", file.toString()},
                {"execute-sql", "--progress=2m", "--quiet", "--no-stat", "--no-jmx", "--url", "jdbc:h2:mem:sqlProgress2", file.toString()}}) {
            Capture launcher = new Capture();
            assertEquals(EtlLauncher.ErrorCode.OK, launcher.launch(args));
            assertEquals(1, launcher.reporters.size());
            assertStopped(launcher);
            assertEquals("", launcher.errors.toString()); // No periodic line before interval.
        }
    }

    private Path job(boolean fail, boolean unsupported) throws Exception {
        String url = "jdbc:h2:mem:cliProgress" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        databases.add(url);
        return xml("<connection id='source' driver='org.h2.Driver' url='" + url + "'/>"
                + "<connection id='target' driver='org.h2.Driver' url='" + url + "' lazy-init='true'>"
                + "statement.batchSize=3</connection>"
                + (unsupported ? "<connection id='unsupported' driver='" + UnavailableDriver.class.getName() + "'/>" : "")
                + "<script connection-id='source'>CREATE ALIAS PAUSE FOR &quot;"
                + CliProgressTest.class.getName() + ".pause&quot;; CREATE TABLE result(id INT);</script>"
                + "<query connection-id='source'>SELECT X AS ID FROM SYSTEM_RANGE(1,5)"
                + "<script connection-id='target'>INSERT INTO result VALUES (?ID)</script></query>"
                + "<query connection-id='source'>CALL PAUSE()</query>"
                + (fail ? "<script connection-id='source'>INVALID SQL;</script>" : ""));
    }

    private Path xml(String body) throws Exception {
        Path file = Files.createTempFile("scriptella-cli-progress", ".xml");
        files.add(file);
        Files.writeString(file, "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'><etl>" + body + "</etl>");
        return file;
    }

    private void assertStopped(Capture launcher) {
        String before = launcher.errors.toString();
        for (PeriodicProgressReporter reporter : launcher.reporters) {
            reporter.report();
        }
        assertEquals(before, launcher.errors.toString());
        for (ScheduledThreadPoolExecutor scheduler : launcher.schedulers) {
            assertTrue(scheduler.isTerminated());
        }
    }

    private static final class Gate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
    }

    private static class Capture extends EtlLauncher {
        final ByteArrayOutputStream errors = new ByteArrayOutputStream();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final AtomicLong clock = new AtomicLong();
        final List<PeriodicProgressReporter> reporters = new CopyOnWriteArrayList<>();
        final List<ExecutionProgress> progresses = new CopyOnWriteArrayList<>();
        final List<ScheduledThreadPoolExecutor> schedulers = new CopyOnWriteArrayList<>();
        final List<Long> intervals = new CopyOnWriteArrayList<>();

        @Override
        protected PrintStream getErr() {
            return new PrintStream(errors);
        }

        @Override
        protected PrintStream getOut() {
            return new PrintStream(output);
        }

        @Override
        PeriodicProgressReporter createProgressReporter(long interval, ExecutionProgress progress, String label) {
            ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
            // Manual ticks while ETL is blocked test live output without 30-second waits.
            PeriodicProgressReporter reporter = new PeriodicProgressReporter(30000, progress, getErr(), label,
                    clock::get, scheduler);
            reporters.add(reporter);
            progresses.add(progress);
            schedulers.add(scheduler);
            intervals.add(interval);
            return reporter;
        }
    }

    public static class UnavailableDriver extends AbstractScriptellaDriver {
        public scriptella.spi.Connection connect(ConnectionParameters parameters) {
            return new AbstractConnection(parameters) {
                final Thread owner = Thread.currentThread();

                public void executeScript(Resource resource, ParametersCallback parameters) {
                    pause();
                }

                public void executeQuery(Resource resource, ParametersCallback parameters, QueryCallback callback) {
                    throw new UnsupportedOperationException();
                }

                public long getExecutedStatementsCount() {
                    assertSame("Driver callback on reporting thread", owner, Thread.currentThread());
                    return 0;
                }

                public void close() {
                    assertSame(owner, Thread.currentThread());
                }
            };
        }
    }
}
