# Executing Scriptella from Java

Use `scriptella.execution.EtlExecutor` to run an ETL job from an application,
batch worker, or integration task. `newExecutor(...)` loads an ETL XML file;
`newSqlFileExecutor(...)` creates a normal executor for a single SQL file.
Both execute through the same engine and return `ExecutionStatistics`.

Include `org.scriptella:scriptella-core` and the JDBC drivers or Scriptella
adapters used by your job on the application's classpath. Optional adapters
are provided by `org.scriptella:scriptella-drivers`. Use matching Scriptella
artifact versions. `newSqlFileExecutor(...)` is part of Scriptella 1.6.
The XML examples also apply to the current published release.

The examples below are Java method fragments. Handle or declare their checked
exceptions in the surrounding application code. Common imports are:

```java
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.execution.ExecutionStatistics;

import java.io.File;
import java.net.URL;
import java.util.Map;
```

## Run an existing ETL job

A batch application can execute the same file used with the command-line launcher:

```java
EtlExecutor executor = EtlExecutor.newExecutor(new File("jobs/import.etl.xml"));
ExecutionStatistics statistics = executor.execute();
System.out.println("Executed statements: " + statistics.getExecutedStatementsCount());
```

The factory parses the XML configuration. Execution opens the configured
connections, runs the job, commits on success, and closes its resources.
Execution failures trigger an attempt to roll back. Rollback depends on the
providers and database: DDL, autocommit, file writes, and external processes
may have effects that cannot be undone.

Relative connection URLs, driver classpaths, and includes use the ETL file's
location as their base. Resolve the job file explicitly in applications where
the working directory can vary.

## Supply parameters for each import

For jobs whose input changes between invocations, pass an explicit map to the
XML factory rather than changing JVM-wide properties:

```java
URL job = new File("jobs/import.etl.xml").toURI().toURL();
Map<String, Object> parameters = Map.of(
    "input.file", "/data/imports/people.csv",
    "target.schema", "staging"
);
EtlExecutor executor = EtlExecutor.newExecutor(job, parameters);
ExecutionStatistics statistics = executor.execute();
```

Reference these values in the ETL as `${input.file}` and `${target.schema}`.
External parameters override properties declared in the ETL XML.
`newExecutor(File)` and `newExecutor(URL)` use JVM system properties by default;
`newExecutor(URL, Map)` uses the supplied map as its external parameters.
Pass an empty map when no external parameters should be supplied.

For a job packaged as a classpath resource:

```java
URL job = MyApplication.class.getResource("/jobs/import.etl.xml");
if (job == null) {
    throw new IllegalStateException("Missing /jobs/import.etl.xml");
}
EtlExecutor executor = EtlExecutor.newExecutor(job, parameters);
executor.execute();
```

Replace `MyApplication` with a class in your application. Relative resources
are resolved from that URL, so use explicit filesystem paths for external input
and output files when the ETL is packaged inside a JAR.

## Apply a SQL setup or deployment script

For one JDBC connection and one SQL file, create an executor without an XML wrapper:

```java
EtlExecutor executor = EtlExecutor.newSqlFileExecutor(
    new File("schema.sql"),
    "jdbc:postgresql://localhost/app",
    databaseUser,
    databasePassword
);
ExecutionStatistics statistics = executor.execute();
System.out.println("JDBC rows updated: " + statistics.getUpdateCount());
```

Obtain `databaseUser` and `databasePassword` from your application's credential
configuration. The factory builds an in-memory ETL configuration containing one
connection and one script. The UTF-8 SQL file is read at execution time; the
factory does not open a database connection.

The available overloads are:

```java
EtlExecutor.newSqlFileExecutor(file, url);
EtlExecutor.newSqlFileExecutor(file, url, user, password);
EtlExecutor.newSqlFileExecutor(file, url, user, password, driver, substitution);
```

Credentials may be null. The first overload supplies neither username nor
password. Drivers normally register through JDBC automatically. For explicit
loading, pass a JDBC class name such as `"org.postgresql.Driver"` in the final
overload; pass null to use automatic registration. The driver JAR must be on the
application's classpath.

