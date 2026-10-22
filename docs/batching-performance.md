# Batching and insert performance

For repeated inserts, enable Scriptella batching and check the JDBC driver's
batch optimization settings. A batch size alone does not determine how the
driver sends the work to the database. PostgreSQL insert rewriting and MariaDB
bulk execution use different mechanisms and defaults.

This guide gives starting points based on the
[CSV import benchmarks](https://github.com/scriptella/scriptella-benchmarks).
The best settings depend on your SQL, row sizes, driver version, database and
network. Batch size 1000 is a tested starting point, not a universal optimum.

## Controls to understand

| Property | What it controls | Starting point |
| --- | --- | --- |
| `statement.batchSize` | Number of statements queued per batch group before execution; `0` disables batching (the default). | Try `1000` for repeated inserts. |
| Driver batch options | Whether eligible JDBC batches use SQL rewriting or a bulk protocol. | See the database examples below. |
| `autocommit` | Whether JDBC commits statements automatically; Scriptella defaults to `false`. | Keep `false` for transactional loads. |
| `autocommit.size` | Periodic commits after a specified number of statements. | Leave unset unless partial commits are intentional. |
| `flushBeforeQuery` | Flushes pending batch updates before a query on the connection. | Set `true` when queries must see pending writes. |
| `statement.fetchSize` | A JDBC hint for reading query results. | Tune separately for source queries; it does not enable insert batching. |

Executing a batch does not commit a transaction. With `autocommit=false` and
no periodic commits, batching retains the ETL transaction boundary. If partial
commits are intentional, `autocommit.size=10000` can commit every 10,000
statements alongside `statement.batchSize=1000`; commits flush pending batches.
Periodic commits prevent all-or-nothing rollback and may reduce throughput,
so leave this setting unset unless that tradeoff is needed.

## PostgreSQL

For compatible repeated INSERT statements, start with:

```xml
<connection id="db" driver="postgresql"
            url="jdbc:postgresql://localhost:5432/example"
            user="username" password="password"
            classpath="lib/postgresql-VERSION.jar">
    autocommit=false
    statement.batchSize=1000
    reWriteBatchedInserts=true
</connection>
```

The `postgresql` adapter alias requires the `scriptella-drivers` module;
core-only users can instead specify `driver="org.postgresql.Driver"`.

Replace the driver JAR path with your installed pgJDBC JAR. Scriptella forwards
`reWriteBatchedInserts` to pgJDBC, which defaults it to `false`; Scriptella does
not enable it automatically. Setting it to `true` lets pgJDBC combine compatible
batched inserts into multi-row INSERT statements. It does not optimize arbitrary
SQL or enable Scriptella batching by itself. See the
[pgJDBC connection parameters](https://jdbc.postgresql.org/documentation/use/).

Keep driver settings in one place. In pgJDBC, a property in the JDBC URL takes
precedence over the same property in the connection body.

The repeated benchmark comparison supports trying rewriting for this kind of
load. It does not establish that every INSERT shape, trigger, generated-key
workflow or batch update-count requirement behaves identically with rewriting.
Test the SQL and error handling your application uses.

## MariaDB

With MariaDB Connector/J, start with Scriptella batching and the driver defaults:

```xml
<connection id="db" driver="org.mariadb.jdbc.Driver"
            url="jdbc:mariadb://localhost:3306/example"
            user="username" password="password"
            classpath="lib/mariadb-java-client-VERSION.jar">
    autocommit=false
    statement.batchSize=1000
</connection>
```

Replace the JAR path with your installed MariaDB Connector/J JAR. This example
uses the JDBC driver class explicitly; the advice concerns MariaDB Connector/J,
not MySQL Connector/J.

The tested Connector/J **3.5.7** defaults to
`useBulkStmtsForInserts=true` and `useBulkStmts=false`. Eligible insert batches
can therefore already use MariaDB's bulk protocol without an extra rewrite flag.
Keep those defaults as a baseline; if overriding them for comparison, put the
properties in the connection body alongside `statement.batchSize`.

`rewriteBatchedStatements` is a separate option, defaulting to `false`. It was
removed in 3.0.0 and reintroduced in 3.5.6; enabling it also forces
`useServerPrepStmts=false`. Do not copy rewrite options from another connector
or assume that enabling more options improves throughput. Check the
[MariaDB Connector/J documentation](https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j)
for your installed version. The benchmark retained MariaDB defaults and did not
compare alternative MariaDB driver settings.

## Write SQL that can batch effectively

Use repeated parameterized SQL with a stable statement shape, for example inside
a source query that supplies `id` and `name`:

```xml
<script connection-id="db">
    INSERT INTO person (id, name) VALUES (?id, ?name);
</script>
```

Scriptella groups prepared statements by SQL text. Different SQL texts go into
different batch groups; statements without parameters use a separate group.
Avoid mixing parameterized inserts, non-parameterized SQL and schema changes
on the same batched connection when execution order matters. Prefer a dedicated
batched output connection and separate setup steps.

If a query on the output connection must see pending batch writes, enable
`flushBeforeQuery=true`. A flush sends pending work; it does not itself commit
it. See the [JDBC batching reference](https://scriptella.org/docs/api/scriptella/jdbc/package-summary.html#batching)
for grouping rules and related properties.

## Tune and verify your load

1. Establish a correct baseline with representative data, SQL and transaction
   settings. Record the actual JDBC driver and database versions.
2. Try batch size 1000, then compare smaller and larger sizes, such as 100 and
   5000. Larger batches can increase memory use and hit driver or server limits,
   especially with wide rows or large values.
3. Change one setting at a time. Keep transaction boundaries and durability
   settings fixed when measuring batching or rewriting. Repeat runs under
   comparable conditions and measure the complete import.
4. Verify stored values as well as row counts, including a final partial batch.
   Exercise constraint failures and rollback behavior. Batch execution can defer
   errors until a flush; check any reliance on per-statement update counts or
   generated keys.

If batching is still insufficient for a direct file import, evaluate a native
bulk loader. PostgreSQL COPY was faster in the measured workload, but uses a
different loading path. Native loading is not a drop-in replacement for every
Scriptella transformation or SQL workflow.

## Benchmark evidence

The measurements motivate these recommendations; they are not throughput
promises or a general ranking of databases. The benchmark covers a specific
million-row CSV import, including parsing, conversion, startup and commit.

- [Repeated PostgreSQL rewrite comparison](https://github.com/scriptella/scriptella-benchmarks/tree/main/docs/results/2026-10-21-pg-rewrite):
  rewriting off versus on, with the same batch size and transaction settings.
- [Benchmark v4 results](https://github.com/scriptella/scriptella-benchmarks/tree/main/docs/results/2026-10-21-ci-v4):
  batching with PostgreSQL rewriting enabled and MariaDB defaults.
- [Methodology and reproduction](https://github.com/scriptella/scriptella-benchmarks/blob/main/docs/benchmark-guide.md):
  exact versions, configuration, correctness checks and interpretation limits.
