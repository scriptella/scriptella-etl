---
type: Provider model
title: Scriptella providers and connections
description: Scriptella connections are provider-backed endpoints spanning databases, files, structured text, scripting runtimes, and external integrations.
tags: [scriptella, connections, drivers, providers, integrations]
sources:
  - id: readme
    resource: https://github.com/scriptella/scriptella-etl/blob/master/README.md
    title: Scriptella README
  - id: driver-factory
    resource: https://github.com/scriptella/scriptella-etl/blob/master/core/src/java/scriptella/core/DriverFactory.java
    title: DriverFactory implementation
  - id: provider-collection
    resource: https://github.com/scriptella/scriptella-etl/blob/master/drivers/src/java/scriptella/driver/package-info.java
    title: Built-in provider package documentation
---

# Connection and provider model

Scriptella is not limited to database-to-database ETL. A `<connection>` is a
provider-backed endpoint through which a `<query>` can produce row context or a
`<script>` can perform an action. A provider may represent a relational
database, file, structured-text source, scripting runtime, directory or
service, external command, or another integration.

The connection abstraction does not require both ends of a flow to use the
same provider family. Natural Scriptella patterns include CSV to JDBC, JDBC to
CSV or text, XML/XPath to JDBC, LDAP to JDBC, and a query feeding a script or
shell action. Whether a connection is source-like or target-like follows from
how queries and scripts use it, as described by the [ETL model](etl-model.md).

# Representative provider families

These families illustrate the in-tree provider surface; they are not an
exhaustive compatibility or support matrix.

| Family | Representative providers and roles |
| --- | --- |
| Relational databases | Generic JDBC bridge plus built-in aliases and adapters. |
| Files and structured text | CSV, text, XLS, and XML through XPath. |
| Scripting and transformations | JSR-223 `script`, JEXL, Janino, and Velocity. |
| External systems and application integration | LDAP, mail, shell commands, JNDI data sources, and Spring-managed data sources. |
| ETL composition | The Scriptella provider can invoke another ETL document. |

Each provider defines the URL and properties it accepts, the query/script
syntax it understands, and which operations it supports. SQL semantics apply
to JDBC connections; they are not imposed on non-JDBC providers.

# Lookup and extension

Scriptella can load a fully qualified driver class name. If direct class lookup
does not find that name, a short name resolves through
`scriptella.driver.<name>.Driver`.

The resolved class may be a JDBC `java.sql.Driver` or a provider implementing
the Scriptella driver SPI. JDBC drivers are adapted through the generic bridge
in `scriptella-core`. Scriptella providers can implement the SPI directly.

`scriptella-drivers` is an optional, representative collection of built-in
adapters and aliases, not the boundary of what Scriptella can connect to.
Applications can supply third-party providers, vendor JDBC drivers, and other
provider dependencies separately through the application classpath or a
connection's `classpath` configuration.

See [JDBC support and the 1.5 validation baseline](jdbc.md) for the narrower
database-specific contract.
