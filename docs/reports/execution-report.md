# JSON execution reports

**Upcoming in 1.7** — available in source builds; not included in the current stable release.

Use `--report=FILE` with one ETL XML execution or `execute-sql` to write a
machine-readable report separately from console output. Scriptella writes a
same-directory temporary file and prefers atomic replacement. If atomic moves
are unsupported, it falls back to replace-existing. If report writing fails
after an execution error, the execution error remains primary. A hard process
termination cannot guarantee a report. The CLI rejects a report path that
normalizes to or resolves as the input ETL/SQL file (when the report destination
exists), and rejects `--report` with `--template`.

No report is written when the command line is rejected or the input file is
missing, because execution never starts. CI and automation should treat the
process exit code as authoritative and remove a stale report or use a unique
report filename before each run.

Embedded callers can use `EtlExecutor.executeReport()` or
`executeReport(ProgressIndicator)`. These return a report for success, failure,
cancellation, and setup failures. Existing `execute()` methods retain their
exception behavior; `EtlExecutorException.getStatistics()` contains finalized
partial statistics when an execution context was prepared.

## Schema version 1

The root object contains `schemaVersion`, `scriptellaVersion`, `executionId`,
`outcome`, `started`, `finished`, `elapsedNanos`, `complete`, `error`,
`metrics`, and `elements`. Timestamps are ISO-8601 UTC. `elapsedNanos` is a
monotonic measurement from report creation through report finalization.
`outcome` is `success`, `failed`, or `cancelled`; `complete` is true only for
success. Partial counters remain available after failure or cancellation.

The terminal `error` has a category (`setup`, `execution`, or `cancellation`),
an ETL location when known, the class name of the underlying SQL exception or
root cause, a generic message, and optional SQLState/vendor code. Driver messages, SQL, scripts, connection URLs,
credentials, bound values, and stack traces are not serialized.

`metrics` is null when statistics are suppressed with `--no-stat` or
`setSuppressStatistics(true)`. Suppressed runs do not collect query-row or
connection-lifecycle counters. Otherwise `metrics` contains:

| Field | Meaning |
| --- | --- |
| `executedStatements` | Existing execution-statistics statement count. An observed nonzero count from a legacy provider is retained; an ambiguous zero is null unless the provider declares counter support. |
| `affectedRowsReported` | Positive sum of affected rows reported by existing connection counters; null when no positive count was observed. On failure, Scriptella captures this before rollback and cleanup. It may be partial when a provider cannot report a count or pending batches have not flushed, does not imply committed rows, and JDBC batch drivers may report aggregate counts only. |
| `queryRowsEmitted` | Rows delivered from query callbacks to nested ETL elements, including rows whose child processing fails. Nested queries count emissions at each query level. |
| `connections` | Basic Scriptella driver connection open/close attempts, outcomes, and initialization time, grouped by configured connection ID. It does not describe provider-internal resources. |

Element entries aggregate the existing successful/failed invocation counts,
statement counts, and exclusive `workingNanos` by ETL location. A zero counter
is null when the provider has not declared counter support, because zero can
mean unsupported; observed nonzero counters from older providers remain
reportable. Query elements also include `queryRowsEmitted`. A single failure
can propagate through nested elements, so failed invocation counts are not
independent root errors.

Batch result details and timings, transaction telemetry, JDBC warning counts,
provider-specific metric reconciliation, and throughput fields are deferred.
Existing JDBC batching and affected-row aggregation remain unchanged. Future
versions may add optional fields after provider and driver behavior is better
specified.

## Examples

See [success](examples/success.json), [partial failure](examples/partial-failure.json)
and [cancellation](examples/cancelled.json). The examples are parsed in the core
test suite to keep them valid JSON.
