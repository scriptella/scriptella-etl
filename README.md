# Scriptella

[![GitHub stars](https://img.shields.io/github/stars/scriptella/scriptella-etl?style=flat&logo=github)](https://github.com/scriptella/scriptella-etl/stargazers)
[![CI](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml/badge.svg)](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.scriptella/scriptella-core?logo=apachemaven)](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)
[![GitHub release](https://img.shields.io/github/v/release/scriptella/scriptella-etl?logo=github)](https://github.com/scriptella/scriptella-etl/releases/latest)

Scriptella is a lightweight, XML-driven ETL and database migration tool for
Java. Use SQL and scripting languages directly to move and transform data
without a proprietary transformation language or graphical designer.

## Why Scriptella?

* **SQL-first:** Work in the language best suited to each data source instead
  of learning a proprietary transformation language.
* **Lightweight:** Run jobs from a single configuration file without a server
  or graphical designer.
* **Automation-friendly:** Keep migrations and data jobs in version control and
  run them locally, in build pipelines, or on a schedule.

## Project status

The latest release is **Scriptella 1.4** (Java 17 baseline), published on
August 14, 2026.

Scriptella 1.4 is available from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.4)
and [Maven Central](https://central.sonatype.com/artifact/org.scriptella/scriptella-core).

See the [changelog](CHANGELOG.md) for the release details and compatibility
changes.

## Requirements

| Version | Java |
| --- | --- |
| **Scriptella 1.4** | Java **17** |
| **Scriptella 1.3 (Java 8 compatibility release)** | Java **8** |

## Getting Scriptella

### Binary distribution

Download a published release from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases)
or [https://scriptella.org/download.html](https://scriptella.org/download.html).

Unzip the binary distribution, change to the extracted directory, and run an
ETL file:

```bash
java -jar scriptella.jar path/to/file.etl.xml
```

Add JDBC drivers and other provider JARs with the connection `classpath`
attribute as needed. See the
[tutorial](https://scriptella.org/tutorial.html) and
[reference](https://scriptella.org/reference/).

### Experimental curl installer

Install Scriptella 1.4 with `curl`:

```bash
curl -fsSL https://scriptella.org/install.sh | sh
```

The installer places Scriptella under `${HOME}/.local/scriptella`. It either
adds `${HOME}/.local/scriptella/bin` to an existing startup file or prints the
exact PATH command to add manually. The guaranteed command is the packaged
`scriptella.sh` launcher:

```bash
scriptella.sh path/to/file.etl.xml
```

The installer does not install Java; Scriptella 1.4 requires Java 17 or newer.
When the installer updates a startup file, start a new shell or reload that
file before using `scriptella.sh`. Manual ZIP installation and
`java -jar scriptella.jar` remain available above.

### Quick start

Create `people.csv` next to `scriptella.jar`:

```csv
id,name
1,Ada
2,Grace
```

Then create `csv-to-sql.etl.xml` in the same directory:

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="input" driver="csv" url="people.csv"/>
    <connection id="output" driver="text" url="load.sql"/>

    <query connection-id="input">
        <script connection-id="output">
            INSERT INTO people (id, name) VALUES ($id, '$name');
        </script>
    </query>
</etl>
```

The flow is source row to nested action:

1. `<query connection-id="input">` asks the CSV connection for rows. The CSV
   driver treats the first line as headers and emits one row for each remaining
   line.
2. The nested `<script connection-id="output">` runs once for every emitted
   row. Its `connection-id` selects the destination; nesting supplies the
   current source row.
3. `$id` and `$name` expand values from that current row. This example writes
   text, so substitution is intentional. When the destination is JDBC, use
   `?id` and `?name` parameters to bind SQL values safely.

Run it from that directory:

```bash
java -jar scriptella.jar csv-to-sql.etl.xml
cat load.sql
```

The generated `load.sql` contains one `INSERT` statement per CSV data row. No
database or JDBC driver is required.

### First migration

The real power comes from nesting a target `<script>` inside a source `<query>`
to transform or copy each row. For a complete MySQL-to-PostgreSQL example that
writes directly to a database with JDBC parameter binding, see
[docs/first-migration.md](docs/first-migration.md).
The [tutorial](https://scriptella.org/tutorial.html) has additional database
and file integration examples.

### Modules and drivers

Scriptella is split into a core module and optional provider modules.
`scriptella-core` contains the ETL engine, service-provider API, and generic
JDBC bridge. It can use any vendor JDBC driver supplied by the application;
the vendor's JAR is not part of Scriptella core.

`scriptella-drivers` contains an opinionated collection of built-in adapters,
including database aliases, CSV, text, Velocity, Spring, mail, and scripting
providers. It depends on `scriptella-core`, but core does not depend on the
drivers module. Third-party drivers and providers can be supplied separately
through a connection's `classpath` attribute.

### Maven coordinates

Published artifacts use group ID `org.scriptella` (from 1.2 onward). Example for
the core module:

```xml
<dependency>
  <groupId>org.scriptella</groupId>
  <artifactId>scriptella-core</artifactId>
  <version>1.4</version>
</dependency>
```

Drivers and tools modules follow the same version. For most Maven-based JDBC
usage, `scriptella-core` is sufficient together with your database's JDBC
driver. Add `scriptella-drivers` when you need Scriptella's built-in aliases or
specialized providers, or use the binary distribution/all-in-one JAR for the
assembled product. Optional provider runtime libraries may still need to be
supplied separately; see the provider documentation.

### Build from source

Requires Java 17+ and Maven 3.6+:

```bash
mvn clean install
```

Release packaging uses Ant 1.10.17 and is validated on JDK 17 and JDK 25;
artifacts remain Java 17-compatible (`--release 17`).

## Documentation

* Website: [https://scriptella.org](https://scriptella.org)
* Reference: [https://scriptella.org/reference/](https://scriptella.org/reference/)
* API docs: [https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Core database compatibility and validation targets: [docs/core-database-compatibility.md](docs/core-database-compatibility.md)
* Command-line usage contract and automation templates: [docs/cli-usage.md](docs/cli-usage.md)
* Release history: [CHANGELOG.md](CHANGELOG.md)
* Maintainer guide: [docs/MAINTAINING.md](docs/MAINTAINING.md)

Packaged documentation may also appear under `docs/` in distribution archives.

## Support and contributions

* **Bugs and features:** [GitHub Issues](https://github.com/scriptella/scriptella-etl/issues)
* **Discussion:** [GitHub Discussions](https://github.com/scriptella/scriptella-etl/discussions)
* **Support:** [scriptella.org/support.html](https://scriptella.org/support.html); commercial inquiries: [scriptella@gmail.com](mailto:scriptella@gmail.com)

Pull requests and well-scoped issue reports for compatibility, correctness, and
maintenance work are welcome. Broad feature development is not currently the
project focus.

## Licensing

This software is licensed under the terms in the file named `LICENSE` in this
directory (Apache License, Version 2.0).

Thank you for using Scriptella.

The Scriptella Project Team  
[https://scriptella.org](https://scriptella.org)
