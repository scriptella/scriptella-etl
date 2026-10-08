# Compatibility evidence — 2026-10-05

Originally recorded in [scriptella-examples at e2499ab](https://github.com/scriptella/scriptella-examples/tree/e2499ab/sqlite-to-postgresql).
The sample now lives here and reads configuration through `${env.*}` instead
of JVM properties. The SQL fixture and verifier are unchanged.

Validated the example against the Scriptella 1.6 distribution (`scriptella.jar`,
Implementation-Version 1.6). The SQLite path did not exist before the run.
`seed.etl.xml` created it, `migrate.etl.xml` copied the four rows, and psql
ran `verify.sql`.

| Component | Tested version |
|---|---|
| Java | Temurin 17.0.15+6 |
| Scriptella | 1.6 distribution |
| SQLite JDBC | Xerial 3.53.4.0 |
| PostgreSQL | 17.11 |
| PostgreSQL JDBC | 42.7.13 |

Both ETL files ran with `java -jar scriptella.jar` from that distribution,
using the `sqlite` and `postgresql` aliases and external connection-classpath
driver JARs. PostgreSQL was `postgres:17.11-alpine3.24`; `SELECT version()`
reported PostgreSQL 17.11. The destination had no `sqlite_records` table
before migration. psql was the client in that server container, with
`ON_ERROR_STOP=1` and UTF-8 client encoding.

That psql connection ran `verify.sql`:

```text
DO
PASS: exact integers, common numerics, Unicode, NULL and empty text
```

After deliberately changing row 1's label in the disposable destination,
the same verifier rejected the data with psql exit status 3:

```text
ERROR: SQLite migration values differ from the expected fixture
```

This covers the four sample rows, including signed 64-bit extrema and the
integer above 2^53. It is bounded compatibility evidence, not a SQLite version
matrix or a claim of arbitrary-decimal fidelity. The drivers-module SQLite
regression separately covers file-backed schema, reads, writes, persistence,
commit, and rollback, with fresh direct JDBC verification.

The [original #62 investigation](https://gist.github.com/ejboy/e3768dd452cbae4ecf5775e176a0da70)
provides reproducible evidence for NUMERIC/REAL precision loss and a direct-JDBC
control. The support documentation explains this limitation. SQLite is tested
as a real file; it is not added to the Testcontainers matrix.
