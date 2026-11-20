# Core database compatibility

Scriptella supports a broad spectrum of databases through JDBC and other
providers. This page helps users configure Scriptella with common relational
databases and understand their compatibility, including the preferred driver,
driver class, URL, validation status, and known limitations. It is not an
exhaustive list of every database Scriptella can connect to.

Scriptella does not bundle vendor JDBC drivers. Download the appropriate driver
from its vendor and make its JAR available through the connection `classpath`
or the Scriptella runtime classpath.

## Core database validation targets

The table records the configurations verified for Scriptella 1.7.0. Each
result applies to the listed versions and test scope.

| Database | Scriptella alias | Preferred JDBC class | Canonical URL prefix | Tested server and JDBC driver | Validation scope |
|---|---|---|---|---|---|
| PostgreSQL | `postgresql` | `org.postgresql.Driver` | `jdbc:postgresql:` | PostgreSQL `17.11` / pgJDBC `42.7.13` | Packaged JDBC contract and focused public Testcontainers checks |
| MariaDB | `mariadb` | `org.mariadb.jdbc.Driver` | `jdbc:mariadb:` | MariaDB `11.8.8` / Connector/J `3.5.7` | Packaged JDBC contract and focused public Testcontainers checks |
| MySQL | `mysql` | `com.mysql.cj.jdbc.Driver` | `jdbc:mysql:` | MySQL `8.4.11` / Connector/J `26.7.0` | Targeted packaged JDBC contract for one server and driver pair |
| Oracle Database | `oracle` | `oracle.jdbc.OracleDriver` | `jdbc:oracle:` | Oracle Free `23.26.2` / ojdbc17 `23.26.3.0.0` | Focused public Testcontainers checks |
| Microsoft SQL Server | `mssql` | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `jdbc:sqlserver:` | SQL Server `2022-CU26` on Linux / mssql-jdbc `13.6.0.jre11` | Focused public Testcontainers checks |
| SQLite | `sqlite` | `org.sqlite.JDBC` | `jdbc:sqlite:` | SQLite `3.53.4` bundled with Xerial SQLite JDBC `3.53.4.0` | Separate file-backed test |

The [Scriptella 1.7.0 binary ZIP](https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.7.0)
passed the packaged checks on 2026-11-03 with OpenJDK 25.0.4
(run `2026-11-03T172315Z`, ZIP SHA-256
`1c8ec413bd4814680afde98081b61222f1158a48acd4a633af4a3eabfc3a3263`).
For PostgreSQL, MariaDB, and the targeted MySQL pair, the run checked
connection, alias and URL autodetection, parameterized writes and reads,
migration in both directions with H2 2.4.240, and commit and rollback
verified through fresh connections.

When upgrading to Scriptella 1.5, use current JDBC driver generations. The
legacy MySQL and Oracle driver class names and the SQL Server 2000, jTDS, and
`jdbc:microsoft:` routes are no longer part of the first-class database
adapter baseline.

## Obtaining drivers

