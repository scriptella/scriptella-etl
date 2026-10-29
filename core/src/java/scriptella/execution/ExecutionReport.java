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

import scriptella.configuration.ConfigurationException;
import scriptella.configuration.Location;
import scriptella.core.ExceptionInterceptor;
import scriptella.spi.AbstractScriptellaDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/** One bounded machine-readable report for one Scriptella execution. */
public final class ExecutionReport {
    public static final int SCHEMA_VERSION = 1;

    private final String executionId = UUID.randomUUID().toString();
    private final Instant started = Instant.now();
    private final long startedNanos = System.nanoTime();
    private Instant finished;
    private long elapsedNanos = -1;
    private ExecutionStatistics statistics;
    private String outcome;
    private ErrorInfo error;

    public synchronized void complete(ExecutionStatistics statistics, Throwable failure) {
        if (finished != null) {
            throw new IllegalStateException("Report is already complete");
        }
        this.statistics = statistics;
        this.outcome = determineOutcome(failure);
        this.error = failure == null ? null : new ErrorInfo(failure);
        finished = Instant.now();
        elapsedNanos = Math.max(0, System.nanoTime() - startedNanos);
    }

    public String getExecutionId() { return executionId; }
    public Instant getStarted() { return started; }
    public Instant getFinished() { return finished; }
    public long getElapsedNanos() { return elapsedNanos; }
    public ExecutionStatistics getStatistics() { return statistics; }
    public ErrorInfo getError() { return error; }
    public boolean isComplete() { return finished != null && "success".equals(outcome); }
    public boolean isFinalized() { return finished != null; }

    public String getOutcome() {
        if (outcome == null) {
            throw new IllegalStateException("Report is not complete");
        }
        return outcome;
    }

