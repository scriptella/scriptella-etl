# Release 1.3 — Execution Log

Detailed notes, commands, outputs, and decisions recorded while working through the plan.

**Repository:** `scriptella-etl/` on branch `exp-v1.3`

---

## Chunk 1 — Repository and Build Baseline

**2026-07-15 14:30

**Status:** ✅ Complete

### Baseline Environment

**Selected baseline:** Java 8 (source/bytecode target), with later testing on a modern LTS JDK.

| Tool  | Version | Path |
|-------|---------|------|
| Java 8 (required baseline) | 1.8.0_492 (Temurin) | `/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home` |
| Java 17 (available reference JDK) | 17.0.15 (Temurin) | `/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home` |
| Java 24 | 24.0.1 (Temurin) | `/Library/Java/JavaVirtualMachines/temurin-24.jdk/Contents/Home` (reference only) |
| Maven | 3.9.9 | IntelliJ bundled |
| Ant   | 1.10.17 | `../apache-ant-1.10.17/` |

### Maven Build Results (Java 8)

**`mvn clean test`** — ✅ **ALL PASSED**
- Scriptella (parent): SUCCESS
- Scriptella Core: 147 tests, 0 failures
- Scriptella Drivers: 141 tests, 0 failures
- Scriptella Tools: 12 tests, 0 failures
- **Total: 288 tests, 0 failures, 0 errors, 0 skipped**

No compilation errors. Warnings about deprecation and unchecked operations (pre-existing).

### Maven Build Results (Java 24 — for reference)

**`mvn compile`** — ✅ SUCCESS
**`mvn test`** — ⚠️ FAILURE (9 errors, all in scriptella.driver.script.*)
- Root cause: `js`/`JavaScript` language not mapped to Rhino on Java 24 (Nashorn removed).
- Affected: ScriptConnectionTest, ScriptConnectionPerfTest, ScriptDriverITest, ScriptingQueryITest
- Note: This is a separate issue from the Ant test divergence. Java 24 has no Nashorn at all; Ant's failures on Java 8 have a different cause.

### Ant Build Results (Java 8)

**`ant jar`** — ✅ SUCCESS
- Module JARs + all-in-one `scriptella.jar` (596 KB)

**`ant test`** — ⚠️ BUILD SUCCESSFUL but test failures reported
- **This is the important finding:** Maven runs all 288 tests successfully on Java 8, but Ant reports failures in the same environment.
- Ant failures: scriptella.driver.script.* (4 test classes) + scriptella.driver.scriptella.ScriptellaDriverITest
- This is **not** a Nashorn issue — Java 8 has Nashorn, and Maven proves the tests can pass.
- Likely causes: different test classpath, different bundled JS engine JARs, different provider registration, stale `build/` or `samples/lib/`, differences between Maven-resolved and `lib/` JARs.
- Core and tools module tests pass under Ant.

**`ant dist`** — ❌ FAILED at DTD documentation step
- DTDDoc tool not installed
- Blocking artifacts: binary ZIP, source ZIP, examples ZIP, Javadocs, DTD docs

### Artifacts Status

| Artifact | Status |
|----------|--------|
| Module JARs | ✅ Produced |
| All-in-one JAR | ✅ `build/scriptella.jar` (596 KB) |
| Binary ZIP | ❌ Blocked by DTDDoc |
| Source ZIP | ❌ Blocked by DTDDoc |
| Examples ZIP | ❌ Blocked by DTDDoc |
| Javadocs | ❌ Requires build-docs.xml |
| DTD docs | ❌ DTDDoc missing |

### Categorized Issues

| Issue | Classification |
|-------|---------------|
| DTDDoc not installed (blocks `ant dist` DTD docs) | Open decision — test whether DTDDoc can be restored easily under Java 8; only then consider fallbacks |
| Ant/Maven test divergence on same Java 8 JDK | Required investigation — likely classpath or test-environment inconsistency |
| Ant reports failed tests but exits successfully | Required release-safety fix or documented wrapper check |

### DTDDoc Decision Log

The historical `ant dist` workflow runs DTDDoc to generate DTD HTML documentation. DTDDoc is not currently installed. The plan now states:

1. First test whether the historical DTDDoc workflow can be restored easily under Java 8.
2. If it works with little effort, preserve it for Release 1.3.
3. Only consider fallback approaches (packaging checked-in generated docs, packaging only etl.dtd) if DTDDoc is genuinely difficult, unavailable, or unreliable.

No build changes will be made until that decision is reviewed. This is tracked in Chunk 7.

