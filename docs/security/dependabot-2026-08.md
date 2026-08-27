# Issue #58: Dependabot and JDK 25 disposition

**Status:** Remediation implemented; validation evidence is recorded below.

**Review date:** 2026-08-27

## Decision

The three Dependabot alerts named in issue #58 are remediated without a risk
acceptance. Scriptella no longer declares, builds, or ships Commons Lang 2.x,
and the Spring driver no longer uses the unsupported Spring Framework 5.3.x
line. The Spring driver is now on Spring Framework 7.0.9, the OSS-supported
line that fixes the later 6.2.x security advisories reviewed for this release.

The optional drivers remain optional in Maven. The main all-in-one JAR and
binary distribution do not embed their optional runtime libraries. The
examples distribution contains the reviewed Velocity runtime set so the
packaged `primes` example remains executable.

This disposition is repository-only release evidence and is intentionally not
included in the binary or source ZIPs. Packaged copies of `CHANGELOG.md` link
to issue #58 for the public release reference.

## Dependency dispositions

| Alert | Previous dependency | Disposition | Reviewed version |
|---|---|---|---|
| CVE-2025-48924 / GHSA-j288-q9x7-2f5v | `commons-lang:commons-lang:2.6` | Removed. Velocity was migrated to the maintained engine and its `org.apache.commons.lang3` API. | `org.apache.commons:commons-lang3:3.20.0` |
| CVE-2024-38820 / GHSA-4gc7-5j7h-4qph | Spring Framework `5.3.39` | Removed. Spring 7.0.9 is outside the affected product ranges. | Spring Framework `7.0.9` |
| CVE-2025-22233 / GHSA-4wp7-92pw-q264 | Spring Framework `5.3.39` | Removed. Spring 7.0.9 is outside the affected product ranges. | Spring Framework `7.0.9` |
| CVE-2026-41850 / CVE-2026-41851 / CVE-2026-41852 | Spring Framework `6.2.12` | Replaced. Spring 7.0.9 includes the OSS fixes for the SpEL DoS, cache-growth, and method-invocation advisories. | Spring Framework `7.0.9` |
| CVE-2026-59280 / CVE-2026-59281 / CVE-2026-59282 / CVE-2026-59283 | Spring Framework `6.2.12` | Replaced. Spring 7.0.9 is the published OSS fix for the August 20, 2026 Spring Framework advisories, including DataBinder and SpEL issues. | Spring Framework `7.0.9` |

The Spring driver uses `BeanFactory`, application-context XML, Spring JDBC,
and `DriverManagerDataSource`. It does not declare or ship Spring MVC, WebFlux,
FreeMarker integration, request handlers, SpEL evaluation, or application
request binding. That reachability review is additional assurance; it is not
used as a substitute for the dependency upgrade. The fixed Spring 7.0.9
artifacts are used even for paths that Scriptella does not expose.

The Velocity 2.4.1 migration changes the documented optional runtime contract
from `velocity.jar`, Commons Collections 3, and Commons Lang 2 to
`velocity-engine-core.jar`, Commons Lang 3, and `slf4j-api.jar`. The driver
tests cover dependency diagnostics, evaluation, parameter substitution, and
the packaged primes example.

Advisory references:

