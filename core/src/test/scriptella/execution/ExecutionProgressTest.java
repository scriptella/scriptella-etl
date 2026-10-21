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
package scriptella.execution;

import junit.framework.TestCase;
import scriptella.spi.AbstractConnection.StatementCounter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionProgressTest extends TestCase {
    public void testAvailabilityAndLiveCounterVisibility() {
        ExecutionProgress progress = new ExecutionProgress();
        assertFalse(progress.snapshot().isAvailable());
        int lazy = progress.addCounter(null);
        StatementCounter first = new StatementCounter();
        progress.addCounter(first);
        first.statements = 10;
        assertEquals(10L, progress.snapshot().getStatements());
        assertTrue(progress.snapshot().isPartial());
        StatementCounter second = new StatementCounter();
        second.statements = 3;
        progress.setCounter(lazy, second);
        assertFalse(progress.snapshot().isPartial());
        assertEquals(13L, progress.snapshot().getStatements());
        first.statements++;
        assertEquals(14L, progress.snapshot().getStatements());
    }

    public void testConcurrentRegistrationAndSnapshots() throws Exception {
        ExecutionProgress progress = new ExecutionProgress();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            Future<?> reads = reader.submit(() -> {
                long previous = 0;
                for (int i = 0; i < 5000; i++) {
                    long current = progress.snapshot().getStatements();
                    assertTrue(current >= previous);
                    previous = current;
                }
            });
            for (int i = 0; i < 1000; i++) {
                int pending = progress.addCounter(null);
                StatementCounter counter = new StatementCounter();
                counter.statements = 1;
                progress.setCounter(pending, counter);
            }
            reads.get();
            assertEquals(1000L, progress.snapshot().getStatements());
            assertFalse(progress.snapshot().isPartial());
        } finally {
            reader.shutdownNow();
        }
    }

    public void testNestedBatchLazyAndNewTransactionCountsSurviveClose() throws Exception {
        String url = "jdbc:h2:mem:progress" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        try (Connection setup = DriverManager.getConnection(url); Statement sql = setup.createStatement()) {
            sql.execute("CREATE TABLE result(id INT PRIMARY KEY)");
            ExecutionProgress progress = new ExecutionProgress();
            EtlExecutor executor = executor("<connection id='source' driver='org.h2.Driver' url='" + url + "'/>"
                    + "<connection id='target' driver='org.h2.Driver' url='" + url + "' lazy-init='true'>"
                    + "statement.batchSize=3</connection>"
                    + "<query connection-id='source'>SELECT X AS ID FROM SYSTEM_RANGE(1,5)"
                    + "<script connection-id='target'>INSERT INTO result VALUES (?ID)</script></query>"
                    + "<script connection-id='target' new-tx='true'>INSERT INTO result VALUES (99)</script>");
            assertEquals(7, executor.execute(null, progress).getExecutedStatementsCount());
            assertEquals(7L, progress.snapshot().getStatements());
            assertTrue(progress.snapshot().isAvailable());
            assertFalse(progress.snapshot().isPartial());
            try (Connection fresh = DriverManager.getConnection(url);
                 Statement verify = fresh.createStatement();
                 ResultSet rows = verify.executeQuery("SELECT COUNT(*) FROM result")) {
                assertTrue(rows.next());
                assertEquals(6, rows.getInt(1));
            }
            sql.execute("DROP ALL OBJECTS");
        }
    }

    public void testFailureRetainsCountsButRollsBackRows() throws Exception {
        String url = "jdbc:h2:mem:progressFailure" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        try (Connection setup = DriverManager.getConnection(url); Statement sql = setup.createStatement()) {
            sql.execute("CREATE TABLE result(id INT)");
            ExecutionProgress progress = new ExecutionProgress();
            try {
                executor("<connection driver='org.h2.Driver' url='" + url + "'/>"
                        + "<script>INSERT INTO result VALUES (1); INVALID SQL;</script>").execute(null, progress);
                fail("Expected failure");
            } catch (EtlExecutorException expected) {
                assertEquals(1L, progress.snapshot().getStatements());
            }
            try (ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM result")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
            sql.execute("DROP ALL OBJECTS");
        }
    }

    private EtlExecutor executor(String body) throws Exception {
        Path file = Files.createTempFile("scriptella-progress", ".xml");
        try {
            Files.writeString(file, "<!DOCTYPE etl SYSTEM 'http://scriptella.org/dtd/etl.dtd'><etl>" + body + "</etl>");
            return EtlExecutor.newExecutor(file.toFile());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
