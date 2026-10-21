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
import scriptella.execution.ExecutionProgress;
import scriptella.spi.AbstractConnection.StatementCounter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PeriodicProgressReporterTest extends TestCase {
    public void testIntervalsAndBounds() {
        assertEquals(30000L, PeriodicProgressReporter.parseInterval("--progress"));
        assertEquals(10000L, PeriodicProgressReporter.parseInterval("--progress=10s"));
        assertEquals(120000L, PeriodicProgressReporter.parseInterval("--progress=2m"));
        assertEquals(1000L, PeriodicProgressReporter.parseInterval("--progress=1s"));
        for (String value : new String[]{"", "10", "s", "0s", "0m", "-1s", "0.5s", "1h", "1S",
                "1 s", "+1s", "999999999999999999999s", "9223372036854775807m", "9223372036855s"}) {
            try {
                PeriodicProgressReporter.parseInterval("--progress=" + value);
                fail("Accepted " + value);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("minimum 1s"));
            }
        }
    }

    public void testFormattingAndAvailability() {
        ExecutionProgress progress = new ExecutionProgress();
        assertEquals("[00:30] statements unavailable | throughput unavailable",
                PeriodicProgressReporter.format(TimeUnit.SECONDS.toNanos(30), progress.snapshot(), null));
        StatementCounter counter = new StatementCounter();
        progress.addCounter(counter);
        counter.statements = 185000;
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            assertEquals("[00:30] 185,000 statements | 6,167/s",
                    PeriodicProgressReporter.format(TimeUnit.SECONDS.toNanos(30), progress.snapshot(), null));
        } finally {
            Locale.setDefault(previous);
        }
        progress.addCounter(null);
        assertEquals("[61:01] 185,000 statements | 51/s (partial: some counters unavailable) | job.xml",
                PeriodicProgressReporter.format(TimeUnit.SECONDS.toNanos(3661), progress.snapshot(), "job.xml"));
        assertTrue(PeriodicProgressReporter.format(0, progress.snapshot(), null).contains("0/s"));
    }

    public void testClockSchedulingAndIdempotentClose() {
        ExecutionProgress progress = new ExecutionProgress();
        AtomicLong clock = new AtomicLong(100);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        PeriodicProgressReporter reporter = new PeriodicProgressReporter(30000, progress,
                new PrintStream(errors), null, clock::get, scheduler);
        assertEquals(1, scheduler.getQueue().size());
        clock.addAndGet(TimeUnit.SECONDS.toNanos(30));
        reporter.report();
        assertTrue(errors.toString().startsWith("[00:30] statements unavailable"));
        reporter.close();
        assertTrue(scheduler.isTerminated());
        String before = errors.toString();
        reporter.report();
        reporter.close();
        assertEquals(before, errors.toString());
    }

    public void testClosePreservesInterrupt() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        PeriodicProgressReporter reporter = new PeriodicProgressReporter(30000, new ExecutionProgress(),
                System.err, null, System::nanoTime, scheduler);
        Thread.currentThread().interrupt();
        try {
            reporter.close();
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(scheduler.isShutdown());
        } finally {
            Thread.interrupted();
        }
    }
}
