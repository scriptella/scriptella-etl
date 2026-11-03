# Scriptella 1.7.0 release notes

Scriptella 1.7.0 reduces repeated SQL parsing overhead in data imports and adds
a JSON and JSON Lines driver, optional CLI progress, and machine-readable
execution reports. Java 17 remains the minimum runtime.

- Read and write structured JSON with typed templates. The distribution includes
  runnable examples. JSON exports are nontransactional and can remain partial
  after failure. See the [JSON guide](../../json.md).
- Use `--progress` for periodic progress on stderr. Counts describe observed
  execution, not committed rows. See [CLI usage](../../cli-usage.md).
- Request a JSON execution report for success, failure or cancellation, with
  available execution metrics and sanitized error information. Batch,
  transaction, throughput, warning and other advanced metrics are deferred.
  See the [report guide](../../reports/execution-report.md).
- Optionally close connections after final use. This commits pending work and
  is disabled by default; later errors cannot roll back that committed work.
  See [connection lifecycle](../../connection-lifecycle.md).
- Reduce repeated parsing and parameter setup for eligible static SQL, including
  repeated INSERTs in CSV imports, by caching the binding plan while resolving
  fresh values for each execution. Dynamic SQL keeps its existing parser (#71).
- Failed ETL and `execute-sql` runs now show elapsed time. Cancellation from
  `new-tx` scripts propagates and causes rollback of pending transactions.

Vendor JDBC drivers remain external. JSON use outside the bundled distribution
requires Jackson Core. See the [changelog](../../../CHANGELOG.md) for details.
