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
| Java 17 (required modern-LTS) | 17.0.15 (Temurin) | `/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home` |
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
| Java 17 (required modern-LTS check) | Temurin 17.0.15 — compile & test |
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
| rhino-js-engine | 1.7.10 | compile (drivers) | No change — works on Java 8; verify on Java 17 |

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
| [#31](https://github.com/scriptella/scriptella-etl/issues/31) — JDK11 compatibility | Covered by release plan; Java 17 upgrade deferred to after 1.3 |
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

