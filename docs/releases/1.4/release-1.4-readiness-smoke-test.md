# Scriptella 1.4 Readiness Smoke Test

**Status:** Draft operator procedure

## Purpose

Build the Scriptella 1.4 distributions from the selected source commit and
verify the binary ZIP as a user receives it. The smoke ETL proves that the
unpacked distribution:

* starts on Java 17 or a tested newer JDK;
* reports its version and displays command-line help;
* discovers the bundled Rhino 1.9.1 provider;
* executes JavaScript through the supported `java -jar` command;
* reads a CSV file with headers; and
* exposes each CSV row to a nested JavaScript script;
* creates and queries a one-row in-memory H2 database; and
* runs the packaged primes example with H2, JEXL, Velocity, and CSV output.

This procedure does not tag, sign, upload, publish, or deploy anything. For a
real release, also follow [`../RELEASE-RUNBOOK.md`](../RELEASE-RUNBOOK.md) and
the private maintainer go/no-go gates.

## Prerequisites

Use:

* the exact source commit being evaluated;
* a clean source checkout, or record all intentional local differences;
* JDK 17 for the release build;
* Maven 3.6 or newer through `mvn-lite`;
* Ant 1.10.17; and
* DTDDoc 1.1.0 for the full distribution build.

Run from the Scriptella workspace root and adjust paths only when the local
workspace layout differs:

```bash
export SCRIPTELLA_SOURCE="$PWD/scriptella-etl"
export SCRIPTELLA_DTDDOC="$PWD/DTDDoc"
mkdir -p "$PWD/release-dir"
export SCRIPTELLA_STAGE
SCRIPTELLA_STAGE=$(mktemp -d "$PWD/release-dir/1.4-readiness-smoke.XXXXXX")
```

Keep `release-dir/` outside every product repository. Do not commit smoke-test
inputs, unpacked archives, logs, or generated evidence.

## 1. Record the test identity

```bash
git -C "$SCRIPTELLA_SOURCE" status --short --branch
git -C "$SCRIPTELLA_SOURCE" rev-parse HEAD
java -version
mvn-lite --version
ant -version
```

Record the source commit, operating system, architecture, JDK vendor and exact
version, Maven version, Ant version, and DTDDoc path. Stop if an unexpected
source modification could affect the artifacts.

## 2. Build and test

Run the Maven reactor gate, then build the binary, source, and examples ZIPs:

```bash
cd "$SCRIPTELLA_SOURCE"
mvn-lite clean verify
ant clean test
ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" clean dist
```

Run the repository's artifact-level distribution contract. This checks the
assembled Rhino JARs and metadata, JEXL, JavaScript aliases, launchers, nested
JavaScript execution, Velocity dependencies, and a packaged example:

```bash
ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" test-distribution
```

All commands must complete successfully. A warning, skipped requirement, or
test failure must be understood before proceeding.

## 3. Inspect and unpack the binary ZIP

The development build uses `1.4-SNAPSHOT`. Substitute the exact candidate
version when testing an RC or final release:

```bash
export SCRIPTELLA_VERSION=1.4-SNAPSHOT
export SCRIPTELLA_BINARY_ZIP="$SCRIPTELLA_SOURCE/build/scriptella-$SCRIPTELLA_VERSION.zip"
export SCRIPTELLA_EXAMPLES_ZIP="$SCRIPTELLA_SOURCE/build/scriptella-examples-$SCRIPTELLA_VERSION.zip"
unzip -t "$SCRIPTELLA_BINARY_ZIP"
unzip -t "$SCRIPTELLA_EXAMPLES_ZIP"
unzip -q "$SCRIPTELLA_BINARY_ZIP" -d "$SCRIPTELLA_STAGE"
mkdir "$SCRIPTELLA_STAGE/examples"
unzip -q "$SCRIPTELLA_EXAMPLES_ZIP" -d "$SCRIPTELLA_STAGE/examples"
export SCRIPTELLA_DIST="$SCRIPTELLA_STAGE/scriptella-$SCRIPTELLA_VERSION"
export SCRIPTELLA_EXAMPLES="$SCRIPTELLA_STAGE/examples"
test -f "$SCRIPTELLA_DIST/scriptella.jar"
test -f "$SCRIPTELLA_DIST/lib/rhino-engine.jar"
test -f "$SCRIPTELLA_DIST/lib/rhino.jar"
```

Optionally record the archive checksum with:

```bash
shasum -a 256 "$SCRIPTELLA_BINARY_ZIP"
shasum -a 256 "$SCRIPTELLA_EXAMPLES_ZIP"
```

## 4. Check command-line startup

Run the version and help paths from the clean unpack:

```bash
cd "$SCRIPTELLA_DIST"
java -jar scriptella.jar --version
java -jar scriptella.jar --help
```

Confirm that both commands exit successfully, identify Scriptella, and do not
report class-loading or missing-dependency errors.

## 5. Create the JavaScript and CSV smoke input

Create `$SCRIPTELLA_DIST/people.csv`:

```csv
id,name
1,Ada
2,Grace
```

Create `$SCRIPTELLA_DIST/readiness-smoke.etl.xml`:

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="javascript" driver="script">language=js</connection>
    <connection id="people" driver="csv" url="people.csv"/>

    <script connection-id="javascript"><![CDATA[
        java.lang.System.out.println("HELLO_WORLD");
    ]]></script>

    <query connection-id="people">
        <script connection-id="javascript"><![CDATA[
            java.lang.System.out.println("CSV_ROW_" + id + "=" + name);
        ]]></script>
    </query>