| Database | Vendor distribution | Maven coordinate or JAR pattern |
|---|---|---|
| PostgreSQL | [pgJDBC downloads](https://jdbc.postgresql.org/download/) | `org.postgresql:postgresql:VERSION`; `postgresql-VERSION.jar` |
| MariaDB | [MariaDB Connector/J](https://mariadb.com/docs/connectors/mariadb-connector-j/) | `org.mariadb.jdbc:mariadb-java-client:VERSION`; `mariadb-java-client-VERSION.jar` |
| MySQL | [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) | `com.mysql:mysql-connector-j:VERSION`; `mysql-connector-j-VERSION.jar` |
| Oracle Database | [Oracle JDBC downloads](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html) | `com.oracle.database.jdbc:ojdbc17:VERSION`; `ojdbc17.jar` for JDK 17 and later |
| Microsoft SQL Server | [Microsoft JDBC Driver downloads](https://learn.microsoft.com/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server) | `com.microsoft.sqlserver:mssql-jdbc:VERSION.jre11`; `mssql-jdbc-VERSION.jre11.jar` for JDK 17 |
| SQLite | [Xerial SQLite JDBC](https://github.com/xerial/sqlite-jdbc/releases/tag/3.53.4.0) | `org.xerial:sqlite-jdbc:3.53.4.0`; `sqlite-jdbc-3.53.4.0.jar` |

These coordinates identify external runtime drivers for users and validation;
they are not dependencies of the Scriptella product build or distribution.

## Connection classpath examples

The following examples assume that the ETL file is next to a `lib/` directory.
Connection `classpath` values are resolved relative to the ETL file. Replace
the placeholder credentials with values from a protected properties or secret
store; do not commit passwords to an ETL file.

PostgreSQL:

```xml
<connection id="postgresql" driver="postgresql"
            url="jdbc:postgresql://db.example.com:5432/app"
            user="app_user" password="${db.password}"
            classpath="lib/postgresql-42.7.13.jar"/>
```

MariaDB:

```xml
<connection id="mariadb" driver="mariadb"
            url="jdbc:mariadb://db.example.com:3306/app"
            user="app_user" password="${db.password}"
            classpath="lib/mariadb-java-client-3.5.7.jar"/>
```

MySQL:

```xml
<connection id="mysql" driver="mysql"
            url="jdbc:mysql://db.example.com:3306/app"
            user="app_user" password="${db.password}"
            classpath="lib/mysql-connector-j-26.7.0.jar"/>
```

Oracle Database:

```xml
<connection id="oracle" driver="oracle"
            url="jdbc:oracle:thin:@//db.example.com:1521/app"
            user="app_user" password="${db.password}"
            classpath="lib/ojdbc17.jar"/>
```

Microsoft SQL Server:

```xml
<connection id="mssql" driver="mssql"
            url="jdbc:sqlserver://db.example.com:1433;databaseName=app;encrypt=true;trustServerCertificate=false"
            user="app_user" password="${db.password}"
            classpath="lib/mssql-jdbc-13.6.0.jre11.jar"/>
```

Current Microsoft JDBC drivers use TLS encryption by default and validate the
server certificate by default. The SQL Server example states both settings
explicitly. Use a server name that matches the certificate and provide trust
material through the JVM trust store or the driver's `trustStore` properties.
Setting `trustServerCertificate=true` disables certificate validation and is
appropriate only for a controlled local test with a self-signed certificate,
not for production.

See the [Microsoft JDBC encryption guidance](https://learn.microsoft.com/sql/connect/jdbc/connecting-with-ssl-encryption)
for the driver-specific trust-store options.

## Compatibility validation

### Public real-database compatibility tests

The public [Scriptella Testcontainers suite](https://github.com/scriptella/scriptella-testcontainers)
runs focused ETL compatibility tests against real PostgreSQL, MariaDB, Oracle
Free, and Microsoft SQL Server databases using pinned container images and JDBC
driver versions. The tests exercise representative reads, writes, values, and
transaction rollback.

Recorded results are available in the suite's
[compatibility history](https://github.com/scriptella/scriptella-testcontainers/blob/main/docs/compatibility-results.md).

All four jobs passed with published Scriptella 1.7.0 in
[run 41696161604](https://github.com/scriptella/scriptella-testcontainers/actions/runs/41696161604).
Oracle Free and SQL Server have narrower coverage than PostgreSQL and MariaDB.
Their results cover the listed container and driver pairs and the focused
copy and rollback contract.

For support or to request compatibility validation for another database,
contact PVR Labs, the maintainer of Scriptella, through the
[Scriptella support page](https://scriptella.org/support.html).

## SQLite

The `sqlite` alias and canonical `jdbc:sqlite:` autodetection use external Xerial
SQLite JDBC 3.53.4.0. See [SQLite support](sqlite.md) for the bounded file-backed
contract, public evidence, SQLite → PostgreSQL sample, and NUMERIC/REAL decimal
precision limitation. The file-backed SQLite test passed in the
[1.7.0 release-candidate CI](https://github.com/scriptella/scriptella-etl/actions/runs/41683147031)
on Java 17 and 25, separately from Testcontainers. The test verifies schema
creation, parameterized reads and writes, representative values, persistence,
commit, and rollback.
