---
okf_version: "0.2"
---

# Scriptella knowledge

This bundle contains curated public knowledge for coding agents and other
knowledge tools that work with Scriptella's existing XML + SQL/script model.

# Core concepts

* [Scriptella project](project.md) - Product scope, philosophy, Java baseline, and module roles.
* [Scriptella ETL model](etl-model.md) - Connections, queries, scripts, row context, and value binding.
* [Scriptella providers and connections](providers.md) - Provider-backed endpoints, representative integration families, lookup, and extension points.
* [Scriptella JDBC support and 1.5 validation baseline](jdbc.md) - General JDBC access plus qualified validation evidence for maintained combinations.
* [Scriptella command-line interface](cli.md) - Invocation, options, exit statuses, static checking, and output boundaries.
* [Scriptella knowledge and execution security](security.md) - Credential handling, redaction, and publication boundaries.

# Example

* [Sanitized MySQL-to-PostgreSQL migration pattern](examples/first-migration.md) - An outer source query with a nested parameterized target script.