SQL variables come from a snapshot of JVM system properties taken when the
factory is called. For example, start the application with
`-Dschema=myapp -Denvironment=prod` and use:

```sql
CREATE SCHEMA ${schema};
INSERT INTO deployment(environment) VALUES (?environment);
```

Use `?name` bindings for data values. Text substitution, including
`'${environment}'`, does not escape SQL. Set `substitution` to false when dollar
or question-mark expressions must remain literal:

```java
EtlExecutor executor = EtlExecutor.newSqlFileExecutor(
    new File("literal.sql"), url, user, password, null, false
);
executor.execute();
```

`getUpdateCount()` sums positive update counts reported by connections, including
batches flushed during commit. Zero can also mean that counts are unsupported
or statistics were suppressed. It is not a count of query rows.

Use ETL XML query elements when you need result processing, transformations,
multiple data sources, or orchestration. See the [CLI guide](cli-usage.md#10-direct-sql-execution)
for running a SQL file from a shell.

## Report progress and handle failures

Connect progress reporting to a batch status display or application callback:

```java
ExecutionStatistics statistics = executor.execute((progress, message) -> {
    System.out.printf("%.0f%% %s%n", progress * 100, message);
});
```

Progress ranges from zero to one. Keep the callback lightweight; it runs as part
of execution. Use the returned statistics for completion information.

```java
try {
    executor.execute();
} catch (EtlExecutorException failure) {
    if (failure.isCancelled()) {
        // Record that this job was cancelled.
    } else {
        // Record failure and inspect failure.getCause() or getLastProvider().
        throw failure;
    }
}
```

XML loading errors occur during `newExecutor(...)` and may throw an unchecked
`ConfigurationException`; execution failures are reported by
`EtlExecutorException`. Exception details can contain SQL and parameter values,
so apply your application's logging policy to them.

## Run a job in a background worker

`EtlExecutor` implements `Callable<ExecutionStatistics>` and `Runnable`.
Use the callable form when a background job needs its statistics and execution
failure:

```java
java.util.concurrent.ExecutorService worker =
    java.util.concurrent.Executors.newSingleThreadExecutor();
try {
    java.util.concurrent.Future<ExecutionStatistics> task =
        worker.submit((java.util.concurrent.Callable<ExecutionStatistics>) executor);
    try {
        ExecutionStatistics statistics = task.get(5, java.util.concurrent.TimeUnit.MINUTES);
    } catch (java.util.concurrent.TimeoutException timeout) {
        task.cancel(true);
        throw timeout;
    }
} finally {
    worker.shutdown();
}
```

The cast selects the callable overload because the executor implements both
interfaces. `Future.get()` wraps execution failures in `ExecutionException`.
`cancel(true)` interrupts execution and requests cancellation; it does not wait
for database work or cleanup to finish. Scriptella checks for interruption and
attempts rollback, but a JDBC call may take time to respond.

`run()` discards statistics and wraps an `EtlExecutorException` in an unchecked
`SystemException`. It fits integrations that accept only `Runnable`.

## Configure monitoring and repeat execution

Java executors have JMX disabled by default. Enable it when the application
needs Scriptella's management beans:

```java
executor.setJmxEnabled(true);
```

Call `setSuppressStatistics(true)` to disable statistics collection when it is
unnecessary. Omit completion counts in that mode.

An executor can be reused for sequential runs. Each run creates a new execution
session; a SQL executor rereads its file on each run. Create a new executor when
parameters or configuration change, and finish setting executor options before
starting execution.

Connection settings passed to `newSqlFileExecutor` use the shared property and
expression substitution mechanism, including `${env.DB_URL}`,
`${env.DB_USER}`, and `${env.DB_PASSWORD}`. The reserved `env` namespace reads
process environment variables and fails if a referenced variable is missing.
Connection settings are expanded regardless of the SQL `substitution` flag.
See [environment variables](cli-usage.md#environment-variables) for the lookup
contract and credential transport guidance.