    private static String determineOutcome(Throwable failure) {
        if (failure == null) { return "success"; }
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < 32; t = t.getCause()) {
            if (t instanceof EtlExecutorException && ((EtlExecutorException) t).isCancelled()) {
                return "cancelled";
            }
            if (t instanceof scriptella.core.EtlCancelledException || t instanceof InterruptedException) {
                return "cancelled";
            }
        }
        return "failed";
    }

    /** Serializes only bounded aggregates and deliberately omits driver messages and stack traces. */
    public synchronized String toJson() {
        if (finished == null) {
            throw new IllegalStateException("Report is not complete");
        }
        StringBuilder b = new StringBuilder(2048);
        b.append("{\"schemaVersion\":").append(SCHEMA_VERSION)
                .append(",\"scriptellaVersion\":").append(quote(AbstractScriptellaDriver.getScriptellaVersion()))
                .append(",\"executionId\":").append(quote(executionId))
                .append(",\"outcome\":").append(quote(getOutcome()))
                .append(",\"started\":").append(quote(started.toString()))
                .append(",\"finished\":").append(quote(finished.toString()))
                .append(",\"elapsedNanos\":").append(elapsedNanos)
                .append(",\"complete\":").append(isComplete())
                .append(",\"error\":");
        appendError(b);
        b.append(",\"metrics\":");
        appendMetrics(b);
        b.append(",\"elements\":[");
        if (statistics != null) {
            boolean first = true;
            for (ExecutionStatistics.ElementInfo e : statistics.getElements()) {
                if (!first) { b.append(','); }
                first = false;
                b.append("{\"location\":").append(quote(e.getId()))
                        .append(",\"successfulExecutions\":").append(e.getSuccessfulExecutionCount())
                        .append(",\"failedExecutions\":").append(e.getFailedExecutionCount())
                        .append(",\"statements\":").append(number(e.getStatementsCountOrNull()))
                        .append(",\"queryRowsEmitted\":").append(e.getQueryRows())
                        .append(",\"workingNanos\":").append(e.getWorkingTime()).append('}');
            }
        }
        return b.append("]}\n").toString();
    }

    private void appendError(StringBuilder b) {
        if (error == null) {
            b.append("null");
            return;
        }
        b.append("{\"category\":").append(quote(error.category))
                .append(",\"terminal\":true")
                .append(",\"location\":").append(quote(error.location))
                .append(",\"exceptionType\":").append(quote(error.exceptionType))
                .append(",\"message\":").append(quote(error.message))
                .append(",\"sqlState\":").append(quote(error.sqlState))
                .append(",\"vendorCode\":").append(error.vendorCode == null ? "null" : error.vendorCode.toString())
                .append('}');
    }

    public static final class ErrorInfo {
        private final String category;
        private final String location;
        private final String exceptionType;
        private final String message;
        private final String sqlState;
        private final Integer vendorCode;

        private ErrorInfo(Throwable failure) {
            category = errorCategory(failure);
            Location errorLocation = failure instanceof EtlExecutorException ?
                    ((EtlExecutorException) failure).getLastElementLocation() : null;
            String locationValue = errorLocation == null ? null : errorLocation.toString();
            location = locationValue != null && locationValue.length() > 1024 ?
                    locationValue.substring(0, 1024) : locationValue;
            SQLException sqlException = findSqlException(failure);
            // Only the class name is used; messages of the underlying exception are not serialized.
            String type = (sqlException != null ? sqlException : rootCause(failure)).getClass().getName();
            exceptionType = type.length() > 256 ? type.substring(0, 256) : type;
            message = "Execution failed; sensitive details omitted.";
            sqlState = sqlException == null ? null : safeSqlState(sqlException.getSQLState());
            vendorCode = sqlException == null ? null : sqlException.getErrorCode();
        }

        public String getCategory() { return category; }
        public String getLocation() { return location; }
        public String getExceptionType() { return exceptionType; }
        public String getMessage() { return message; }
        public String getSqlState() { return sqlState; }
        public Integer getVendorCode() { return vendorCode; }
    }

    private static String errorCategory(Throwable failure) {
        if ("cancelled".equals(determineOutcome(failure))) { return "cancellation"; }
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < 32; t = t.getCause()) {
            if (t instanceof EtlExecutorException && ((EtlExecutorException) t).isSetupFailure()) {
                return "setup";
            }
            if (t instanceof ConfigurationException) { return "setup"; }
        }
        return "execution";
    }

    private static SQLException findSqlException(Throwable failure) {
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < 32; t = t.getCause()) {
            if (t instanceof SQLException) { return (SQLException) t; }
        }
        return null;
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable root = failure;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth++ < 32) {
            root = root.getCause();
        }
        return root;
    }

    private static String safeSqlState(String state) {
        if (state == null || state.length() > 16) { return null; }
        for (int i = 0; i < state.length(); i++) {
            char c = state.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) { return null; }
        }
        return state;
    }

    private void appendMetrics(StringBuilder b) {
        if (statistics == null) {
            b.append("null");
            return;
        }
        b.append("{\"executedStatements\":").append(number(statistics.getExecutedStatementsCountOrNull()))
                .append(",\"affectedRowsReported\":")
                .append(statistics.getUpdateCount() > 0 ? Long.toString(statistics.getUpdateCount()) : "null")
                .append(",\"queryRowsEmitted\":").append(statistics.getQueryRowsCount())
                .append(",\"connections\":[");
        boolean first = true;
        for (ExecutionStatistics.ConnectionInfo c : statistics.getConnections()) {
            if (!first) { b.append(','); }
            first = false;
            b.append("{\"id\":").append(quote(c.getId()))
                    .append(",\"openAttempts\":").append(c.getOpenAttempts())
                    .append(",\"opened\":").append(c.getOpens())
                    .append(",\"openFailures\":").append(c.getOpenFailures())
                    .append(",\"initializationNanos\":").append(c.getInitializationNanos())
                    .append(",\"closeAttempts\":").append(c.getCloseAttempts())
                    .append(",\"closed\":").append(c.getCloses())
                    .append(",\"closeFailures\":").append(c.getCloseFailures()).append('}');
        }
        b.append("]}");
    }

    private static String number(Number value) { return value == null ? "null" : value.toString(); }

    /**
     * Writes a complete report using same-directory temporary storage and atomic replacement where supported.
     * When atomic moves are unsupported, replacement falls back to a normal replace-existing move.
     */
    public void write(Path destination) throws IOException {
        Path target = destination.toAbsolutePath();
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Report destination has no parent directory");
        }
        Path temporary = Files.createTempFile(parent, ".scriptella-report-", ".tmp");
        try {
            Files.write(temporary, toJson().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Best-effort fallback; this move does not promise atomic replacement.
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String quote(String value) {
        if (value == null) { return "null"; }
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 32) {
                b.append(String.format("\\u%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }
}
