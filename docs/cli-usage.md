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
`-v`/`--version`, `--no-stat`, `--no-jmx`, and `-t`/`--template`. Put JVM options,
including `-D` properties, before `-jar`; put launcher options after the JAR:

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
| `0` | All selected ETL files succeeded, or help/version/template generation succeeded. |
| `1` | At least one ETL execution failed, or template generation failed. |
| `2` | An ETL input file could not be resolved. No ETL files are executed if resolution of any command-line file fails. |
| `3` | An unrecognized launcher option was supplied. |

When several ETL files resolve successfully, the launcher executes them in
argument order. A runtime failure does not stop later files; the final status is
`1` if any file failed. Scriptella attempts transaction rollback after an
execution failure, but rollback is not guaranteed for non-transactional
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

## 5. Validate before execution

Scriptella has no validation-only or dry-run launcher option. Perform strict
XML/DTD validation without executing the ETL:

```sh
docs/agent-templates/validate-etl.sh "$SCRIPTELLA_JAR" "$ETL_FILE"
```

The validator requires `unzip` and `xmllint`, uses the DTD bundled in the
selected JAR, disables network access, prints `VALID <ETL_FILE>` to stdout on
success, and returns nonzero on failure.

DTD validation checks XML structure only. It does not open connections, load
JDBC drivers, expand every runtime property, read every include, parse SQL, or
predict side effects. There is no general safe command that performs those
checks: a normal Scriptella run may initialize connections and execute work.

## 6. External properties

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

For credentials, pass only a protected properties-file path on the command
line:

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

## 7. Output contract

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

## 8. Copyable templates

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

## Open issues / non-guarantees

- There is no first-class `--validate` or `--dry-run` launcher mode.
- Launcher logs are human-readable and have no stable JSON or event schema.
- Success output is not intrinsically isolated from driver or child-process
  output.
- JVM `-D` values are visible in process arguments on many systems; use them
  only for non-secrets or a protected secret-file path.
- Provider exceptions can contain connection details; treat captured stderr as
  potentially sensitive even when credentials came from a protected file.
- DTD validation does not verify includes, property completeness, JDBC
  classpaths, SQL syntax, permissions, transactionality, or idempotency.
- File-producing drivers and non-transactional systems cannot provide the same
  rollback guarantee as a transactional JDBC connection.
