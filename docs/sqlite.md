# SQLite support in Scriptella 1.6

Use `driver="sqlite"` with Xerial SQLite JDBC `3.53.4.0` (`org.sqlite.JDBC`):

```xml
<connection id="source" driver="sqlite"
            url="jdbc:sqlite:/absolute/path/source.db"
            classpath="lib/sqlite-jdbc-3.53.4.0.jar"/>
```

Canonical `jdbc:sqlite:` URLs also select SQLite when the driver is omitted
or set to `auto`. The alias is in `scriptella-drivers`; embedded applications
need that module as well as `scriptella-core`. Supply the external Xerial JAR
through the connection `classpath` (relative to the ETL file) or application
runtime classpath. It is a pinned test dependency, not a bundled runtime driver.
Use an existing parent directory and an absolute database path to avoid
working-directory ambiguity. SQLite can create a new file for a missing path;
check source paths before migration to avoid reading an unintended empty file.

The initial support scope is file-backed SQLite for ordinary migration
workloads: schema creation, reads/writes, persistence, signed 64-bit integers,
common numeric values, Unicode/text, NULL versus empty text, successful commit,
and rollback after failure. The adapter uses the existing generic JDBC
implementation; no SQLite-specific transaction architecture is introduced.

The [canonical SQLite → PostgreSQL sample](https://github.com/scriptella/scriptella-examples/tree/master/sqlite-to-postgresql)
seeds the SQLite file with Scriptella and checks the destination with native psql.
The verified boundary is PostgreSQL 17.11 with pgJDBC 42.7.13; it does not imply
verification of other server destinations.

## Numeric precision

SQLite's dynamic numeric representation can lose precision for high-precision
decimals. Xerial may expose NUMERIC/REAL as `Double`; generic JDBC transfer to
PostgreSQL NUMERIC can round further. In the investigation, the literal
`1234567890.123456789` became SQLite REAL, read as `1234567890.1234567`, and
arrived as PostgreSQL NUMERIC `1234567890.12346`. Direct JDBC reproduced this
without Scriptella. Do not assume lossless migration of arbitrary SQLite
numeric values.

Explicit conversion is required when exact decimal fidelity matters. Preserve
exact source decimals as TEXT and explicitly convert to BigDecimal or a target
NUMERIC value; this sample does not implement a decimal adapter. Conversion
cannot recover precision already lost in source REAL storage.

## Public compatibility evidence

The small `SQLiteTest` in the drivers module runs against an actual temporary
SQLite file using the pinned Xerial driver. It exercises alias resolution and
auto selection, schema creation, query-to-parameter-bound writes, persistence,
representative values, commit, and deliberate primary-key failure with rollback.
Fresh direct JDBC connections verify stored values and transaction results.
Run it with Java 17 selected:

```sh
mvn-lite -pl drivers -am test -Dtest=SQLiteTest,AutoDriverTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```

The [reproducible #62 investigation](https://gist.github.com/ejboy/e3768dd452cbae4ecf5775e176a0da70)
also verifies SQLite independently with Python sqlite3, the PostgreSQL boundary
with direct pgJDBC and psql, and a direct-JDBC decimal control.
SQLite needs no provisioning container and is not part of the Testcontainers
matrix. No version matrix or broad SQLite suite is implied.

WAL, concurrent writers/locking, STRICT mode, foreign-key behavior, date/time
and boolean mappings, binary values, reverse migrations, and special decimal
adapters remain outside this verified contract.

Public website/generated documentation publication for 1.6 is tracked by
[#64](https://github.com/scriptella/scriptella-etl/issues/64); this source
prepares the documentation without making a release announcement.
