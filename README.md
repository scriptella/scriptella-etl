# Scriptella

Open source ETL (Extract-Transform-Load) and script execution tool written in Java.
Its primary focus is simplicity: use SQL or another scripting language suited to
the data source, with XML providing orchestration rather than a proprietary
transformation language.

## Project status

Maintenance development has resumed. Scriptella is maintained through focused
compatibility and bug-fix releases.

The latest release is **Scriptella 1.3** (Java 8 target and preserved
Maven/Ant packaging), published on July 17, 2026.

Scriptella 1.3 is available from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.3)
and Maven Central.

## Requirements

| Component | Requirement |
|-----------|-------------|
| **Java** | Java **8** runtime and bytecode target (required baseline). JDK 17 compatibility is deferred to Scriptella 1.4. |
| **Maven** | **3.6+** to build from source (tested with 3.9.x). Maven is the primary module build and test path. |
| **Ant** | **1.10.17** is the documented environment for release packaging (`ant dist`, all-in-one JAR, distribution ZIPs). Other Ant versions may work but are not promised. |

Scriptella is pure Java and should run on any platform with a compatible JDK.

## Getting Scriptella

### Binary distribution

Download a published release from [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases)
or [https://scriptella.org/download.html](https://scriptella.org/download.html).

The binary ZIP includes `scriptella.jar`. Run an ETL file:

```bash
java -jar scriptella.jar path/to/file.etl.xml
```

Add JDBC drivers and other provider JARs to the classpath as needed (for example
with `-cp` / the `classpath` connection attribute). See the
[tutorial](https://scriptella.org/tutorial.html) and
[reference](https://scriptella.org/reference/).

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

## Website

The public site is **[scriptella.org](https://scriptella.org)**, served from the
sibling repository [`scriptella.github.io`](https://github.com/scriptella/scriptella.github.io).

* Maintained pages (tutorial, reference, FAQ, …) are edited directly in that repo.
* Generated **API** and **DTD** docs are produced here (`build-docs.xml`) and
  published with [`docs/site/sync_generated_docs.py`](docs/site/sync_generated_docs.py).
  See [`docs/site/README.md`](docs/site/README.md).

## Documentation

* Website: [https://scriptella.org](https://scriptella.org)
* Reference: [https://scriptella.org/reference/](https://scriptella.org/reference/)
* API docs: [https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Release history: [CHANGELOG.md](CHANGELOG.md)
* Release procedure: [docs/releases/RELEASE-RUNBOOK.md](docs/releases/RELEASE-RUNBOOK.md)
* Maven Central publishing: [RELEASE-PUBLISHING.md](RELEASE-PUBLISHING.md)

Packaged documentation may also appear under `docs/` in distribution archives.

## Support and contributions

* **Bugs and features:** [GitHub Issues](https://github.com/scriptella/scriptella-etl/issues)
* **Discussion:** [GitHub Discussions](https://github.com/scriptella/scriptella-etl/discussions)
* **Website support page:** [https://scriptella.org/support.html](https://scriptella.org/support.html)
* **Commercial inquiries:** [scriptella@gmail.com](mailto:scriptella@gmail.com)

Pull requests and well-scoped issue reports for compatibility, correctness, and
maintenance work are welcome. Broad feature development is not currently the
project focus.

## Licensing

This software is licensed under the terms in the file named `LICENSE` in this
directory (Apache License, Version 2.0).

Thank you for using Scriptella.

The Scriptella Project Team  
[https://scriptella.org](https://scriptella.org)
