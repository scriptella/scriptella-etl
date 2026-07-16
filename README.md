# Scriptella

Open source ETL (Extract-Transform-Load) and script execution tool written in Java.
Its primary focus is simplicity: use SQL or another scripting language suited to
the data source, with XML providing orchestration rather than a proprietary
transformation language.

## Project status

Maintenance development has resumed. Scriptella is maintained through focused
compatibility and bug-fix releases.

The current source baseline is **Scriptella 1.3 RC1** (Java 8 target,
preserved Maven/Ant packaging, and a plain HTML website). Official final
artifacts have not yet been published.

The latest **published** release remains **1.2** (October 2019).

## Requirements

| Component | Requirement |
|-----------|-------------|
| **Java** | Java **8** runtime and bytecode target (required baseline). JDK 17 compatibility will be evaluated after RC1; if bounded, it may become part of RC2. |
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
  <version>1.2</version>
</dependency>
```

Drivers and tools modules follow the same version. Release-candidate
snapshots may be built locally; final 1.3 coordinates will be published
after the release gate clears. Prefer the binary distribution or the
all-in-one JAR when you need the full set of bundled providers without
assembling modules yourself.

### Build from source

```bash
# Module build and tests (primary path)
mvn clean install

# Optional: Ant packaging (requires Ant 1.10.17 and project-specific doc tools
# for a full distribution build — see docs/releases/1.3/PLAN.md)
ant clean jar
```

## Documentation

* Release history and upgrade notes: [CHANGELOG.md](CHANGELOG.md)
* Maven Central release procedure: [RELEASE-PUBLISHING.md](RELEASE-PUBLISHING.md)
* Website and reference: [https://scriptella.org](https://scriptella.org)
* Reference manual: [https://scriptella.org/reference/](https://scriptella.org/reference/)
* API docs: [https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Developer notes: [GitHub wiki](https://github.com/scriptella/scriptella-etl/wiki)

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
