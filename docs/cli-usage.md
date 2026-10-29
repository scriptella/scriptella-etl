# Scriptella command-line usage contract

This document describes the Scriptella command-line launcher for interactive
use, shell scripts, CI systems, and other automated callers. Treat exit status
as authoritative; do not infer success from log text.

## 1. Resolve the executable

Use an absolute path in `SCRIPTELLA_JAR`. Resolve it in this order:

1. A caller-supplied `SCRIPTELLA_JAR`.
2. `scriptella.jar` at the root of an unpacked binary distribution.
3. `build/scriptella.jar` in a source checkout after `ant jar`.

Reject a missing or unreadable file. Confirm the selected JAR:

```sh
test -r "$SCRIPTELLA_JAR"
java -jar "$SCRIPTELLA_JAR" --version
```

`--version` exits `0` and writes one version line to stdout. Do not select a JAR
by recursively taking the first file named `scriptella.jar`.

## 2. Canonical invocation

Use absolute paths for both the JAR and ETL file:

```sh
java -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE"
```

`--quiet` suppresses INFO progress and statistics. `--no-jmx` suppresses JMX
registration. On an ordinary successful run in which no ETL connection writes
to the console:

- exit status: `0`
- stdout: empty
- stderr: empty

Run without `--quiet` only when human-readable progress is required. Never parse
timestamps, progress messages, exception text, or execution statistics as a
machine protocol.

