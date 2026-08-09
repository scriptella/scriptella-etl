# Scriptella 1.4 Readiness Smoke-Test Results

**Status:** Passed

**Run date:** August 7, 2026 (America/Los_Angeles)

**Procedure:** [release-1.4-readiness-smoke-test.md](release-1.4-readiness-smoke-test.md)

## Candidate identity

The experiment used a clean `scriptella-etl` checkout at commit
`67ae44aba90171899de18680999914bd7a0e2b39` (`master`, tracking
`origin/master`). The candidate build version was `1.4-SNAPSHOT`.

| Item | Result |
| --- | --- |
| Operating system | macOS 15.6.1, x86_64 |
| JDK | Eclipse Adoptium Temurin 17.0.15+6 |
| Maven | Apache Maven 3.9.9, through `mvn-lite` |
| Ant | Apache Ant 1.10.17 |
| DTDDoc | Workspace-local `DTDDoc` checkout |
| Source status | Clean before and after the run |

## Build and package gates

All required gates completed successfully:

* `mvn-lite clean verify`
* `ant clean test`
* `ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" clean dist`
* `ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" test-distribution`
* `unzip -t` for the binary and examples ZIPs

The binary unpack contained `scriptella.jar`, `lib/rhino-engine.jar`, and
`lib/rhino.jar`.

## Runtime checks

The checks were run from the unpacked archives, not from the build tree.

| Check | Result | Observed output |
| --- | --- | --- |
| `java -jar scriptella.jar --version` | Passed | `Scriptella ETL and Scripts Execution Tool. Version 1.4-SNAPSHOT` |
| `java -jar scriptella.jar --help` | Passed | Help displayed; no dependency errors |
| JavaScript and CSV ETL via `java -jar` | Passed | `HELLO_WORLD`, `CSV_ROW_1=Ada`, `CSV_ROW_2=Grace` |
| JavaScript and CSV ETL via `bin/scriptella.sh` | Passed | Same three assertions |
| In-memory H2 ETL | Passed | `H2_ROW_1=hello from h2` |
| Packaged `primes` example | Passed | Non-empty `report.html` and `report.csv`; report begins with prime rows |

No runtime errors, exceptions, or dependency-loading failures appeared in the
smoke-test logs.

## Archive checksums

| Archive | SHA-256 |
| --- | --- |
| `scriptella-1.4-SNAPSHOT.zip` | `fcdbdae25baf85ed09d3ba156e41fe29fc2237f5331d6020199d0ef347aa6c71` |
| `scriptella-examples-1.4-SNAPSHOT.zip` | `dc05a17fbfde900491ac67fb2593591a1e6818b622c21352e410258783a450bd` |

## Notes and scope

The distribution build emitted four Javadoc warnings about diagnostic markers
for invalid input. They did not fail the build or occur in runtime smoke logs,
and were recorded as understood build warnings. Native Windows batch-launcher
execution was not tested on this macOS host.

The complete command logs, generated inputs, unpacked archives, reports, and
checksums remain in the workspace-local `release-dir/` staging area and are
not part of the product repository.
