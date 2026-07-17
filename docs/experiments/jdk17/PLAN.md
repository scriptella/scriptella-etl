# Experiment: JDK 17 Build and Runtime Compatibility

**Issue:** [#31](https://github.com/scriptella/scriptella-etl/issues/31)  
**Branch:** `exp-jdk17`  
**Status:** In progress — plan and baseline established  
**Parent context:** Release 1.3 closed JDK 17 work for RC2; this branch is groundwork for a merge/defer decision (RC2 vs 1.4).

## Goal

Determine the **smallest practical set of changes** required to compile, test, package, and run Scriptella on **JDK 17**, while preferring to **preserve Java 8 source and bytecode**.

This is an experiment, not a commitment to merge into 1.3 RC2.

## Decision rule (from #31)

Merge into RC2 only if:

* work fits roughly **one or two focused implementation chunks**, and
* no broad dependency or architecture migration is required.

Otherwise: keep `exp-jdk17` as groundwork for **Scriptella 1.4** and close #31 with that recommendation.

## Preferred outcome

* Java 8 remains the source and bytecode baseline (`source`/`target` 1.8).
* Scriptella builds, tests, and runs on JDK 17 (classpath model; no module-info rewrite).
* Existing ETL behavior is unchanged where possible.
* JavaScript defaults and common aliases (`js`, `JavaScript`, …) still resolve (via Rhino when Nashorn is absent).
* Maven remains the primary path; Ant packaging remains valid after Maven is green.

## Non-goals

* Shell driver (#32) — already available; out of scope for this investigation.
* Broad dependency upgrades (Spring, Jakarta Mail, H2/HSQLDB modernization, etc.) unless a JDK 17 blocker forces them.
* Raising the minimum supported Java version.
* Full Java 21/24 certification (note residual risk only).

---

## Environment (record as you run)

| Item | Value |
|------|--------|
| Platform | macOS (x86_64) — update if different |
| JDK 17 | Eclipse Temurin 17.0.15 — `/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home` |
| JDK 8 (regression) | Eclipse Temurin 1.8.0_492 — `/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home` |
| Maven | record with `mvn -v` |
| Ant | 1.10.17 (workspace `apache-ant-1.10.17/` if used) |
| DTDDoc | workspace `DTDDoc/` if used for `ant dist` |
| Branch base | `master` @ start of experiment |

Export for commands:

```bash
export JAVA_HOME=/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

---

## Preliminary baseline (pre-fix probe)

Recorded before production code changes on this branch. Useful so the experiment does not rediscover the same facts.

### Commands already run

| Command | JDK | Result |
|---------|-----|--------|
| `mvn clean test` (reactor) | 8 | **PASS** |
| `mvn -pl core test` | 17 | **PASS** |
| `mvn -pl tools -am test` | 17 | **PASS** |
| `mvn -pl drivers test` (full module) | 17 | **FAIL** — 141 tests, **9 errors**, all in `scriptella.driver.script.*` |
| `mvn clean verify` (ticket initial) | 17 | **Not yet re-run on branch** (expected fail on drivers) |
| `mvn clean deploy -Dcentral.skipPublishing=true` | 17 | **Not yet run** |
| `ant clean test` / `ant dist` | 17 | **Not yet run** |

### Failure detail (script driver only)

```text
scriptella.configuration.ConfigurationException:
Specified language=js not supported.
Available values are: [[JEXL, Jexl, jexl], [rhino-nonjdk, rhino]]
```

Failing tests:

* `ScriptConnectionTest` (4)
* `ScriptConnectionPerfTest` (2)
* `ScriptDriverITest` (2)
* `ScriptingQueryITest` (1)

Root cause:

* On JDK 8, default `language=js` resolves to **Nashorn**.
* On JDK 17, Nashorn is gone.
* Bundled Rhino (`cat.inspiracio:rhino-js-engine:1.7.10` + Rhino 1.7.10) **works**, but SPI names are only **`rhino`** / **`rhino-nonjdk`**, not `js` / `JavaScript`.
* `ScriptConnection` defaults to `js` and does not fall back to Rhino.

Probe summary:

| Engine name | JDK 8 + Rhino JARs | JDK 17 + Rhino JARs |
|-------------|--------------------|---------------------|
| `js` / `JavaScript` | Nashorn | **null** |
| `rhino` | Rhino | Rhino (eval OK) |

Non-script areas that already looked healthy under Maven tests on JDK 17: core ETL/JDBC, Janino, Spring 1.2 path, CSV/text, tools launcher.

---

## Work plan

### Phase 0 — Branch and tracking  ✅

- [x] Create `exp-jdk17` from current `master`
- [x] Add this plan document
- [ ] Push `exp-jdk17` to `origin`
- [ ] Comment on #31 with branch link and plan path

### Phase 1 — Reproduce and freeze baseline  (diagnostic)

Run and paste exact exit codes into **Command log** below.

- [ ] `JAVA_HOME=…/jdk17 mvn clean verify`
- [ ] Focused surefire reports for `scriptella.driver.script.*`
- [ ] Optional smoke: ScriptEngine SPI names (Rhino probe) for the record
- [ ] Confirm Janino tests green when script tests are excluded or after fix
- [ ] Confirm mail driver unit/integration tests present and status on JDK 17

**Stop condition:** Do not expand into dependency upgrades during this phase.

### Phase 2 — Minimal script-engine fix  (implementation candidate)

Cheapest fix (preferred):

1. In `drivers/.../script/ScriptConnection.java`, when `getEngineByName(lang)` returns null for common JS aliases (`js`, `javascript`, `JavaScript`, `ECMAScript`, case variants as needed), fall back to **`rhino`**.
2. Keep default language presentation as JavaScript/ECMAScript for users.
3. Do **not** raise `source`/`target` above 1.8.
4. Add focused tests for alias resolution that pass on both JDK 8 and 17.

Allowed if needed and still localized:

* Explicit registration of Rhino factory when SPI discovery is incomplete
* Align Ant `lib/` Rhino packaging only if distribution smoke fails (Maven already has the dependency)

**Out of scope unless forced:**

* Replacing Rhino
* Redesigning the scripting SPI
* Module-path / `module-info` work

### Phase 3 — Ticket validation matrix

After Phase 2 (or documenting why Phase 2 is insufficient):

| Check | JDK 17 | JDK 8 (if merge candidate) |
|-------|--------|----------------------------|
| `mvn clean verify` | | |
| `mvn clean deploy -Dcentral.skipPublishing=true` | | |
| JavaScript / Rhino ETL (default `js` + explicit `rhino`) | | |
| Janino compile + generated class load | | |
| Mail driver scenario (or unit tests) | | |
| JDBC representative ETL / existing integration tests | | |
| Command-line launcher (`tools` tests / jar smoke) | | |
| Javadoc attachment / site plugin path used by deploy | | |
| `ant clean test` | | |
| `ant dist` (with DTDDoc if full dist required) | | |
| Bytecode still Java 8 (`javap -verbose` sample class) | | |

### Phase 4 — Decision and publish findings

- [ ] Summarize minimal diffs (file list + intent)
- [ ] List remaining blockers and effort
- [ ] Explicit Java 8 compatibility assessment
- [ ] Recommendation:

  * **Merge bounded changes for 1.3 RC2**, or  
  * **Retain branch as 1.4 groundwork**

- [ ] Push branch (even if incomplete)
- [ ] Close or update #31 with the decision

---

## Candidate production touch points

| Area | Likely file(s) | Notes |
|------|----------------|-------|
| JS alias fallback | `drivers/src/java/scriptella/driver/script/ScriptConnection.java` | Primary fix |
| Tests | `drivers/src/test/scriptella/driver/script/*` | Alias + default language |
| Docs / upgrade notes | `CHANGELOG.md`, maybe README | Only if merge candidate |
| Ant lib consistency | `lib/rhino*.jar`, `lib/versions.properties` | Only if dist diverges from Maven |
| Parent POM | `pom.xml` | Avoid unless javadoc/deploy forced |

Shell driver is **not** in scope.

---

## Effort estimate (hypothesis)

| Track | Estimate | Confidence |
|-------|----------|------------|
| Alias fallback + dual-JDK Maven green | ~0.5–1 day | High (root cause known) |
| + deploy skip-publish + launcher/JDBC/JS/Janino/mail smoke | +0.5 day | Medium |
| + Ant test/dist on JDK 17 | +0.5 day | Medium (untested) |
| Forced broad dep/Javadoc/module work | multi-day+ | Would trigger **defer to 1.4** |

Hypothesis going into implementation: **mergeable for RC2 under the decision rule**, if Phase 3 stays clean after the script-alias fix. Revisit if Ant/Javadoc/deploy uncover unrelated blockers.

---

## Command log

Fill in as the experiment proceeds.

### JDK 17

```bash
export JAVA_HOME=/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -v
```

| When | Command | Result | Notes |
|------|---------|--------|-------|
| baseline | `mvn -pl drivers test` | FAIL 9 errors | language=js vs rhino names |
| | `mvn clean verify` | | |
| | `mvn clean deploy -Dcentral.skipPublishing=true` | | |
| | `ant clean test` | | |
| | `ant -Ddtddoc.dir=… clean dist` | | |

### JDK 8 regression (merge candidate only)

| When | Command | Result | Notes |
|------|---------|--------|-------|
| | `mvn clean verify` | | |
| | `ant clean test` | | |

---

## Findings log

### F1 — JavaScript default alias depends on Nashorn (2026-07-16)

* **Severity:** blocks green Maven suite on JDK 17  
* **Scope:** script driver only  
* **Fix direction:** map JS aliases → Rhino when Nashorn absent  
* **Java 8 impact:** low if fallback is only used when primary name fails (JDK 8 still prefers Nashorn for `js` unless policy changes to prefer Rhino)

### F2 — Core, tools, non-script drivers largely OK on JDK 17

* Preliminary probe: core PASS, tools PASS; Janino/Spring/CSV/text exercised without failure outside script package  
* Re-confirm after Phase 2 with full `mvn clean verify`

### F3 — (open) Ant packaging / Javadoc / Central-skip deploy on JDK 17

* Not yet executed on this branch

---

## Final recommendation

_To be filled at end of experiment._

```text
[ ] Merge bounded changes for 1.3 RC2
[ ] Retain exp-jdk17 as groundwork for Scriptella 1.4
```

**Rationale:** _(fill in)_

**Minimal change set:** _(file list)_

**Java 8 preserved?** _(yes/no + how verified)_

**Remaining work if deferred:** _(bullets)_
