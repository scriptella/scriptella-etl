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

import scriptella.execution.ExecutionProgress;

import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** One CLI execution's optional timer. It has no references to driver resources. */
final class PeriodicProgressReporter implements AutoCloseable {
    private final ExecutionProgress progress;
    private final PrintStream errors;
    private final String label;
    private final LongSupplier clock;
    private final long started;
    private final ScheduledExecutorService scheduler;
    private boolean closed;

    PeriodicProgressReporter(long intervalMillis, ExecutionProgress progress, PrintStream errors, String label) {
        this(intervalMillis, progress, errors, label, System::nanoTime,
                Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "Scriptella CLI progress");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    // Clock and scheduler injection keeps tests independent of real 30-second waits.
    PeriodicProgressReporter(long intervalMillis, ExecutionProgress progress, PrintStream errors, String label,
                             LongSupplier clock, ScheduledExecutorService scheduler) {
        this.progress = progress;
        this.errors = errors;
        this.label = label;
        this.clock = clock;
        this.scheduler = scheduler;
        started = clock.getAsLong();
        scheduler.scheduleWithFixedDelay(this::report, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    static long parseInterval(String option) {
        if ("--progress".equals(option)) {
            return 30000;
        }
        String duration = option.substring("--progress=".length());
        try {
            if (!duration.matches("[0-9]+[sm]")) {
                throw new IllegalArgumentException();
            }
            long value = Long.parseLong(duration.substring(0, duration.length() - 1));
            long millis = Math.multiplyExact(value, duration.endsWith("m") ? 60000 : 1000);
            if (millis < 1000) {
                throw new IllegalArgumentException();
            }
            Math.multiplyExact(millis, 1000000); // Bound scheduler's nanosecond arithmetic, too.
            return millis;
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new IllegalArgumentException("Invalid progress interval: use --progress or "
                    + "--progress=<positive whole number>s|m (minimum 1s; duration must fit in nanoseconds)");
        }
    }

    synchronized void report() {
        if (!closed) {
            errors.println(format(clock.getAsLong() - started, progress.snapshot(), label));
        }
    }

    static String format(long elapsedNanos, ExecutionProgress.Snapshot snapshot, String label) {
        long seconds = Math.max(0, elapsedNanos) / 1000000000;
        String elapsed = String.format(Locale.ROOT, "[%02d:%02d] ", seconds / 60, seconds % 60);
        String metrics;
        if (snapshot.isAvailable()) {
            double rate = elapsedNanos > 0 ? snapshot.getStatements() * 1000000000.0 / elapsedNanos : 0;
            metrics = String.format(Locale.ROOT, "%,d statements | %,.0f/s", snapshot.getStatements(), rate);
            if (snapshot.isPartial()) {
                metrics += " (partial: some counters unavailable)";
            }
        } else {
            metrics = "statements unavailable | throughput unavailable";
        }
        return elapsed + metrics + (label == null ? "" : " | " + label.replace('\n', ' ').replace('\r', ' '));
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true; // Waits for an in-flight print; future callbacks become no-ops.
            scheduler.shutdownNow();
        }
        boolean interrupted = false;
        try {
            while (!scheduler.isTerminated()) {
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