* [GHSA-j288-q9x7-2f5v](https://github.com/advisories/GHSA-j288-q9x7-2f5v)
* [Spring CVE-2024-38820](https://spring.io/security/cve-2024-38820)
* [Spring CVE-2025-22233](https://spring.io/security/cve-2025-22233/)
* [Spring Framework 7.0.8 and 6.2.19 release](https://spring.io/blog/2026/06/08/spring-framework-7-0-8-and-6-2-19-available-now/)
* [CVE-2026-41850](https://spring.io/security/cve-2026-41850/), [CVE-2026-41851](https://spring.io/security/cve-2026-41851/), and [CVE-2026-41852](https://spring.io/security/cve-2026-41852/)
* [CVE-2026-59280](https://spring.io/security/cve-2026-59280/), [CVE-2026-59281](https://spring.io/security/cve-2026-59281/), [CVE-2026-59282](https://spring.io/security/cve-2026-59282/), and [CVE-2026-59283](https://spring.io/security/cve-2026-59283/)
* [Apache Commons Lang release notes](https://commons.apache.org/proper/commons-lang/changes.html)

## Distribution inventory

The authoritative inputs are `pom.xml`, module POMs, `lib/`, and the Ant
packaging files. The release validation must inspect the generated archives,
not only those inputs.

| Artifact | Reviewed dependency contents |
|---|---|
| Maven `scriptella-drivers` | Optional `velocity-engine-core:2.4.1`, `commons-lang3:3.20.0`, `slf4j-api:1.7.36`, Commons Logging 1.3.5, Spring 7.0.9 modules, Micrometer Observation/Common 1.16.7, and JSpecify 1.0.0 |
| Ant all-in-one `scriptella.jar` | Core/tools/drivers plus required JEXL and Commons Logging; no optional Velocity or Spring runtime jars |
| Binary ZIP | All-in-one JAR, required libraries, and Rhino; no optional Velocity or Spring runtime jars |
| Examples ZIP | All-in-one JAR plus `velocity-engine-core:2.4.1`, `commons-lang3:3.20.0`, and `slf4j-api:1.7.36`; no Spring runtime jars |
| Source ZIP | Reviewed `lib/` inputs, including the complete Spring 7.0.9 runtime closure, Commons Logging 1.3.5, Micrometer Observation/Common 1.16.7, JSpecify 1.0.0, and the maintained Velocity set |

The validation script asserts exact JAR names and manifest versions for the
Spring runtime closure and rejects the removed `commons-lang.jar`,
`commons-collections.jar`, and `velocity.jar` artifacts. The examples ZIP
does not carry the optional Spring closure because it does not carry Spring
itself.

## JDK policy

* Java 17 is the minimum supported runtime and the class-file/API baseline.
* Maven and Ant compile product code with `--release 17`; class files must be
  major version 61 even when built by a newer JDK.
* JDK 17 is the compatibility gate.
* JDK 25 is the selected release-build JDK. CI runs Maven tests and Ant
  compile/test/package checks on both JDK 17 and JDK 25. A release is not
  publishable unless both matrix legs pass.
* The selected JDK 25 release-build version is Temurin 25.0.4.1. The local
  review host now has both selected JDKs, and the committed CI matrix remains
  the reproducible enforcement point for clean-checkout validation.

## Validation evidence

The following local release-candidate validation completed on 2026-08-27 on
macOS 15.6.1 x86_64 with Temurin 17.0.15, Temurin 25.0.4.1, Maven 3.9.9,
Ant 1.10.17, and DTDDoc 1.1.0:

* `mvn --batch-mode --no-transfer-progress clean verify` — **PASS**;
  core 165 tests, drivers 166 tests, tools build and tests passed.
* `ant -noinput clean test jar` — **PASS**; all three Ant module test suites,
  module JARs, and the all-in-one JAR passed. The CI command additionally
  packages the source and examples archives on both JDK matrix legs.
* `ant -noinput -Ddtddoc.dir=/Users/pvr/dev/prj/scriptella/DTDDoc clean dist`
  — **PASS**; binary, source, and examples ZIPs generated.
* `ant -noinput -Ddtddoc.dir=/Users/pvr/dev/prj/scriptella/DTDDoc
  test-distribution` — **PASS**; optional runtime and installer contracts
  passed, including the unpacked Java 17 launcher and representative ETLs.
* The same full Maven `clean verify`, Ant `clean dist`, and Ant
  `test-distribution` commands passed on Temurin 25.0.4.1. The JDK 25 run
  produced valid binary and examples archives, and the distribution contract
  passed with the same reviewed dependency contents.
* The same Maven reactor and Ant `clean test jar` commands passed on local
  Temurin 24.0.1 as an additional newer-JDK signal.
* All inspected product classes and the all-in-one JAR report class-file major
  version **61**.

Candidate archive SHA-256 values from the final Temurin 17 run:

```text
scriptella-1.5-SNAPSHOT.zip         2a3d2baf69647d5bdcf841d5ca2d27a9469aba75284a9b49f7ba93edf2b6a756
scriptella-1.5-SNAPSHOT-src.zip     7b62a414112c14f2ad7621e08d9c2ae4be174ce9ec29e9e44313a18e16c008a4
scriptella-examples-1.5-SNAPSHOT.zip b238da028a27a5a8d8ec448b8363acd3ce782ad095f77479f2531ee6c7b63c10
```

Candidate archive SHA-256 values from the Temurin 25.0.4.1 run:

```text
scriptella-1.5-SNAPSHOT.zip         7a984a45db17c56a7b8803c58d6a3a881437376080ca7285c9d41c064023093e
scriptella-1.5-SNAPSHOT-src.zip     7b62a414112c14f2ad7621e08d9c2ae4be174ce9ec29e9e44313a18e16c008a4
scriptella-examples-1.5-SNAPSHOT.zip fcd27acadaed6b1b9fa0a415e1517303c3abb850870ab2122804b946ca2e01ac
```

The JDK 25 matrix is committed in `.github/workflows/ci.yml` and enforces the
same Maven and Ant checks on clean CI runners.

Run from a clean checkout with the selected JDKs:

```text
mvn --batch-mode --no-transfer-progress clean verify
ant -noinput clean test jar
ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" clean dist
ant -Ddtddoc.dir="$SCRIPTELLA_DTDDOC" test-distribution
```

The distribution contract covers archive integrity, exact dependency versions,
all-in-one contents, launcher startup, representative ETL execution, nested
JavaScript, and the Velocity/H2 `primes` sample. Record these additional checks
for each release candidate:

```text
java -version
unzip -t build/scriptella-<version>.zip
unzip -t build/scriptella-examples-<version>.zip
javap -verbose ... | grep 'major version: 61'
```

No maintainer risk acceptance is required for the three issue #58 alerts after
the remediation. If a future release reintroduces one of the removed versions,
publication is blocked until the dependency is fixed or a maintainer records
scope, exploitability, mitigation, owner, and review date here.
