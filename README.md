# Scriptella

Scriptella is an open-source, Java-based ETL (Extract-Transform-Load) tool for
moving and transforming data between databases, files, and other systems. Use
SQL or another language suited to each data source, while a small XML file
orchestrates the workflow instead of a proprietary transformation language.

## Project status

The latest release is **Scriptella 1.3** (Java 8 target and preserved
Maven/Ant packaging), published on July 17, 2026.

Scriptella 1.3 is available from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.3)
and Maven Central.

## Requirements

| Component | Requirement |
|-----------|-------------|
| **Java** | Java **8** |
| **Maven** | **3.6+** to build from source |
| **Ant** | **1.10.17** for release packaging only |

## Getting Scriptella

### Binary distribution

Download a published release from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases)
or [https://scriptella.org/download.html](https://scriptella.org/download.html).

The binary ZIP includes `scriptella.jar`. Run an ETL file:

```bash
java -jar scriptella.jar path/to/file.etl.xml
```

Add JDBC drivers and other provider JARs with the connection `classpath`
attribute as needed. See the
[tutorial](https://scriptella.org/tutorial.html) and
[reference](https://scriptella.org/reference/).

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

Run it from that directory:

```bash
java -jar scriptella.jar csv-to-sql.etl.xml
cat load.sql
```

The generated `load.sql` contains one `INSERT` statement per CSV row. The CSV
driver reads the header row, so `$id` and `$name` refer to those columns. No
database or JDBC driver is required.

### First migration

The real power comes from nesting a target `<script>` inside a source `<query>`
to transform or copy each row. For a complete MySQL-to-PostgreSQL example that
writes directly to a database with JDBC parameter binding, see
[docs/first-migration.md](docs/first-migration.md).
The [tutorial](https://scriptella.org/tutorial.html) has additional database
and file integration examples.

### Maven coordinates

Published artifacts use group ID `org.scriptella` (from 1.2 onward). Example for
the core module:

```xml
<dependency>
  <groupId>org.scriptella</groupId>
  <artifactId>scriptella-core</artifactId>
  <version>1.3</version>
</dependency>
```

Drivers and tools modules follow the same version. Prefer the binary
distribution or the all-in-one JAR when you need the full set of bundled
providers without assembling modules yourself.

### Build from source

```bash
# Module build and tests (primary path)
mvn clean install

# Optional: Ant packaging (Ant 1.10.17 for release dist / all-in-one JAR)
ant clean jar
```

## Documentation

* Website: [https://scriptella.org](https://scriptella.org)
* Reference: [https://scriptella.org/reference/](https://scriptella.org/reference/)
* API docs: [https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Release history: [CHANGELOG.md](CHANGELOG.md)
* Release procedure: [docs/releases/RELEASE-RUNBOOK.md](docs/releases/RELEASE-RUNBOOK.md)
* Maven Central publishing: [RELEASE-PUBLISHING.md](RELEASE-PUBLISHING.md)
* Generated website docs (maintainers): [docs/site/README.md](docs/site/README.md)

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