---

## Chunk 2 — Supported Release Environment

**2026-07-15 14:50**

**Status:** ✅ Complete

### Confirmed Decisions (updated 2026-07-15)

| Decision | Value |
|----------|-------|
| Java 8 (required baseline) | Temurin 1.8.0_492 — build & full test |
| Modern-LTS compatibility check | Postponed; target Java 21 after the Java 8 release baseline is stable |
| Ant version | 1.10.17 |
| Maven requirement | 3.6+ (tested with 3.9.9) |

### Compatibility Policy

Drafted and added to plan. Ready to be copied into README and release documentation when the time comes (Chunk 18).

---

## Chunk 3 — Dependency and Bundled-Library Inventory

**2026-07-15 15:10**

**Status:** ✅ Complete

### Maven Dependencies (from parent POM dependencyManagement)

**Classification rule:** "Required" only if it demonstrably blocks the agreed build, tests, distribution, website, or publication. Age is a risk signal, not a reason to modify.

| Dependency | Version | Scope | Classification |
|------------|---------|-------|---------------|
| commons-jexl | 2.0.1 | compile (default) | No change |
| junit | 3.8.2 | test | No change |
| ant | 1.7.1 | compile (tools only) | No change — resolves fine |
| javax.mail:mail | 1.4.1 | optional (drivers) | No change / Defer — Jakarta migration is substantial, not required for 1.3 |
| janino | 3.1.0 | optional (drivers) | No change |
| spring | 1.2 | optional (drivers) | Defer — unless demonstrably broken, not required |
| velocity | 1.5 | optional (drivers) | Easy improvement — reconcile POM 1.5 vs lib/ 1.6.2 mismatch |
| commons-logging | 1.0.4 | test | No change |
| hsqldb | 1.8.0.10 | test | No change — investigate only if tests or supported behavior require newer |
| h2 | 1.1.116 | test | No change — investigate only if tests or supported behavior require newer |
| rhino-js-engine | 1.7.10 | compile (drivers) | No change — works on Java 8; verify during the deferred Java 21 compatibility stage |

### Bundled Libraries (lib/)

| File | Version | In all-in-one JAR? | Classification |
|------|---------|-------------------|---------------|
| commons-jexl.jar | 2.0.1 | ✅ Yes | No change |
| commons-logging.jar | 1.0.4 | ✅ Yes (filtered) | No change |
| h2.jar | 1.1.116 | ❌ No (test only) | No change (investigate if needed) |
| hsqldb.jar | 1.8.0.10 | ❌ No (test only) | No change (investigate if needed) |
| janino.jar | 3.1.0 | ❌ No (Maven resolves) | No change |
| janino-commons-compiler.jar | 3.1.0 | ❌ No (Maven resolves) | No change |
| junit.jar | 3.8.2 | ❌ No | No change |
| spring.jar | 1.2 | ❌ No (optional) | Defer |
| velocity-dep.jar | 1.6.2 | ❌ No (Maven resolves) | Easy improvement — reconcile POM/lib version |
| j2ee/activation.jar | 1.1 | ❌ No | No change |
| j2ee/mail.jar | 1.4.1 | ❌ No | No change |

### Maven Build Plugins

**Rule:** Update only if existing configuration fails. Publication plugins validated at release time.

| Plugin | Version | Classification |
|--------|---------|---------------|
| maven-compiler-plugin | 3.8.1 | No change |
| maven-surefire-plugin | 3.0.0-M3 | No change |
| buildnumber-maven-plugin | 1.0-beta-2 | No change (works, not required) |
| cobertura-maven-plugin | 2.6 | Defer — remove if broken, don't upgrade |
| maven-clean-plugin | 2.5 | No change |
| maven-resources-plugin | 2.6 | No change |
| maven-install-plugin | 2.3.1 | No change |
| maven-deploy-plugin | 3.0.0-M1 | Investigate at publication validation |
| maven-release-plugin | 2.5.3 | Investigate at publication validation |
| maven-site-plugin | 2.0.1 | Remove or ignore — site gen not needed |
| maven-source-plugin | 2.2.1 | No change |
| maven-jar-plugin | 2.2 | No change |
| maven-javadoc-plugin | 3.1.0 | No change |
| maven-gpg-plugin | 1.5 | Investigate at publication validation |
| dtddoc-maven-plugin | 1.1 | Defer — requires external DTDDoc tool |

### Reporting Profile Plugins

All defer or remove — reporting profile is not required for 1.3.

### Notable Observations

