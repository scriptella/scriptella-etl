# First migration: MySQL to PostgreSQL

This example copies `id` and `email` from a MySQL `customers` table to the
corresponding PostgreSQL table. It is intentionally small, but uses the same
row-by-row mapping pattern as a larger migration.

## Before you start

You need access to both databases, with these tables already created:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);
```

Place [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) and the
[PostgreSQL JDBC driver](https://jdbc.postgresql.org/download/) in a `lib/`
directory next to `scriptella.jar`. The current development targets are
MySQL Connector/J `26.7.0` and pgJDBC `42.7.13`; use the filenames that match
the JARs you downloaded, for example:

```bash
mkdir -p lib
# Rename or copy the downloaded files to these paths as appropriate.
# lib/mysql-connector-j-26.7.0.jar
# lib/postgresql-42.7.13.jar
```

## Create the ETL file

Create `mysql-to-postgres.etl.xml` next to `scriptella.jar`. Replace the URLs
and credentials with those for your databases; do not commit real passwords.

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="source"
                driver="mysql"
                url="jdbc:mysql://localhost:3306/source_db"
                user="source_user"
                password="source_password"
                classpath="lib/mysql-connector-j-26.7.0.jar"/>
    <connection id="target"
                driver="postgresql"
                url="jdbc:postgresql://localhost:5432/target_db"
                user="target_user"
                password="target_password"
                classpath="lib/postgresql-42.7.13.jar"/>

    <query connection-id="source">
        SELECT id, email FROM customers
        <script connection-id="target">
            INSERT INTO customers (id, email) VALUES (?id, ?email)
        </script>
    </query>
</etl>
```

`driver="mysql"` and `driver="postgresql"` select Scriptella's built-in
database adapters. The MySQL adapter uses
`com.mysql.cj.jdbc.Driver`, and the PostgreSQL adapter uses
`org.postgresql.Driver`. Each adapter loads its JDBC driver from the
connection's `classpath` attribute. MySQL remains provisional until the
certification matrix includes a MySQL server.

For MariaDB, Oracle Database, and Microsoft SQL Server examples, including
their current driver classes and connection classpaths, see the
[core database compatibility page](core-database-compatibility.md).

The outer `<query>` selects rows from the `source` connection. For each result
row, Scriptella runs the nested `<script>` against the `target` connection and
makes the current row's columns available by name. JDBC placeholders such as
`?id` and `?email` bind those values as prepared-statement parameters; they are
not textual substitutions.

## Run and verify

```bash
java -jar scriptella.jar mysql-to-postgres.etl.xml
psql -d target_db -c 'SELECT id, email FROM customers;'
```

The PostgreSQL query should show the copied rows.

For production work, keep credentials outside the ETL file and make reruns
safe—for example, by using a target-side upsert or clearing a dedicated staging
table before loading.
