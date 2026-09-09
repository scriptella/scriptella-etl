---
type: Product
title: Scriptella
description: A lightweight, XML-driven ETL and database migration tool for Java.
resource: https://github.com/scriptella/scriptella-etl
tags: [scriptella, etl, java, sql, database-migration]
sources:
  - id: readme
    resource: https://github.com/scriptella/scriptella-etl/blob/master/README.md
    title: Scriptella README
  - id: changelog
    resource: https://github.com/scriptella/scriptella-etl/blob/master/CHANGELOG.md
    title: Scriptella changelog
---

# Overview

Scriptella is a lightweight, XML-driven ETL and database migration tool for
Java. It is designed for SQL-centric jobs that need more structure than
standalone scripts but do not justify a full ETL or batch platform.

Scriptella orchestrates queries and scripts across heterogeneous,
provider-backed connections. A connection may represent a relational database,
a file or structured-text source, a scripting runtime, or another integration;
it is not necessarily a database.

Scriptella is SQL-first, but not database-only: SQL and scripting languages
remain directly exposed, and Scriptella does not introduce a proprietary
transformation language. An ETL job is kept in a versionable XML configuration
file and can be run locally, in build pipelines, or from an external scheduler.

Scriptella has no required server, scheduler, job repository, or graphical ETL
environment. It is a runtime and provider model, not a hosted job-management
service.

Scriptella 1.5 uses Java 17 as its runtime baseline. Scriptella 1.3 remains the
Java 8 compatibility line.

# Modules

| Module | Stable role |
| --- | --- |
| `scriptella-core` | ETL engine, provider API, and generic JDBC bridge. |
| `scriptella-drivers` | Optional representative collection of built-in adapters and aliases. |

The `scriptella-drivers` module is not an exhaustive list of endpoints or
providers that Scriptella can use. Applications may use `scriptella-core` with
vendor JDBC drivers or Scriptella providers supplied separately. A connection
can also load provider JARs through its `classpath` configuration.

See the [ETL model](etl-model.md), [provider model](providers.md),
[JDBC support and compatibility](jdbc.md), [CLI contract](cli.md), and
[security guidance](security.md).
