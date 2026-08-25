# Database driver compatibility

Scriptella uses standard JDBC and does not bundle vendor JDBC drivers. Download
the appropriate driver from its vendor and make its JAR available through the
connection `classpath` or the Scriptella runtime classpath.

## Current validation targets

This is the initial database modernization baseline selected on 2026-08-25.
The versions below are targets for compatibility testing on JDK 17; they are
not yet certified or an unconditional recommendation for every deployment.
A target becomes a tested version only after the applicable certification
matrix passes against a packaged Scriptella release candidate and the result
is recorded.

| Database | Scriptella alias | Preferred JDBC class | Canonical URL prefix | Validation target | Status |
|---|---|---|---|---|---|
| PostgreSQL | `postgresql` | `org.postgresql.Driver` | `jdbc:postgresql:` | pgJDBC `42.7.13` | Pending certification |
| MariaDB | `mariadb` | `org.mariadb.jdbc.Driver` | `jdbc:mariadb:` | MariaDB Connector/J `3.5.7` | Pending certification |
| MySQL | `mysql` | `com.mysql.cj.jdbc.Driver` | `jdbc:mysql:` | MySQL Connector/J `26.7.0` | Provisional; no MySQL server in the initial matrix |
| Oracle Database | `oracle` | `oracle.jdbc.OracleDriver` | `jdbc:oracle:` | Not yet selected | Limited compatibility target |
| Microsoft SQL Server | `mssql` | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `jdbc:sqlserver:` | Not yet selected | Limited compatibility target |

Scriptella 1.5 provides first-class aliases and URL autodetection for the
current driver classes and canonical URLs above. Obsolete or deprecated driver
classes and URL schemes are not retained as adapter fallbacks. Other JDBC
drivers may still be usable by specifying their fully qualified driver class,
but they are outside the supported and certified baseline.

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

These coordinates identify external runtime drivers for users and validation;
they are not dependencies of the Scriptella product build or distribution.

## Certification record

For each version promoted from a validation target to a tested version, the
certification record must include:

- the packaged Scriptella version and source revision;
- the JDK version;
- the JDBC driver version and checksum;
- the database server version;
- the test date and applicable matrix routes; and
- the result, including any documented limitations.

“Certified” in this documentation means that Scriptella's practical
compatibility suite passed for the recorded versions. It is not an industry or
regulatory certification or an unbounded compatibility promise.
