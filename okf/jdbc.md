---
type: Compatibility reference
title: Scriptella JDBC support and 1.5 validation baseline
description: Scriptella supports JDBC databases broadly, with a smaller documented baseline for explicitly maintained and validated 1.5 combinations.
tags: [scriptella, databases, jdbc, compatibility, validation]
sources:
  - id: compatibility
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/core-database-compatibility.md
    title: Core database compatibility
  - id: changelog
    resource: https://github.com/scriptella/scriptella-etl/blob/master/CHANGELOG.md
    title: Scriptella changelog
---

# JDBC database support

Scriptella is not limited to a fixed database catalog. `scriptella-core`
contains a generic JDBC bridge and can work with JDBC databases broadly when a
compatible vendor JDBC driver and its fully qualified driver class are
supplied.

The optional [`scriptella-drivers` module](providers.md) also provides
convenient aliases for a number of databases, including both commonly used and
more specialized databases. These aliases are conveniences rather than the
boundary of Scriptella's JDBC support.

# Scriptella 1.5 validation baseline

The following table records the database/driver combinations for which the
project currently maintains explicit modern compatibility guidance and
release-validation evidence. It is intentionally qualified evidence, not a
simple supported/unsupported boolean and not an industry certification.

| Database | Scriptella alias | Preferred JDBC class | Canonical JDBC URL prefix | Validation target/evidence | Support/validation status |
| --- | --- | --- | --- | --- | --- |
| PostgreSQL | `postgresql` | `org.postgresql.Driver` | `jdbc:postgresql:` | pgJDBC `42.7.13` / PostgreSQL `17.11`; full packaged-distribution matrix passed | Tested for Scriptella 1.5 |
| MariaDB | `mariadb` | `org.mariadb.jdbc.Driver` | `jdbc:mariadb:` | MariaDB Connector/J `3.5.7` / MariaDB `11.8.8`; full packaged-distribution matrix passed | Tested for Scriptella 1.5 |
| MySQL | `mysql` | `com.mysql.cj.jdbc.Driver` | `jdbc:mysql:` | MySQL Connector/J `26.7.0` / MySQL `8.4.11`; one targeted packaged lane passed connection, parameterized write/read, migration, commit, and rollback checks | Targeted 1.5 lane; not a broad compatibility matrix |
| Oracle Database | `oracle` | `oracle.jdbc.OracleDriver` | `jdbc:oracle:` | No real-server matrix selected | Provisional; adapter/smoke coverage exists |
| Microsoft SQL Server | `mssql` | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `jdbc:sqlserver:` | No real-server matrix selected | Provisional; adapter/smoke coverage exists |

“Tested for Scriptella 1.5” means the recorded practical matrix passed for the
listed server and driver versions. “Targeted 1.5 lane” means one recorded
server/driver contract passed and does not imply a broad MySQL compatibility
matrix. “Provisional” means adapter or smoke coverage exists without a passed
real-server contract.

This table records tested and maintained compatibility evidence. It is not an
exhaustive list of databases Scriptella can use.

# Other JDBC databases

Other JDBC databases, including databases for which Scriptella has existing
built-in adapters, may be used outside this validation matrix. For example,
the `scriptella-drivers` module includes adapters for H2, DB2, CUBRID, Derby,
AS/400, Sybase, and others. A database does not need to appear in the
Scriptella 1.5 validation table to be usable through JDBC. For a database
without a convenient alias, supply its compatible vendor JDBC driver and fully
qualified driver class through the normal [provider model](providers.md).

# External drivers

Vendor JDBC drivers are not bundled by `scriptella-core`. Supply the vendor JAR
through the application runtime classpath or the ETL connection's `classpath`
attribute. The [first-migration example](examples/first-migration.md) shows
this as an external deployment input.