Supported launcher options are `-h`/`--help`, `-d`/`--debug`, `-q`/`--quiet`,
`-v`/`--version`, `--check`, `--no-stat`, `--no-jmx`, `--progress[=Ns|Nm]`,
`--report=FILE`, and `-t`/`--template`. See
[section 11](#11-periodic-execution-progress) for periodic progress and the
[execution report guide](reports/execution-report.md) for `--report=FILE`.
Scriptella 1.6 also accepts the `execute-sql` command documented in
[section 10](#10-direct-sql-execution). Put JVM options, including `-D`
properties, before `-jar`; put launcher options after the JAR:

```sh
java -Dinput.file=/data/input.csv \
     -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE"
```

Compatibility note: single-dash long forms (`-help`, `-version`, `-debug`,
`-quiet`, `-template`, `-nostat`, `-nojmx`) remain accepted for backward
compatibility. Prefer the canonical double-dash options above in new scripts.

## 3. Exit codes and failure behavior

| Code | Meaning |
|---:|---|
| `0` | All selected ETL files or the direct SQL file executed successfully, all `--check` files passed, or help/version/template generation succeeded. |
| `1` | At least one ETL execution, the direct SQL execution, or `--check` failed, or template generation failed. |
| `2` | An ETL input file could not be resolved, or the `execute-sql` file is missing. No ETL files are executed if resolution of any command-line file fails. |
| `3` | An unrecognized launcher option or invalid progress interval was supplied, or `execute-sql` arguments are missing or invalid. |

When several ETL files resolve successfully, the launcher processes them in
argument order. A runtime or checking failure does not stop later files; the
final status is `1` if any file failed. Scriptella attempts transaction rollback
after an execution failure, but rollback is not guaranteed for non-transactional
connections, autocommit connections, files, or external processes.

Unrecognized options print `Unrecognized option <option>` to stderr, return
exit code `3`, and execute no ETL files. Only complete option names are
accepted; undocumented prefixes are rejected.

## 4. ETL file and resource resolution

The launcher applies these rules:

| Argument | Resolution |
|---|---|
| No ETL argument | `etl.xml` in the current working directory. |
| Existing path | Use that path unchanged, then convert it to an absolute file. |
| Missing path with no `.` anywhere in the argument | Append `.etl.xml`. |
| Missing path containing `.` | Fail with exit `2`; do not append a suffix. |

Examples: `jobs/load` resolves only as `jobs/load.etl.xml`; `jobs/load.v2`
does not become `jobs/load.v2.etl.xml`.

When no ETL filename is supplied and the default `etl.xml` is missing, the
launcher prints a short message that includes a `--help` hint and exits `2`.
When an explicitly supplied file is missing, the concise missing-file message is
used without that no-argument hint.

Connection URLs, connection `classpath` entries, and `<include href="...">`
resources are resolved relative to the directory containing the ETL file.
Prefer absolute paths for generated or environment-specific runtime values.

## 5. Experimental static checking

Use `--check` when a developer or coding agent needs to load an ETL file without
executing it:

```sh
scriptella.sh --check my-job.etl.xml
```

The checker reuses Scriptella's normal `ConfigurationFactory` and configuration
model, including XML/DTD validation and model-level connection-reference
validation. It also checks that configured driver names resolve to a compatible
JDBC or Scriptella driver class. It does not create a `Session`, initialize a
driver, open a configured Scriptella/JDBC connection, or execute scripts or
queries. Configuration parsing may still read external resources referenced by
the ETL, such as included properties or XML resources, and it does not validate
runtime SQL or external-system behavior.

Do not treat `--check` as a sandbox for untrusted ETL files.

Exit status `0` means that Scriptella loaded the configuration and the
implemented static checks passed. A nonzero status means loading or checking
failed. Passing `--check` does not guarantee that the ETL will execute
successfully or safely against real systems.

## 6. Validate before execution

For strict XML/DTD validation independent of the model checker, use the
following helper without executing the ETL:

```sh
sh docs/agent-templates/validate-etl.sh "$SCRIPTELLA_JAR" "$ETL_FILE"
```

The validator requires `unzip` and `xmllint`, uses the DTD bundled in the
selected JAR, disables network access, prints `VALID <ETL_FILE>` to stdout on
success, and returns nonzero on failure.

DTD validation checks XML structure only. It does not open connections, load
JDBC drivers, expand every runtime property, read every include, parse SQL, or
predict side effects. There is no general safe command that performs those
checks: a normal Scriptella run may initialize connections and execute work.

## 7. External properties

The CLI exposes JVM system properties to ETL files. Pass non-secret values with
`-Dname=value` before `-jar` and reference them as `${name}`:

```sh
java -Dinput.file=/data/input.csv \
     -Doutput.file=/data/output.csv \
     -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE"
```

External JVM properties take precedence over values declared inside the ETL
`<properties>` element. Environment variables are not imported as ETL
properties automatically.

For credentials, use a protected external properties file or the reserved
[`${env.*}` namespace](#environment-variables). With a protected file, pass
only its path on the command line:

```xml
<properties>
    <include href="${properties.file}"/>
</properties>
```

```sh
java -Dproperties.file=/run/secrets/job.properties \
     -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE"
```

The properties file can contain `target.user`, `target.password`, and other
values used by the ETL. Restrict it to the executing account (for example,
mode `0600`), keep it outside the repository, and delete it through the
credential provider's normal lifecycle. Do not put secret values in ETL XML,
shell history, process arguments, logs, issue reports, or generated SQL.

## 8. Output contract

Launcher help and version output go to stdout. Launcher progress, statistics,
warnings, and failures use Java logging and normally go to stderr. Missing-file
and unrecognized-option messages also go to stderr. Shutdown cancellation
messages go to stdout.

ETL drivers may have their own output behavior. In particular, a text, CSV, or
shell connection without an output URL can write to the process console.
Therefore stdout/stderr are empty-on-success only when every ETL connection has
an explicit destination and no invoked process writes to the console.

Capture all three results independently:

```sh
java -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE" \
    >run.stdout 2>run.stderr
rc=$?
```

## 9. Copyable templates

Templates are in [`docs/agent-templates/`](agent-templates/):

| Template | Purpose |
|---|---|
| `csv-to-sql.etl.xml` | Read CSV rows and execute parameterized SQL through JDBC. |
| `jdbc-to-jdbc.etl.xml` | Copy selected JDBC columns into another JDBC database. |
| `sql-script.etl.xml` | Execute an external SQL file through JDBC. |
| `file-transform.etl.xml` | Transform matching text lines into another file. |
| `validate-etl.sh` | Validate ETL XML against the DTD without execution. |

The nested `<query>` / `<script>` form means the nested script runs once for
each source row. JDBC templates use `?name` parameters instead of interpolating
row values into SQL strings.

Required properties are exact:

| Template | Required properties |
|---|---|
| `csv-to-sql.etl.xml` | `source.file`, `target.driver`, `target.url`, `target.user`, `target.password`, `target.classpath` |
| `jdbc-to-jdbc.etl.xml` | `source.driver`, `source.url`, `source.user`, `source.password`, `source.classpath`, plus the five corresponding `target.*` properties |
| `sql-script.etl.xml` | `db.driver`, `db.url`, `db.user`, `db.password`, `db.classpath`, `sql.file` |
| `file-transform.etl.xml` | `source.file`, `target.file` |

The three JDBC templates also require the external
`properties.file` property naming the protected file that contains the table's
required values. Invoke one as:

```sh
java -Dproperties.file=/run/secrets/job.properties \
     -jar "$SCRIPTELLA_JAR" --quiet --no-jmx \
     /absolute/path/to/csv-to-sql.etl.xml
```

Invoke the credential-free file template as:

```sh
java -Dsource.file=/data/input.txt -Dtarget.file=/data/output.txt \
     -jar "$SCRIPTELLA_JAR" --quiet --no-jmx \
     /absolute/path/to/file-transform.etl.xml
```

## 10. Direct SQL execution

Run a UTF-8 SQL file without an ETL XML wrapper:

```bash
SCRIPTELLA_JAVA_OPTS="-Dschema=myapp -Denvironment=prod" \
  scriptella.sh execute-sql \
  --url jdbc:postgresql://localhost/app \
  --user app \
  schema.sql
```

Place your JDBC driver JAR in the distribution's `lib/` directory. JDBC driver
registration is automatic; `--driver org.postgresql.Driver` explicitly loads a
class if needed. Use `execute-sql --help` for all options. Common options such as
`--debug`, `--quiet`, `--no-jmx`, `--no-stat`, and `--report=FILE` work before or after `execute-sql`.

`--password VALUE` is convenience syntax; it can expose the password in shell
history and process listings. Prefer a protected configuration path such as
`${env.DB_PASSWORD}`, described under [Environment variables](#environment-variables).

`execute-sql` requires Scriptella 1.6 or newer.

JVM system properties are available to Scriptella's normal SQL substitution,
using the same property mechanism as ETL XML execution.
For example, `CREATE SCHEMA ${schema};` substitutes an identifier and
`INSERT INTO deployment(environment) VALUES (?environment);` binds a data value.
Text substitution such as `'${environment}'` does not escape SQL. Use
`--no-substitution` to preserve all dollar and question-mark expressions literally.
`SCRIPTELLA_JAVA_OPTS='-Denvironment=staging'` supplies JVM system properties
through the shell launcher.

The command accepts exactly one SQL file, using its path literally without the
ETL filename fallback rules. Exit status is `0` for success, `1` for execution
failure, `2` for a missing SQL file, and `3` for invalid or missing arguments.
Successful completion is written to stdout unless `--quiet` is used;
`--no-stat` omits the completion counts. Errors are written to stderr, with
stack traces enabled by `--debug`. `--check` applies only to ETL XML files.

Execution commits on success and attempts rollback on failure; DDL transaction
behavior depends on the database. Completion reports statements executed and the
sum of positive JDBC update counts. Queries and result dumping require normal
ETL query elements.

For execution inside an application, see [Executing Scriptella from Java](java-execution.md)
for `EtlExecutor`, direct SQL files, job parameters, progress, and background execution.

## Environment variables

`${env.FOO}` reads `FOO` from the environment of the Scriptella process. Use it
in ETL configuration attributes, property values, and expressions, or in direct
SQL connection settings:

```xml
<connection driver="jdbc" url="${env.DB_URL}"
            user="${env.DB_USER}" password="${env.DB_PASSWORD}"/>
```

```bash
scriptella.sh execute-sql --url '${env.DB_URL}' \
  --user '${env.DB_USER}' --password '${env.DB_PASSWORD}' schema.sql
```

Export the variables before starting Scriptella. Single quotes in the command
keep the shell from interpreting Scriptella's expressions. `--driver` also
supports substitution. Connection settings are expanded even with
`--no-substitution`, which disables substitution only in the SQL file.

The `env` namespace is reserved and read-only: ETL or external properties named
`env` or `env.FOO` cannot shadow it. A referenced missing environment variable is
a configuration error naming the variable, without printing its value. An empty
but defined variable is valid. Ordinary `${foo}` references keep their existing
behavior, including unresolved references; external properties still take
precedence over ETL properties. Environment variables are not merged into that
property namespace.

Environment variables are a convenient configuration and credential transport
mechanism, not a secret store. They can be exposed by process inspection, child
processes, or operational tooling. Keep credentials out of source control and
avoid substituting them into SQL text or scripts that may be logged. SQL text
substitution does not escape values. Scriptella redacts connection URL and user
values from connection debug descriptions and masks passwords; treat provider
and application output as potentially sensitive. Debug mode does not enable raw
JDBC DriverManager logging, since it can print credentials before sanitization.
Applications that explicitly configure a DriverManager log writer are responsible
for protecting that output.

## Open issues / non-guarantees

- `--check` is experimental and intentionally performs only lightweight static
  checks; it is not a general-purpose ETL linter or dry run.
- Launcher logs are human-readable and have no stable JSON or event schema.
- Success output is not intrinsically isolated from driver or child-process
  output.
- JVM `-D` values are visible in process arguments on many systems; use them
  only for non-secrets or a protected secret-file path.
- Provider exceptions can contain connection details; treat captured stderr as
  potentially sensitive even when credentials came from a protected file or
  environment variables.
- DTD validation does not verify includes, property completeness, JDBC
  classpaths, SQL syntax, permissions, transactionality, or idempotency.
- File-producing drivers and non-transactional systems cannot provide the same
  rollback guarantee as a transactional JDBC connection.


## 11. Periodic execution progress

**Upcoming in 1.7** — available in source builds; not included in the current stable release.

Scriptella can report live execution activity on **stderr**, without JMX or
verbose logging:

```sh
scriptella.sh --progress import.etl.xml
scriptella.sh --progress=10s import.etl.xml
scriptella.sh --progress=2m import.etl.xml
java -jar "$SCRIPTELLA_JAR" --quiet --no-stat --no-jmx --progress=10s "$ETL_FILE"
```

Bare `--progress` uses 30 seconds. An override must use `=` and a positive whole
number followed by lowercase `s` (seconds) or `m` (minutes). Minimum: 1 second.
The duration must fit in a signed 64-bit nanosecond interval. Empty values,
missing suffixes, fractions, zero, negative numbers, other units and overflow
are rejected with exit code `3`. For example, `--progress=`, `--progress=10`,
`--progress=0s`, and `--progress=0.5s` are invalid. `--progress 10s` is not an
interval override: the duration must be attached to the option.

Output appears approximately every interval, starting one interval after the
executor starts. No final periodic line is forced for a short job:

```text
[00:30] 185,000 statements | 6,167/s
[01:00] 371,000 statements | 6,183/s
```

Elapsed time uses a monotonic clock; minutes continue beyond 59. Throughput is
the cumulative count divided by elapsed seconds, rounded to a whole number.
Each ETL file gets a fresh timer and count. Multi-file output appends the active
file's path. The option also works before or after `execute-sql`; `--check` and
template generation do not execute ETL and cannot be combined with progress.

These are **Scriptella statements**, not migrated rows, server-confirmed
executions or committed transactions. JDBC updates can be counted when queued
in a batch, before flushing or commit. A query is counted after its callbacks
finish, so its count can remain unchanged during a long operation. Nested
queries are not summed as overlapping element statistics. Rollback does not
subtract observed statement activity. Progress is not a percentage, ETA or a
promise that the database is making progress while a statement is blocked.

JDBC, CSV, text, shell, JSON, XPath and LDAP connections expose their existing
in-memory counters. Other providers can opt in through the optional
`Connection.getProgressCounter()` method. Existing providers remain compatible;
the reporter never invokes driver/JDBC methods. Before initialization, or when
no registered connection supplies a counter, output says `statements unavailable
| throughput unavailable`. Mixed coverage (including unopened lazy connections)
is labelled `(partial: some counters unavailable)`. A numeric zero is not used
to infer counter availability. Snapshot totals are approximate live reads, not
atomic database snapshots; they survive connection cleanup.

An explicit progress request remains active with `--quiet`, `--no-stat` and
`--no-jmx`. Quiet still suppresses normal INFO logs; no-stat still suppresses
detailed execution statistics. Progress does not write to stdout. Failure
messages and exit status keep their existing behavior. The timer stops on
success, failure or cooperative cancellation. A hard process termination cannot
guarantee cleanup or a final message.

Without `--progress`, no periodic reporter or registry is created and existing
CLI output is unchanged. ETL drivers retain their own logging/output behavior;
console progress is human-readable and is not the JSON reporting protocol.
