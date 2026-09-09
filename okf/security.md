---
type: Security guidance
title: Scriptella knowledge and execution security
description: Scriptella jobs and curated knowledge must keep credentials, infrastructure details, and runtime data out of source control and published context.
tags: [scriptella, security, credentials, redaction, etl]
sources:
  - id: cli
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/cli-usage.md
    title: Scriptella command-line usage contract
  - id: migration
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/first-migration.md
    title: First migration documentation
  - id: compatibility
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/core-database-compatibility.md
    title: Core database compatibility
---

# Credentials and diagnostics

Do not commit credentials in ETL files. Do not expose secrets in process
arguments, logs, generated SQL, issue reports, or OKF content. Prefer a
protected external properties file for credentials and keep that file outside
the repository with access limited to the executing account.

Connection URLs and provider properties can contain sensitive infrastructure or
secret information. Provider exceptions and error output may include connection
details, so captured stderr and diagnostics must be treated as potentially
sensitive even when credentials came from an external file.

# Static checking is not isolation

`--check` may read referenced resources while loading configuration. It does
not execute the ETL, but it is not a sandbox and is not a safe-execution mode
for untrusted ETL files.

# Publishing OKF

Publishing this bundle must not automatically include production ETL files,
SQL or scripts, row-level data, local filesystem paths, real environment JDBC
URLs, arbitrary connection properties, credentials, tokens, or stack traces.
The OKF layer is curated public project knowledge; it is not an ETL exporter,
runtime-data publication mechanism, or production configuration mirror.
