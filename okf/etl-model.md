---
type: Configuration model
title: Scriptella ETL model
description: Scriptella ETL XML composes provider-backed connections, queries, scripts, properties, and related configuration.
tags: [scriptella, etl, xml, query, script, providers]
sources:
  - id: migration
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/first-migration.md
    title: First migration documentation
  - id: readme
    resource: https://github.com/scriptella/scriptella-etl/blob/master/README.md
    title: Scriptella README
  - id: configuration-factory
    resource: https://github.com/scriptella/scriptella-etl/blob/master/core/src/java/scriptella/configuration/ConfigurationFactory.java
    title: ConfigurationFactory implementation
---

# Document structure

An ETL document is XML. It declares one or more connections, queries, scripts,
properties, and related configuration such as includes. The XML configuration
is Scriptella's job model; OKF describes the model but does not replace it with
another DSL.

A `<connection>` is an abstraction over a provider-backed endpoint, not
necessarily a database. Depending on its provider, a connection may represent
a JDBC database, CSV or text file, XML/XPath source, scripting runtime,
directory service, external command, or another integration. The provider
defines the query and script syntax and the capabilities available through the
connection.

A [`<query>`](https://github.com/scriptella/scriptella-etl/blob/master/docs/first-migration.md)
executes against the connection selected by its `connection-id` and produces a
row context. A nested `<script>` executes once for each row produced by its
enclosing query. The nested script can read the current row's values by name.
This nesting is the basic row-by-row source-to-action pattern.

`connection-id` chooses which declared connection an element uses. A
connection is not intrinsically a source or a target: source/target is a role
created by how queries and scripts use that connection. For example, a
connection used by an outer query is source-like, while one used by a nested
write script is target-like. Agents must not classify a connection from its ID,
driver, or URL alone.

# Values and SQL

`$name` is textual/property substitution. It expands a value in the current
property or row context before the receiving connection handles the text.
When data values are sent to JDBC, normally use JDBC named placeholders such as
`?id` or `?email`; those values are bound as prepared-statement parameters
rather than interpolated into SQL text.

SQL and scripts are arbitrary application logic from Scriptella's point of
view. OKF records how they participate in the configuration model, but does
not semantically reinterpret, analyze, or infer business meaning from their
contents. Scriptella's existing SQL and scripting languages remain fully
available.

See the [provider model](providers.md), [JDBC support](jdbc.md), and the
[sanitized first-migration pattern](examples/first-migration.md).
