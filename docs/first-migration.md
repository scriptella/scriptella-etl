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
directory next to `scriptella.jar`, named `mysql-connector-j.jar` and
`postgresql.jar`:

```bash
mkdir -p lib
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
                classpath="lib/mysql-connector-j.jar"/>
    <connection id="target"
                driver="postgresql"
                url="jdbc:postgresql://localhost:5432/target_db"
                user="target_user"
                password="target_password"
                classpath="lib/postgresql.jar"/>

    <query connection-id="source">
        SELECT id, email FROM customers
        <script connection-id="target">
            INSERT INTO customers (id, email) VALUES (?id, ?email)
        </script>
    </query>
</etl>
```

`driver="mysql"` and `driver="postgresql"` select Scriptella's built-in
database adapters. Each adapter loads its JDBC driver from the connection's
`classpath` attribute.

## Run and verify

```bash
java -jar scriptella.jar mysql-to-postgres.etl.xml
psql -d target_db -c 'SELECT id, email FROM customers;'
```

The nested `script` runs once for every selected MySQL row. `?id` and `?email`
refer to the current row's column values. The PostgreSQL query should show the
copied rows.

For production work, keep credentials outside the ETL file and make reruns
safe—for example, by using a target-side upsert or clearing a dedicated staging
table before loading.