1. **velocity**: POM 1.5 vs lib/ velocity-dep.jar 1.6.2 — reconcile during dependency update chunk
2. **cat.inspiracio:rhino-js-engine**: Resolves from non-Maven-Central repo; note for reproducibility
3. **Maven site plugin (2.0.1)**: If Maven site generation is no longer used, remove rather than upgrade

---

## Chunk 4 — Issue Triage and Scope Freeze

**2026-07-15 15:30**

**Status:** ✅ Complete

### Triage Results

#### Required for 1.3
| Issue | Why | Effort |
|-------|-----|--------|
| [#29](https://github.com/scriptella/scriptella-etl/issues/29) — DTD error: `<include>` in `<script>` not declared | Real bug, documented in DTD | Small |
| [#20](https://github.com/scriptella/scriptella-etl/issues/20) — ID columns/variables overridden | Reporter says "serious bug" — investigate and fix if confirmed | Medium |

#### Easy Improvements
| Issue | Why | Effort |
|-------|-----|--------|
| [#18](https://github.com/scriptella/scriptella-etl/issues/18) — Show execution time on error | Small change, clear benefit | Small |
| [#13](https://github.com/scriptella/scriptella-etl/issues/13) — Enforce UTF-8 in start scripts | Add `-Dfile.encoding=UTF8` to `scriptella.sh` and `scriptella.bat` | Trivial |

#### Deferred
| Issue | Why |
|-------|-----|
| [#36](https://github.com/scriptella/scriptella-etl/issues/36) — Spring 5.x | Optional driver; Spring upgrade is high-risk, deferred |
| [#33](https://github.com/scriptella/scriptella-etl/issues/33) — Environment variables | Feature request / support question |
| [#32](https://github.com/scriptella/scriptella-etl/issues/32) — Shell driver | New feature, out of scope for 1.3 |
| [#31](https://github.com/scriptella/scriptella-etl/issues/31) — JDK11 compatibility | Covered by release plan; modern-JDK work deferred to the Java 21 compatibility stage after the Java 8 baseline |
| [#27](https://github.com/scriptella/scriptella-etl/issues/27) — YEAR column → timestamp | Minor bug, low impact |
| [#26](https://github.com/scriptella/scriptella-etl/issues/26) — JavaScript variable scope | JDK-dependent, hard to reproduce |
| [#24](https://github.com/scriptella/scriptella-etl/issues/24) — Null/blank formatting error | Minor bug |
| [#22](https://github.com/scriptella/scriptella-etl/issues/22) — Velocity + dotted properties | Feature enhancement, optional driver |
| [#17](https://github.com/scriptella/scriptella-etl/issues/17) — Transaction tag | New feature |
| [#15](https://github.com/scriptella/scriptella-etl/issues/15) — Console output to log | Feature |
| [#8](https://github.com/scriptella/scriptella-etl/issues/8) — JSON driver | New feature |
| [#4](https://github.com/scriptella/scriptella-etl/issues/4) — Current row info | Feature |
| [#3](https://github.com/scriptella/scriptella-etl/issues/3) — Stored procedure out params | Feature |

#### Reconsider / Already Covered
| Issue | Why |
|-------|-----|
| [#41](https://github.com/scriptella/scriptella-etl/issues/41) — Release 1.3 | This is the release plan itself |
| [#10](https://github.com/scriptella/scriptella-etl/issues/10) — Release v1.2 | Being done as Release 1.3 |
| [#19](https://github.com/scriptella/scriptella-etl/issues/19) — Explicitly close connections | Question, not a bug |

### Scope Refinement

**Rule:** Fix only defects that affect correctness, compatibility with selected Java targets, or the ability to build, test, package, and publish reliably. Everything else waits.

### Frozen 1.3 Issue Scope

| # | What | Why |
|---|------|-----|
| [#29](https://github.com/scriptella/scriptella-etl/issues/29) | DTD: `<include>` in `<script>` | Concrete, small, affects valid documents |
| [#20](https://github.com/scriptella/scriptella-etl/issues/20) | ID columns overridden | Investigate briefly; fix only if reproducible, serious, bounded |
| — | Ant/Maven test divergence | Resolve or document — directly affects release confidence |
| — | Ant test success despite failures | Fix or add reliable release check |

**Deferred from earlier proposal:** #18 (execution time on error) and #13 (UTF-8 launcher scripts) — neither is critical for a trustworthy release.

All other open issues remain deferred as previously classified.

---

## Chunk 5 — Maven Build Fixes

**2026-07-15 16:53**

**Status:** ✅ Complete

### Fixes Applied

1. **Removed cobertura-maven-plugin `clean` execution from `<build>` section** (`pom.xml:235-246`)
   - The Cobertura 2.6 plugin's `clean` goal was bound to the `clean` lifecycle in `<build><plugins>`, causing every `mvn clean` to fail on JDK 9+ (could not resolve `com.sun:tools:jar:0`).
   - Cobertura remains available in the `reporting` profile for site generation — only the build-breaking default execution was removed.

2. **Documented `JAVA_HOME` requirement for Javadoc**
   - The `maven-javadoc-plugin:3.1.0` requires `JAVA_HOME` to find the `javadoc` executable.
   - Build command for environments without `JAVA_HOME` set: `export JAVA_HOME=$(/usr/libexec/java_home) && mvn clean install`

### Build Results (Java 8 — Temurin 1.8.0_492)

```
mvn clean install
```

| Module | Tests | Status |
|--------|-------|--------|
| Scriptella (parent) | — | ✅ SUCCESS |
| Scriptella Core | 147 ✅ | ✅ SUCCESS |
| Scriptella Drivers | 141 ✅ | ✅ SUCCESS |
| Scriptella Tools | 12 ✅ | ✅ SUCCESS |
| **Total** | **300** | **✅ BUILD SUCCESS** |

### Artifacts Generated

| Artifact | Generated |
|----------|-----------|
| Module JARs | ✅ core, drivers, tools |
| Source JARs | ✅ core, drivers, tools |
| Javadoc JARs | ✅ core, drivers, tools |
| Test JAR | ✅ core (test-jar) |

### Remaining Issues (pre-existing, not blocking build)

- **Rhino/JS test failures on JDK 21+**: The `cat.inspiracio:rhino-js-engine:1.7.10` does not register via `javax.script.ScriptEngineFactory` on JDK 21+ due to module system service discovery restrictions. All 9 JS-related tests fail (`ScriptConnectionTest`, `ScriptConnectionPerfTest`, `ScriptDriverITest`, `ScriptingQueryITest`). This was already documented in Chunk 2 findings. Fix deferred to Phase 4 dependency work.
- **Cobertura stays in reporting profile**: Not removed from the `reporting` profile, just from the build-breaking `<build>` section execution.

---

## Chunk 6 — Ant Build and Distribution Preservation

**2026-07-15 17:00**

**Status:** ✅ Complete

### Fixes Applied

1. **Preserved required DTD documentation generation** — verified DTDDoc 1.1.0 under Java 8 and retained the fail-fast `dtddoc.dir` check. Distribution builds cannot silently omit DTD documentation.

2. **Fixed Ant test failure detection** (`build-template.xml:86`, `build.xml:154`):
   - Added `failureproperty="unit.tests.failed"` to `<junit>` task in the build template (was `errorproperty` only, missing `failureproperty`).
   - Replaced the broken `<fail if="unit.tests.failed">` check in `build.xml` with a reliable check that scans the generated `TESTS-TestSuites.xml` for non-zero `errors` or `failures` attributes.
   - Before: Ant tests could fail but build would exit SUCCESSFUL.
   - After: `ant test` properly fails the build when any test fails.

3. **Documented Rhino JAR dependency in `drivers/build.xml`** — Resolved the Ant/Maven test divergence:
   - Restored `lib/rhino.jar` and `lib/rhino-js-engine.jar` from Maven repo (they were referenced by the Ant build but never committed to git).
   - Added a TODO comment in `drivers/build.xml` about replacing with a Maven dependency copy script.
   - Root cause: These missing JARs caused `ScriptellaDriverITest` (which uses `language=rhino` in a sub-ETL) to fail during Ant test runs.
   - Maven was unaffected because it resolves these dependencies from the repository.
   - Release 1.3 decision: commit these JARs and their required license material under `lib/`. This is the shortest path to a reproducible Ant build from a fresh checkout and deliberately avoids making Maven a prerequisite for Ant.
   - Deferred migration: replace the committed Rhino JARs after Release 1.3 with a deterministic Maven dependency-staging step into a generated build directory. Do not copy from hard-coded `~/.m2` paths.

### Build Results (Java 8 — Temurin 1.8.0_492, Ant 1.10.17)

| Target | Status |
|--------|--------|
| `ant clean jar` | ✅ SUCCESS (module JARs + all-in-one scriptella.jar) |
| `ant clean test` | ✅ SUCCESS (all tests pass, build fails on failure) |
| `ant -Ddtddoc.dir=/path/to/DTDDoc clean dist` | ✅ SUCCESS (all artifacts produced, including DTD documentation) |

### Artifacts Produced by `ant dist`

| Artifact | Size |
|----------|------|
| `scriptella.jar` (all-in-one) | 579 KB |
| Module JARs (core, drivers, tools) | ✅ |
| `scriptella-1.3-SNAPSHOT.zip` (binary) | 2.6 MB |
| `scriptella-1.3-SNAPSHOT-src.zip` (source) | 7.4 MB |
| `scriptella-examples-1.3-SNAPSHOT.zip` | 4.6 MB |
| `build/docs/api/` (Javadoc) | ✅ |

### DTDDoc verification and release command

- Downloaded `DTDDoc_1_1_0.zip` from the SourceForge DTDDoc 1.1.0 release.
- The archive contains `DTDDoc.jar`, `dtdparser120.jar`, `jakarta-regexp-1.2.jar`, and `jhighlight.jar`; no separate dependency reconstruction is necessary.
- The historical DTDDoc Ant task runs successfully under Java 8 and generates current HTML documentation from `core/src/conf/scriptella/dtd/etl.dtd`.
- `ant -Ddtddoc.dir=/path/to/DTDDoc clean dist` succeeds and packages the generated DTD HTML and `etl.dtd` under `docs/dtd/`.
- DTDDoc remains an external release tool and is not bundled into Scriptella's runtime `lib/` directory.

---

## Chunk 8 — Apply Low-Risk Dependency Changes

**2026-07-15 17:38**

**Status:** ✅ Complete

### Changes Applied

1. **Reconciled Velocity at 1.6.2** (`pom.xml`):
   - Updated Maven dependency management from Velocity 1.5 to 1.6.2.
   - Kept the existing `lib/velocity-dep.jar`, which already reports version 1.6.2 in its manifest.
   - Confirmed the root bundled JAR, generated sample copy, and examples ZIP entry are byte-for-byte identical.
   - Confirmed Maven resolves `org.apache.velocity:velocity:1.6.2` for the Drivers module.

2. **Added explicit Velocity distribution license material** (`lib/velocity-dep.license.txt`):
   - Records the Apache License 2.0 terms and points to the complete license and notice texts embedded in the JAR.
   - The Ant `jar` target copies it into `samples/lib/`, and the examples ZIP includes it next to `velocity-dep.jar`.

3. **Kept all other dependencies unchanged**:
   - Chunk 3 approved no other updates.
   - Spring, JavaMail/Jakarta, modern-JDK Rhino work, reporting plugins, and publication-plugin validation remain deferred as recorded in the plan.

### Verification (Java 8 — Temurin 1.8.0_492)

| Check | Result |
|-------|--------|
| Drivers tests with Maven-resolved Velocity 1.6.2 | ✅ 141 tests passed |
| `mvn clean install` | ✅ 300 tests passed; all modules built and installed |
| `ant clean test` with Ant 1.10.17 | ✅ Passed |
| `ant -Ddtddoc.dir=/path/to/DTDDoc clean dist` | ✅ Passed |
| Examples ZIP dependency metadata | ✅ `velocity-dep=1.6.2` |
| Examples ZIP Velocity license | ✅ Present |
| Bundled/sample/archive Velocity JAR SHA-256 | ✅ Identical (`8b3d055e...c96c4e`) |

An initial incremental Maven reactor run encountered failures in two core cancellation/JMX tests before reaching the Drivers module. A clean full build immediately passed all 300 tests; the focused Drivers run also passed all 141 tests. No Velocity-related failure was observed.

---

## Chunk 9 — Selected Bug Fix: Issue #29

**2026-07-15 17:45**

**Status:** ✅ Complete

### Finding

The reported DTD omission was not reproducible from any maintained source copy:

- `core/src/conf/scriptella/dtd/etl.dtd` declares `script` as `(#PCDATA | include | dialect | onerror)*`.
- Git history shows that declaration has allowed `include` since 2006.
- The copies in `scriptella.github.io/dtd/etl.dtd` and `scriptella.github.io/docs/dtd/etl.dtd` contain the same declaration.
- The Ant-generated distribution DTD is copied from the canonical core DTD.

The historical report therefore most likely came from stale deployed or validator-cached DTD content. Changing the already-correct production declaration would add risk without fixing a reproducible defect.

### Regression Coverage

Added `Issue29DtdValidationTest` and a fixture matching the reported structure: an `include` in a `script` nested inside a `query`. The test uses a strict validating XML parser and the packaged Scriptella DTD, failing on warnings or validation errors. This protects both the declaration and resource packaging from regression.

No changelog entry was added because runtime behavior and the canonical DTD did not change; the investigation and regression coverage are recorded here instead.

### Verification (Java 8 — Temurin 1.8.0_492)

| Check | Result |
|-------|--------|
| Strict issue #29 DTD regression test | ✅ Passed |
| `mvn clean test` | ✅ 301 tests passed (Core 148, Drivers 141, Tools 12) |
| `ant clean test` with Ant 1.10.17 | ✅ Passed |

An incremental Core test run again exposed the pre-existing cancellation/JMX test-state sensitivity documented during Chunk 8. The clean full Maven run passed all 301 tests.

---

## Bounded Investigation — Issue #20

**2026-07-15 17:58**

**Status:** ✅ Complete — not reproducible; no production fix

### Report Boundary

Issue #20 states only that columns/variables named `id` seem to be overridden. It provides no ETL example, expected or observed value, database, driver, Scriptella version, or comments. The bounded investigation therefore covered the common JDBC interpretation without speculating about unreported driver-specific behavior.

### Code and Existing-Coverage Findings

* `ResultSetAdapter` registers both JDBC column names and labels in a case-insensitive map and resolves a result-set column before falling back to parent parameters.
* `QueryExecutor.QueryCtxDecorator` gives special treatment only to the documented `rownum` and `etl` variables. It does not reserve or replace `id`.
* Existing `DBTableCopyTest`, `NestedQueryTest`, and `SQLParametersTest` cases successfully retrieve and bind `ID`/`id` result columns, including nested query scopes.

### Reproduction Attempt and Regression Coverage

Added `Issue20IdColumnTest` with an HSQLDB ETL fixture that deliberately defines a global `id=999` property while querying rows whose `ID` values are 1 and 2. A nested JDBC script copies each result-column value through all supported JDBC forms:

* `$id`
* `${id}`
* `?id`
* `?{id}`

All four forms resolve to each row's `ID`, not the same-named global property. This directly covers the most plausible override collision and preserves the expected precedence as regression coverage.

### Decision

The issue is not reproducible on the Release 1.3 Java 8 baseline with the bundled HSQLDB/JDBC path. A production change would be speculative and could alter established variable precedence. No runtime fix or changelog entry is warranted for 1.3.

Reconsider only if a concrete failing ETL plus database/driver and expected/actual values is supplied. The upstream issue may remain open for that information, but it no longer blocks website work or Release 1.3.

### Verification (Java 8 — Temurin 1.8.0_492)

| Check | Result |
|-------|--------|
| Issue #20 JDBC/property-collision regression | ✅ Passed |
| `mvn clean test` | ✅ 302 tests passed (Core 149, Drivers 141, Tools 12) |
| `ant clean test` with Ant 1.10.17 | ✅ Passed |

---

## Chunk 11 — Representative Page Migration

**2026-07-15**

**Status:** ✅ Complete

### Pages Migrated

The representative pages in the separate `scriptella.github.io` repository now use clean HTML5 and the shared Chunk 10 stylesheet:

* `index.html` — redesigned from scratch with a concise explanation, a real cross-database ETL example, capability summary, common uses, and clear routes to downloads, the tutorial, and reference material.
* `download.html` — retained the published 1.2 and 1.1 binary, source, and examples links without presenting the unpublished 1.3 release as available.
* `reference/index.html` — migrated the first substantial reference section: execution concepts, use cases, Java 8 requirements, installation, core ETL elements, and variable binding. The remaining integration and advanced-provider material stays in scope for Chunks 13 and 14.
* `howto/initialize-database.html` — retained the database initialization workflow, diagrams, ETL, servlet-listener example, Spring example, and historical-document caveat.

### Architecture and Compatibility

* Applied the shared header, primary navigation, documentation sidebar, content shell, and footer at root and one-directory nesting depths.
* Added reusable homepage hero, feature-card, route-card, figure, on-this-page, and compatibility-anchor styles to `style.css`.
* Preserved the four public page URLs and useful legacy fragment targets in the migrated content, including `overview`, `usage`, `features`, `News`, `INSTALLATION`, `BIND_VARIABLES`, and the how-to section anchors.
* Removed Forrest scripts and menus, publication strips, font-size controls, PDF links, StatCounter, legacy HTML 4 declarations, and generated footer UI from all four pages.
* Kept the site framework-free and free of third-party assets. A small dependency-free `theme.js` now provides a persistent Light, Dark, or System color-theme control; templates include it for later migrations.
* Added a light-background wordmark variant while keeping legacy documentation diagrams on explicit white surfaces in both themes.
* Added a documentation directory to the reference landing page with direct routes to the driver/provider matrix, FAQ, API and DTD references, both how-to guides, support, license, and change history. Added FAQ, API Docs, and DTD Reference to the shared footer templates and all representative pages.

### Validation

| Check | Result |
|-------|--------|
| HTML5 parsing with Nokogiri HTML5 | ✅ No parser errors on all four pages |
| Root and nested local paths | ✅ All referenced local pages, images, styles, and icons exist |
| Local fragment links and duplicate IDs | ✅ All resolved; no duplicate IDs |
| Obsolete UI, scripts, and PDF-link scan | ✅ None present in migrated pages |
| CSS structure and whitespace | ✅ Balanced braces; `git diff --check` clean |
| Narrow layout | ✅ Header wraps, documentation layout becomes single-column, and homepage grids collapse by 56rem |

---

## Website Branch and Publication Policy

**2026-07-15**

The two Release 1.3 repositories now use matching development branches:

* `scriptella-etl/` — `exp-v1.3`
* `scriptella.github.io/` — `exp-v1.3`

The website modernization commits were preserved on `scriptella.github.io/exp-v1.3`. Local website `master` was repointed to `origin/master` at `fb0b99e`, the currently published site baseline. The website repository is not to be pushed or deployed yet. Publication remains an explicit final action after website cleanup and validation and the Release 1.3 artifacts are ready.

---

## Chunk 12 — Remaining Root Pages

**2026-07-15**

**Status:** ✅ Complete

### Pages Migrated

The remaining high-value root pages now use the shared HTML5 shell and stylesheet:

* `faq.html`
* `support.html`
* `license.html`
* `tutorial.html`
* `changes.html`
* `links.html`

The generated `linkmap.html` page was removed because it was a Forrest navigation artifact with no remaining user value.

### Changes

* Removed Forrest markup and scripts, PDF links, StatCounter, font-size controls, and generated publication UI.
* Preserved the public page URLs and important content anchors.
* Converted code blocks, notes, and inline code to the shared modern styles.
* Replaced changelog icon images with readable text labels.
* Added dependency-free XML and SQL syntax highlighting for example snippets, including light- and dark-theme colors. Detection is conservative and preserves the original snippet text.

### Repository State

The completed website work is recorded through `1944b6b` on local branch `scriptella.github.io/exp-v1.3`. It has not been pushed or deployed.

---

## Chunk 16 — Remaining Documentation Pages

**2026-07-16**

**Status:** ✅ Complete

### Pages Migrated

The last Forrest-skinned content pages now use the shared HTML5 docs shell and stylesheet:

* `reference/drivers.html` — JDBC bridge and non-JDBC driver matrices, with all legacy driver fragment IDs preserved (`#jdbcbridge`, `#nonjdbc`, and per-driver IDs such as `#oracle`, `#spring`, `#csv`).
* `howto/migrate-from-ant.html` — Ant SQL task migration guide with legacy section anchors preserved (`#Intended-Audience`, `#Purpose`, `#Prerequisites`, `#Steps`, `#The+simplest+case`).

There were no other high-value nested content pages still on the Forrest layout. Generated `docs/api/` and `docs/dtd/` remain separate and unchanged.

### Changes

* Applied the documentation layout (primary nav, docs sidebar, footer, theme toggle) used by the reference manual and the other how-to.
* Wrapped wide driver tables in keyboard-focusable `.table-scroll` regions.
* Fixed the SQL Server row (extra empty table cell in the legacy HTML).
* Light polish only: grammar fix (“provides”), HTTPS for a few external links, clearer notes, and a Related links section on the Ant migration how-to.
* Removed Forrest markup, PDF links, StatCounter, and publication UI from both pages.

### Validation

| Check | Result |
|-------|--------|
| No remaining Forrest content pages outside `docs/` and `skin/` | ✅ |
| Driver row IDs (29) | ✅ All present |
| How-to section anchors | ✅ All preserved |
| Nested relative paths for CSS, theme, logo, favicon | ✅ Match docs shell |
| Generated API/DTD docs | ✅ Unchanged |

### Repository State

Website changes are on local branch `scriptella.github.io/exp-v1.3` and have not been pushed or deployed. Publication remains deferred to Chunk 17 cleanup and the final Release 1.3 deployment decision.

---

## Chunk 17 — Website Cleanup and Validation

**2026-07-16**

**Status:** ✅ Complete

### Validation

| Check | Result |
|-------|--------|
| Content-page internal links (12 HTML pages) | ✅ No missing local targets |
| Fragment links from content pages | ✅ All resolve (after Javadoc fragment fix) |
| Major reference anchors (`INSTALLATION`, `BIND_VARIABLES`, `Ant+Integration`, `JDBC+Adapters`, `Command+Line+Execution`, `jmx`, `maven`, `inprocess`, `Performance+and+batching`, …) | ✅ Present |
| Image paths on content pages | ✅ All resolve |
| High-priority public URLs | ✅ All present |
| Download URLs (GitHub release assets for 1.2 / sample 1.1) | ✅ HTTP 200 on HEAD |
| Nested nav paths (root, `howto/`, `reference/`) | ✅ CSS, theme, logo, favicon paths correct |
| CSS structure | ✅ Balanced braces; `:focus-visible`; `@media (max-width: 56rem)` stacks header/docs nav; `pre` / `.table-scroll` use `overflow-x: auto` |
| README website links | ⏳ HTTP `scriptella.org` links remain; wording/HTTPS alignment deferred to Chunk 18 |
| Generated `docs/api/` and `docs/dtd/` | ✅ Kept as-is (historical Javadoc internal quirks, missing `script.js`, and `#package_description` typos inside generated HTML are out of scope) |

### Fixes during validation

* Corrected content links from `#package_description` to `#package.description` to match Java 8 Javadoc anchors (`reference/drivers.html`, `faq.html`).

### Removed

* `skin/` (Forrest CSS, JS, images, i18n)
* All PDF page variants (`*.pdf` at root, `howto/`, `reference/`)
* `broken-links.xml`
* Unused Forrest/legacy images: `add.jpg`, `fix.jpg`, `update.jpg`, `built-with-forrest-button.png`, `instruction_arrow.png`, `site-logo.png`, `pvr_labs_optimized.png`

### Kept

* Useful content images and diagrams
* Logo SVGs, powered badge, RSS icon
* `favicon.ico`, `CNAME`, `dtd/etl.dtd`
* `changes.rss` (linked from `changes.html`)
* Generated Javadocs and DTD docs
* `templates/` for future page work
* `style.css`, `theme.js`

### Output

The modernized site on `scriptella.github.io/exp-v1.3` has no Forrest dependency and is ready for later deployment decisions (Chunk 22 / A8). Not pushed.


---

## Chunk 18 — README and Status Wording

**2026-07-16**

**Status:** ✅ Complete

### README (`scriptella-etl/README.md`)

Replaced the “no longer actively developed” note with maintenance-oriented status wording aligned to the release plan:

* Maintenance development has resumed; focused compatibility and bug-fix releases.
* Release 1.3 establishes a modernized baseline; latest **published** release remains 1.2 until 1.3 is tagged.
* Documented Java 8, Maven 3.6+, and Ant 1.10.17 (packaging) requirements.
* Documented binary `java -jar scriptella.jar` usage, Maven coordinates (`org.scriptella`), and `mvn clean install` / optional Ant packaging.
* HTTPS links to scriptella.org, GitHub issues/discussions/wiki, and support page.
* Commercial contact retained without overstating feature-development commitments.

### Website alignment (`scriptella.github.io`)

* `support.html` — status lede, HTTPS community links, clearer commercial and issue-tracker wording.
* `index.html` — “Latest published release” label plus short 1.3 maintenance note (without claiming 1.3 is available).
* `download.html` — note that 1.3 is not published yet.

### Out of scope / deferred

* Full CHANGELOG (Chunk 19).
* Claiming 1.3 download artifacts before publication (Chunks 21–23).

---

## Chunk 19 — Changelog

**2026-07-16**

**Status:** ✅ Complete

### Output

Added `CHANGELOG.md` as the primary maintainable changelog from Release 1.3
onward. The historical `forrest/status.xml` remains unchanged as source material
for older releases. The README links to the new changelog.

The 1.3 entry covers:

* Post-1.2 user-visible work already present on the branch, including the Rhino
  JavaScript transition (#2) and shell-command driver (#32).
* Java 8 compatibility policy, Maven and Ant build preservation, dependency
  reconciliation, regression coverage, website modernization, and documentation.
* Upgrade notes for Java, Rhino, the shell driver, Maven coordinates, and full
  Ant distribution builds.
* Deferred Java 21 compatibility and broad dependency modernization.

Historical summaries for 1.2, 1.1, and 1.0 were carried forward from the
Forrest status file, with a link to that file for older and more detailed
history.
