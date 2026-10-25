# Connection lifecycle and early closure

## Default connection lifecycle

Connections open during ETL initialization by default. Setting
`lazy-init="true"` on a connection defers opening until its first use. Scripts
and queries referencing the same connection ID reuse its connection. A script
with `new-tx="true"` uses a separate connection for its transaction, retained
under the same connection ID's ownership until cleanup.

On successful ETL completion, Scriptella commits pending transactions, including
flushing pending JDBC batches. On failure or cooperative cancellation, it
attempts rollback. Rollback cannot undo work already committed through
configured autocommit behavior or output from nontransactional drivers.

Connections are closed during final cleanup, regardless of whether execution
succeeds or fails. Lazy connections that were never used remain unopened.

## Closing connections after final use

Starting with Scriptella 1.7.0, set `close.after.use=true` inside a connection to
opt into **early commit and close**. The default lifecycle remains unchanged
unless this option is enabled:

```xml
<connection id="source" driver="csv" url="input.csv">
    close.after.use=true
</connection>
<connection id="target" driver="h2" url="jdbc:h2:./result">
    close.after.use=true
    statement.batchSize=100
</connection>

<query connection-id="source">
    <script connection-id="target">
        INSERT INTO result VALUES (?1, ?2)
    </script>
</query>
<!-- Both connections have now been committed and closed. -->
```

**This introduces an early transaction boundary. Changes committed here will
not be rolled back if subsequent ETL operations fail or are cancelled.** Pending
JDBC batches, including final partial batches, are flushed by the normal driver
commit operation. Nontransactional drivers retain their existing commit/close
behavior; the option does not make their output transactional.

## Configuration and final-use detection

### Defaults and precedence

Resolution order is:

1. Explicit connection property `close.after.use`.
2. Execution parameter `scriptella.close.after.use`.
3. Built-in default `false`.

An explicit connection value of `false` overrides a global `true`. The global
default uses existing ETL/external parameter resolution: external parameters
override the ETL `<properties>` section. The normal CLI and default Java executor
factory supply JVM system properties as external parameters. Embedded callers
passing their own parameter map control those external values themselves.

For example, before the connection declarations:

```xml
<properties>
    scriptella.close.after.use=true
</properties>
```

Or configure the default through the JVM, without a dedicated CLI option:

```sh
java -Dscriptella.close.after.use=true -jar scriptella.jar job.etl.xml
```

Values use Scriptella's existing boolean parser: case-insensitive `true/false`,
`yes/no`, `on/off`, or `1/0`. Invalid values cause a configuration error.

### Final-use boundary

Scriptella conservatively examines declared connection references in each
top-level script/query and its entire nested subtree, including inherited
references and `onerror` handlers. A connection is eligible only after its last
potentially referencing top-level element completes successfully. It is never
closed between query rows, repeated nested script invocations, or while an
ancestor query is active. Conditions are potential uses even if skipped at run
time; this can delay closure.

The primary connection and all `new-tx` connections owned by its connection ID
are committed and closed together, in the existing manager transaction order.
An unused eagerly opened connection is eligible before the first top-level
operation. An unopened lazy connection is retired without opening it. Retired
connections cannot be reopened. Cumulative statement/update totals and periodic
progress counters remain available after retirement.

Unknown executable structures are conservatively retained until normal cleanup.
This analysis concerns declared ETL references; custom driver code must not retain
and later reuse another connection outside those declarations.

If an element fails or is cancelled before completing, its connections remain
under normal rollback/cleanup handling. Early commit failures also propagate to
normal rollback/cleanup; as with ordinary final commits, earlier successful
commits cannot be undone. Close errors follow existing cleanup behavior: they
are logged as warnings. Already retired connections are skipped by final
commit, rollback, and close operations.
