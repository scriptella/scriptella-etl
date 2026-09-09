---
type: Example
title: Sanitized MySQL-to-PostgreSQL migration pattern
description: A row-by-row Scriptella migration pattern using an outer source query and a nested parameterized target script.
tags: [scriptella, etl, migration, mysql, postgresql, jdbc]
sources:
  - id: migration
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/first-migration.md
    title: First migration documentation
  - id: compatibility
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/core-database-compatibility.md
    title: Core database compatibility
---

# Pattern

This sanitized example copies `id` and `email` from a MySQL source table to a
PostgreSQL target table. Values marked `${...}` are deployment-supplied
properties; they are placeholders only and contain no real credentials,
hostnames, or filesystem paths.

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <properties>
        <include href="${properties.file}"/>
    </properties>

    <connection id="source"
                driver="mysql"
                url="jdbc:mysql://${source.host}:3306/${source.database}"
                user="${source.user}"
                password="${source.password}"
                classpath="${mysql.jdbc.jar}"/>
    <connection id="target"
                driver="postgresql"
                url="jdbc:postgresql://${target.host}:5432/${target.database}"
                user="${target.user}"
                password="${target.password}"
                classpath="${postgresql.jdbc.jar}"/>

    <query connection-id="source">
        SELECT id, email FROM customers
        <script connection-id="target">
            INSERT INTO customers (id, email) VALUES (?id, ?email)
        </script>
    </query>
</etl>
```

The outer query runs on the `source` connection and produces one row at a time.
The nested script runs against `target` once for each row. The query's `id` and
`email` values flow into the script through the current-row context, and
`?id` / `?email` bind them as JDBC prepared-statement parameters.

The MySQL and PostgreSQL JDBC driver JARs are external deployment inputs, not
part of `scriptella-core`. Set `mysql.jdbc.jar` to the downloaded MySQL
Connector/J JAR and `postgresql.jdbc.jar` to the downloaded pgJDBC JAR; the
placeholder classpath values above represent those application-supplied JARs.
The aliases and preferred JDBC classes are documented in the
[JDBC support and compatibility reference](../jdbc.md).

This demonstrates Scriptella's existing XML + SQL model. It does not infer
source/target semantics from the connection declarations, analyze the SQL, or
introduce a second migration DSL. See the [ETL model](../etl-model.md) and
[provider model](../providers.md), plus the [security guidance](../security.md).
