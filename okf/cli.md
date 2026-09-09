---
type: CLI contract
title: Scriptella command-line interface
description: Scriptella's machine-relevant CLI contract is based on invocation, exit status, and documented safety boundaries.
tags: [scriptella, cli, automation, validation]
sources:
  - id: cli
    resource: https://github.com/scriptella/scriptella-etl/blob/master/docs/cli-usage.md
    title: Scriptella command-line usage contract
  - id: changelog
    resource: https://github.com/scriptella/scriptella-etl/blob/master/CHANGELOG.md
    title: Scriptella changelog
---

# Invocation

The canonical launcher form for automation is:

```sh
java -jar "$SCRIPTELLA_JAR" --quiet --no-jmx "$ETL_FILE"
```

Use an absolute path for the selected JAR and ETL file. Supported launcher
options include `-h`/`--help`, `-d`/`--debug`, `-q`/`--quiet`, `-v`/`--version`,
`--check`, `--no-stat`, `--no-jmx`, and `-t`/`--template`. Prefer the documented
double-dash forms in new automation.

# Exit status

Exit status is authoritative:

| Code | Meaning |
| ---: | --- |
| `0` | ETL execution or static checking succeeded, or help/version/template generation succeeded. |
| `1` | ETL execution, `--check`, or template generation failed. |
| `2` | An ETL input file could not be resolved. |
| `3` | An unrecognized launcher option was supplied. |

When multiple ETL files are supplied, they are processed in argument order;
the final status is `1` if any file fails.

# Static checking and resource resolution

`--check` loads and statically validates the XML/DTD configuration, connection
references, and configured driver class compatibility without creating a
session, opening configured connections, or executing queries/scripts. It may
still read referenced resources such as included properties or XML files. It is
not a sandbox and is not a dry-run guarantee: passing it does not establish
that normal execution will be safe or successful.

Connection URLs, connection `classpath` entries, and `<include href="...">`
resources are generally resolved relative to the directory containing the ETL
file.

Normal Scriptella execution can open connections and have side effects. Use
`--check` only for static validation, not as permission isolation for untrusted
ETL files.

# Output boundary

stdout/stderr are not a stable structured protocol. Help and version use
stdout; logs, warnings, failures, and normally progress/statistics use stderr,
while drivers or child processes may write to either stream. Launcher
statistics and log text are human-readable and must not be treated as a JSON,
event, or other machine API. Use the exit status as the machine contract.

See the [security guidance](security.md).