</etl>
```

The ETL intentionally uses `language=js` to exercise Scriptella's JavaScript
alias fallback and the Rhino provider bundled under `lib/`.

## 6. Run JavaScript and CSV from the unpacked distribution

Run the documented standalone command from the distribution directory so the
CSV URL resolves relative to the ETL file:

```bash
cd "$SCRIPTELLA_DIST"
java -jar scriptella.jar readiness-smoke.etl.xml \
  >"$SCRIPTELLA_STAGE/readiness-smoke.out" 2>&1
cat "$SCRIPTELLA_STAGE/readiness-smoke.out"
```

The output may contain normal Scriptella logging, but it must contain all of:

```text
HELLO_WORLD
CSV_ROW_1=Ada
CSV_ROW_2=Grace
```

Check those assertions directly:

```bash
grep -F 'HELLO_WORLD' "$SCRIPTELLA_STAGE/readiness-smoke.out"
grep -F 'CSV_ROW_1=Ada' "$SCRIPTELLA_STAGE/readiness-smoke.out"
grep -F 'CSV_ROW_2=Grace' "$SCRIPTELLA_STAGE/readiness-smoke.out"
```

Repeat through the Unix distribution launcher:

```bash
"$SCRIPTELLA_DIST/bin/scriptella.sh" \
  "$SCRIPTELLA_DIST/readiness-smoke.etl.xml" \
  >"$SCRIPTELLA_STAGE/readiness-launcher-smoke.out" 2>&1
grep -F 'HELLO_WORLD' "$SCRIPTELLA_STAGE/readiness-launcher-smoke.out"
grep -F 'CSV_ROW_1=Ada' "$SCRIPTELLA_STAGE/readiness-launcher-smoke.out"
grep -F 'CSV_ROW_2=Grace' "$SCRIPTELLA_STAGE/readiness-launcher-smoke.out"
```

Run `bin\scriptella.bat` with the same ETL on native Windows when Windows is
part of the candidate's supported validation matrix. Static inspection of the
batch file is not a substitute for that platform check.

## 7. Run an in-memory H2 smoke ETL

Create a scratch directory inside the unpacked examples tree so the ETL can
use its bundled H2 JDBC JAR:

```bash
mkdir "$SCRIPTELLA_EXAMPLES/h2-readiness-smoke"
```

Create `$SCRIPTELLA_EXAMPLES/h2-readiness-smoke/h2-smoke.etl.xml`:

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="database"
                driver="h2"
                url="jdbc:h2:mem:readiness;DB_CLOSE_DELAY=-1"
                user="sa"
                classpath="../lib/h2.jar"/>
    <connection id="console" driver="text"/>

    <script connection-id="database">
        CREATE TABLE smoke_result (
            id INTEGER PRIMARY KEY,
            message VARCHAR(40) NOT NULL
        );
        INSERT INTO smoke_result (id, message) VALUES (1, 'hello from h2');
    </script>

    <query connection-id="database">
        SELECT id, message FROM smoke_result
        <script connection-id="console">
            H2_ROW_${id}=${message}
        </script>
    </query>
</etl>
```

Run it from the scratch directory:

```bash
cd "$SCRIPTELLA_EXAMPLES/h2-readiness-smoke"
java -jar ../lib/scriptella.jar h2-smoke.etl.xml \
  >"$SCRIPTELLA_STAGE/h2-smoke.out" 2>&1
grep -F 'H2_ROW_1=hello from h2' "$SCRIPTELLA_STAGE/h2-smoke.out"
```

This verifies H2 driver loading from the unpacked examples distribution,
in-memory DDL and DML, JDBC querying, row-variable expansion, and console
output through the text driver.

## 8. Run the packaged primes example

Run the example exactly from the unpacked examples archive:

```bash
cd "$SCRIPTELLA_EXAMPLES/primes"
java -jar ../lib/scriptella.jar etl.xml \
  >"$SCRIPTELLA_STAGE/primes-smoke.out" 2>&1
test -s report.html
test -s report.csv
```

This single example is a compact smoke check for the 1.4 H2 replacement, JEXL,
the split Velocity 1.7 runtime dependencies, and CSV output. Inspect the first
few generated rows and confirm that the log has no errors:

```bash
head report.csv
```

Do not expand this smoke procedure into dedicated Janino, Mail, Spring, ODBC,
or HSQLDB scenarios. Their focused and negative coverage belongs in the normal
automated suites; Mail and Spring also require environments that are unsuitable
for a portable distribution smoke test.

## 9. Readiness result

Treat this smoke test as passed only when:

* Maven verification, Ant tests, `dist`, and `test-distribution` pass;
* every generated ZIP passes `unzip -t`;
* the binary ZIP contains `scriptella.jar` and both Rhino JARs under `lib/`;
* version and help commands start without dependency errors;
* `java -jar` prints the greeting and both CSV rows from the clean unpack;
* the Unix launcher produces the same result;
* the in-memory H2 ETL prints its single expected row through the text driver;
* the unpacked primes example produces non-empty HTML and CSV reports;
* there are no unexplained errors or warnings; and
* the source commit, environment, commands, archive names, checksums, and
  sanitized outputs are recorded with the candidate evidence.

Any source or packaging fix invalidates the result. Build a fresh distribution
from the corrected commit and repeat the procedure from the beginning.
