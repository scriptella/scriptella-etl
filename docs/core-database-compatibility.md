# Core database compatibility

Scriptella supports a broad spectrum of databases through JDBC and other
providers. This page documents the core relational databases used for recurring
compatibility and certification validation; it is not an exhaustive list of
every database Scriptella can connect to.

Scriptella does not bundle vendor JDBC drivers. Download the appropriate driver
from its vendor and make its JAR available through the connection `classpath`
or the Scriptella runtime classpath.

## Core database validation targets

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
            classpath="lib/mssql-jdbc-VERSION.jre11.jar"/>
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
