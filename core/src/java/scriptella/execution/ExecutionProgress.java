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

import scriptella.spi.AbstractConnection.StatementCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Live statement counters for one execution. Registration happens on the
 * execution thread. Snapshots read only in-memory volatile values, including
 * counters belonging to closed connections; no driver code runs on the reader.
 * This does not collect row events or replace final execution statistics.
 */
public final class ExecutionProgress {
    private final List<StatementCounter> counters = new ArrayList<StatementCounter>();

    /** Internal connection-lifecycle hook. Null denotes unavailable/unopened. */
    public synchronized int addCounter(StatementCounter counter) {
        counters.add(counter);
        return counters.size() - 1;
    }

    /** Internal hook replacing an unopened connection's counter after lazy init. */
    public synchronized void setCounter(int index, StatementCounter counter) {
        counters.set(index, counter);
    }

    /** An approximate live total, not an atomic database/transaction snapshot. */
    public synchronized Snapshot snapshot() {
        long statements = 0;
        int available = 0;
        for (StatementCounter counter : counters) {
            if (counter != null) {
                statements += counter.statements;
                available++;
            }
        }
        return new Snapshot(statements, available, counters.size());
    }

    public static final class Snapshot {
        private final long statements;
        private final int available;
        private final int total;

        private Snapshot(long statements, int available, int total) {
            this.statements = statements;
            this.available = available;
            this.total = total;
        }

        public long getStatements() {
            return statements;
        }

        public boolean isAvailable() {
            return available > 0;
        }

        public boolean isPartial() {
            return available < total;
        }
    }
}
