# Experiment: JDK 17 Build and Runtime Compatibility

**Issue:** [#31](https://github.com/scriptella/scriptella-etl/issues/31)  
**Branch:** `exp-jdk17`  
**Status:** Phase 2 complete — JS alias → Rhino fallback; Maven green on JDK 17 and JDK 8

## 1. Goal and scope

This branch investigates the **smallest practical set of changes** required to **build, test, package, and run** Scriptella on **JDK 17**.

**Release assignment is outside the scope of this experiment.** This ticket produces a technical JDK 17 result only; whether and when that result is adopted in a release is decided elsewhere.

### Meanings of “JDK 17 support” (define and validate separately)

| Meaning | What success looks like |
|---------|-------------------------|
| **Build with JDK 17** | Maven reactor compiles and tests under a JDK 17 `JAVA_HOME` |
| **Run on JDK 17** | CLI and drivers execute under a JDK 17 runtime |
| **Generate artifacts with JDK 17** | Normal Maven packaging/deploy-skip and (separately) Ant packaging produce usable outputs |
| **Preserve Java 8 where practical** | Source/target remain 1.8; artifacts still run on a real JDK 8; representative class-file major versions stay Java 8 |
| **Ant packaging path** | Recorded as its own result: tests, all-in-one JAR, and distributions — distinct from ordinary Maven/runtime compatibility |

Do **not** treat any of the above as making JDK 17 the **minimum** runtime. Prefer keeping Java 8 as the supported baseline bytecode and runtime target unless a blocker forces a different conclusion (which would be reported as a limitation, not a release decision).

### Primary vs secondary validation order

1. **Maven first** — reactor build, tests, artifact generation.  
2. **Runtime smoke** on JDK 17, including packaged JAR if available.  
3. **Ant path** after Maven is understood — required to **record**, not required to block the initial JavaScript investigation.  
4. **Java 8 regression** once a candidate change set exists.

---

## 2. Non-goals

* Deciding which release receives JDK 17 work.
* Shell driver (#32) — already available; out of scope here.
* Broad dependency upgrades (Spring, Jakarta Mail, H2/HSQLDB modernization, etc.) unless a concrete JDK 17 blocker forces a minimal dependency change.
* Raising the minimum supported Java version as a product policy change.
* Full Java 21/24 certification (mention only if encountered as residual risk).
* Replacing Rhino or redesigning the scripting SPI unless the minimal alias fix is insufficient.
* Module-path / `module-info` migration.

---

## 3. Environment

| Item | Value |
|------|--------|
| Platform | macOS (x86_64) — update if different |
| JDK 17 | Eclipse Temurin 17.0.15 — `/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home` |
| JDK 8 (regression) | Eclipse Temurin 1.8.0_492 — `/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home` |
| Maven | **3.9.9** (IntelliJ bundled) — Java version reported by Maven: 17.0.15 |
| Ant | 1.10.17 (workspace `apache-ant-1.10.17/` if used) — not used in Phase 1 |
| DTDDoc | workspace `DTDDoc/` if used for full `ant dist` — not used in Phase 1 |
| Branch base | `master` at start of experiment; Phase 1 run on `exp-jdk17` |
| Platform detail | macOS 15.6.1, x86_64 |

```bash
export JAVA_HOME=/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -v
```

---

## 4. Baseline findings (Phase 1 freeze)

Recorded on `exp-jdk17` with **no production code changes**. These are **not** certification of full JDK 17 support.

### Environment used for freeze

```text
JAVA_HOME=/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home
java -version  → openjdk version "17.0.15" 2025-04-15 (Temurin-17.0.15+6)
mvn -v         → Apache Maven 3.9.9; Java version 17.0.15; OS macOS 15.6.1 x86_64
```

### Commands run

| Command | JDK | Exit / result |
|---------|-----|----------------|
| `mvn clean verify` | 17 | **BUILD FAILURE** (Maven reactor exit non-zero; drivers surefire failed) |
| `mvn -pl drivers test` | 17 | **exit 1** — confirms drivers-only failure |
| ScriptEngine SPI probe (Rhino 1.7.10 + rhino-js-engine 1.7.10 on classpath) | 17 | Rhino factory present; JS aliases null |
| `mvn clean test` (reactor) | 8 | PASS (earlier session; not re-run in Phase 1) |
| `mvn clean deploy -Dcentral.skipPublishing=true` | 17 | **Not run** (blocked until tests green or explicitly skipped) |
| `ant clean test` / `ant jar` / `ant dist` | 17 | **Not run** (Phase 4) |
| Packaged `java -jar …` JavaScript smoke | 17 | **Not run** (Phase 3/4) |
| JDK 8 regression after candidate fixes | 8 | **Not run** |

### Reactor summary (`mvn clean verify` on JDK 17)

| Module | Result | Tests |
|--------|--------|-------|
| `scriptella-parent` | SUCCESS | — |
| `scriptella-core` | SUCCESS | **149** run, 0 failures, 0 errors |
| `scriptella-drivers` | **FAILURE** | **141** run, 0 failures, **9 errors** |
| `scriptella-tools` | **SKIPPED** | (reactor stopped after drivers) |

Total time ≈ 18s. Finished at 2026-07-16T17:20:36-07:00 (local).

Bytecode note (incidental): a class under `core/target/classes` compiled during this JDK 17 build reports **major version 52** (Java 8), consistent with `source`/`target` 1.8. This is not a full preservation proof (no JDK 8 run in Phase 1).

### First concrete blocker: JavaScript engine name resolution

All 9 errors share the same root:

```text
scriptella.configuration.ConfigurationException:
Specified language=js not supported. Available values are: [[JEXL, Jexl, jexl], [rhino-nonjdk, rhino]]
```

or the ETL-wrapped form:

```text
scriptella.execution.EtlExecutorException:
Specified language=JavaScript not supported. Available values are: [[JEXL, Jexl, jexl], [rhino-nonjdk, rhino]]
```

Stack: `ScriptConnection.<init>` line 92 (`getEngineByName` returned null → `ConfigurationException`).

| Test class | Errors | Requested language in failure |
|------------|--------|-------------------------------|
| `ScriptConnectionTest` | 4 of 5 | default / `js` |
| `ScriptConnectionPerfTest` | 2 of 2 | default / `js` |
| `ScriptDriverITest` | 2 of 2 | **`JavaScript`** (ETL config) |
| `ScriptingQueryITest` | 1 of 1 | **`js`** (ETL config) |
| `MissingQueryNextCallDetectorTest` | 0 | (no engine) |
| `ParametersCallbackMapTest` | 0 | (no engine) |

Mechanism:

* On JDK 8, default `language=js` resolves to **Nashorn** (SPI names include `js`, `JavaScript`, `ECMAScript`, …).
* On JDK 17, Nashorn is gone.
* Bundled Rhino (`cat.inspiracio:rhino-js-engine:1.7.10` + Rhino 1.7.10) is on the Maven test classpath and **loads**, but SPI names are only **`rhino`** / **`rhino-nonjdk`**.
* `ScriptConnection` defaults empty language to `js` and does not fall back when that name is missing.

### ScriptEngine SPI probe (JDK 17 + Rhino JARs)

```text
factories=1
class=com.sun.phobos.script.javascript.RhinoScriptEngineFactory
  engine=Mozilla Rhino lang=ECMAScript names=[rhino-nonjdk, rhino]

getEngineByName(js)          => null
getEngineByName(JS)          => null
getEngineByName(javascript)  => null
getEngineByName(JavaScript)  => null
getEngineByName(ecmascript)  => null
getEngineByName(ECMAScript)  => null
getEngineByName(rhino)       => com.sun.phobos.script.javascript.RhinoScriptEngine
getEngineByName(rhino-nonjdk)=> com.sun.phobos.script.javascript.RhinoScriptEngine
getEngineByName(python)      => null
```

JEXL appears in Scriptella’s “available values” list via its own registration path during driver tests; it is not a substitute for JavaScript.

### Janino and mail (reactor status only — no deep dive)

Observed as **passing** within the failed drivers module run (errors only in script package):

| Area | Tests observed | Result |
|------|----------------|--------|
| Janino | `JaninoBaseClassesTest` (2), `JaninoConnectionTest` (3), `JaninoGetNativeDbConnectionITest` (1), `JaninoPerfTest` (2) | all 0 failures / 0 errors |
| Mail (`javax.mail`) | `MaiConnectionTest` (4), `MailDriverTest` (1) | all 0 failures / 0 errors |

Other drivers that ran without errors in the same module include h2, hsqldb, csv, text, xpath, jexl, velocity, jndi, auto, spring, shell (shell out of investigation scope). **Tools module was not executed** because the reactor failed on drivers.

### What Phase 1 does *not* certify

* Full reactor green on JDK 17  
* Maven deploy-skip packaging  
* Tools / CLI launcher tests  
* Packaged `scriptella.jar` JavaScript runtime  
* Ant test / jar / dist  
* Real JDK 8 regression after any fix  

### Packaging note (why Maven green ≠ distribution green)

Maven can resolve `rhino-js-engine` on the test classpath. That does **not** prove that the Ant-built `scriptella.jar` or distribution ZIPs include Rhino where the launcher can load it on JDK 17. The top-level `build.xml` treats bundled libraries selectively; packaged-runtime smoke remains Phase 3/4 work.

---

## 5. Phase 1 — Reproduce and freeze baseline

**Status: complete** (2026-07-16). No production code changes. No dependency upgrades. Ant not run.

- [x] `mvn clean verify` under JDK 17
- [x] Capture surefire detail for `scriptella.driver.script.*`
- [x] Re-confirm ScriptEngine SPI names (Rhino probe) for the log
- [x] Note status of Janino and mail-related tests already included in the reactor (no deep dive)

**Frozen conclusion for Phase 2:** the only Maven test failures under full `mvn clean verify` on JDK 17 are the nine script-driver errors above; they are explained by missing Nashorn aliases and Rhino registering only as `rhino` / `rhino-nonjdk`.

---

## 6. Phase 2 — Minimal JavaScript compatibility change

**Status: complete** (implementation on `exp-jdk17`).

### Implementation

| File | Change |
|------|--------|
| `drivers/src/java/scriptella/driver/script/ScriptConnection.java` | `resolveScriptEngine`: primary SPI lookup, then fixed JS-alias → `rhino` fallback |
| `drivers/src/test/scriptella/driver/script/ScriptConnectionTest.java` | Default/`js`/`JavaScript`/`rhino`/invalid language + fixed alias-set tests |

No new dependencies or JARs. No change to `source`/`target` 1.8.

### Phase 2 validation

| Command | JDK | Result |
|---------|-----|--------|
| `mvn clean verify` | 17 | **BUILD SUCCESS** (core 149, drivers 147, tools 12) |
| `mvn clean verify` | 8 | **BUILD SUCCESS** (core 149, drivers 147, tools 12) |

Note: one intermediate JDK 8 full run hit flaky JMX/cancellation pollution in `CancellationTest` / `JmxEtlManagerITest` (core, unrelated to script driver). Immediate re-run of full `mvn clean verify` on JDK 8 was green.

### Behavioral contract

Implement only what is needed for common JavaScript usage without Nashorn:

1. **Honor the requested engine name first** via normal `ScriptEngineManager` lookup.
2. **Only if** lookup fails **and** the requested name is a **common JavaScript alias**, try **`rhino`**.
3. **`language="rhino"`** (and existing Rhino SPI names such as `rhino-nonjdk` if requested explicitly) must continue to work via normal lookup.
4. **Unknown non-JavaScript** language names must still fail with `ConfigurationException`.
5. On **JDK 8**, `js` (and other aliases still provided by Nashorn) must continue to select **Nashorn** — the fallback must not run when the primary name already resolves. Do not intentionally change that policy in this experiment.

### Aliases eligible for Rhino fallback

Use a fixed set aligned with historical Nashorn / public JS naming and current defaults (do not invent open-ended “case variants as needed”):

| Requested name (exact, as given by connection property after empty-default) | Fallback when lookup fails |
|----------------------------------------------------------------------------|----------------------------|
| *(empty / omitted)* → treated as `js` (existing default) | `rhino` |
| `js` | `rhino` |
| `JS` | `rhino` |
| `javascript` | `rhino` |
| `JavaScript` | `rhino` |
| `ecmascript` | `rhino` |
| `ECMAScript` | `rhino` |

Names **not** in this set must **not** fall back (e.g. `nusuchlanguage`, `python`, typos).  
Do **not** map arbitrary strings case-insensitively beyond the rows above.

Implementation home (expected):  
`drivers/src/java/scriptella/driver/script/ScriptConnection.java`

Keep:

```xml
<source>1.8</source>
<target>1.8</target>
```

unless a later phase proves that is insufficient (then record as a limitation; consider `--release 8` only if it can be introduced without disrupting the JDK 8 build path).

### Focused engine-compatibility tests (required)

Add or extend tests so the **fallback itself** is regression-covered. Prefer extending existing surfaces (`ScriptConnectionTest`, `ScriptDriverITest`, `ScriptingQueryITest`) rather than a large new suite.

Required coverage:

| Case | Expectation |
|------|-------------|
| Omitted language | Defaults successfully (resolves an engine) |
| `language="js"` | Succeeds on JDK 17 (via Rhino fallback when Nashorn absent) |
| `language="JavaScript"` | Succeeds (alias in the fixed table) |
| `language="rhino"` | Succeeds (direct SPI name) |
| Unrelated invalid engine | Still throws `ConfigurationException` |
| Dialect reporting | Remains sensible (e.g. language name still meaningful for JS/Rhino) |
| Existing behavioral surface | Bindings, Java method calls, query callbacks, numeric results, `print`, and error propagation still work (covered primarily by existing tests once default/`js` resolve again) |

Where dual-JDK assertions are awkward in one process, document that full proof is: **same suite green on JDK 8 and JDK 17**.

---

## 7. Phase 3 — Maven and runtime validation (JDK 17)

After Phase 2 (or after documenting why it is insufficient):

- [ ] `mvn clean verify`
- [ ] `mvn clean deploy -Dcentral.skipPublishing=true`
- [ ] Representative **JDBC** path (existing integration tests or sample)
- [ ] **JavaScript / Rhino** (default `js`, explicit `JavaScript`, explicit `rhino`)
- [ ] **Janino** compilation and generated class loading (existing tests + any focused smoke)
- [ ] **Mail** driver using explicit `javax.mail` (tests and/or sample; record gaps if SMTP not available)
- [ ] **Command-line launcher** scenarios already covered by tools tests, plus any needed manual smoke
- [ ] Javadoc / attached javadoc artifacts as produced by the normal Maven packaging path used above

### Packaged-runtime JavaScript check (required)

Maven test success is not enough. After an artifact that contains the launcher is available (Maven shaded/all-in-one if produced, or Ant `jar` once built in Phase 4):

```bash
java -jar build/scriptella.jar <javascript-etl-file>
```

Also:

- [ ] Inspect the distribution / all-in-one layout for Rhino (and JSR-223 adapter) JARs or embedding where the launcher can load them on JDK 17.
- [ ] Record whether JS works from the **packaged** artifact without relying on the Maven reactor classpath.

If Maven does not produce the same all-in-one JAR as Ant, note that and complete this check against the Ant-built JAR in Phase 4.

---

## 8. Phase 4 — Ant / package validation (JDK 17)

Run and record even if outcomes differ from Maven. Ant must not block Phase 1–2 diagnosis, but the **final experiment result must include Ant outcomes**.

```bash
ant clean test
ant jar
ant dist   # with DTDDoc prerequisites documented if required
```

Classify each Ant result separately:

| Outcome label | Meaning |
|---------------|---------|
| **Supported** | Works under documented JDK 17 + Ant environment |
| **Supported with prerequisites** | Works only with external tools/paths (e.g. DTDDoc) |
| **Blocked by legacy documentation tooling** | Runtime/JAR OK but docs/dist step fails for DTDDoc/Javadoc tooling reasons |
| **Not required for ordinary runtime compatibility** | Dist/docs gap does not prevent running Scriptella from jar/classes on JDK 17 |

Also record:

- [ ] Contents of Ant-built `scriptella.jar` / ZIP regarding Rhino
- [ ] `java -jar build/scriptella.jar <javascript-etl-file>` if not already done in Phase 3

---

## 9. Java 8 preservation (when a change set exists)

`source`/`target` 1.8 only controls class-file **syntax level**. Code compiled on JDK 17 can still call APIs introduced after Java 8 unless care is taken.

Require:

- [ ] **Run** the resulting artifacts / full `mvn clean verify` on a **real JDK 8**
- [ ] Check **representative class-file major versions** (e.g. `javap -verbose` on a few produced classes; major 52 = Java 8)
- [ ] Consider **`--release 8`** only if it can be introduced without disrupting the existing JDK 8 build path; optional, not the first lever

For this experiment, an actual **JDK 8 regression run is the primary proof** of preserved Java 8 compatibility.

---

## 10. Command log

### JDK 17

| When | Command | Result | Notes |
|------|---------|--------|-------|
| Phase 1 | `mvn clean verify` | **BUILD FAILURE** | parent SUCCESS; core SUCCESS (149 tests); drivers FAILURE (141 tests, 9 errors); tools SKIPPED |
| Phase 1 | `mvn -pl drivers test` | **exit 1** | same 9 script errors |
| Phase 1 | Rhino SPI probe (standalone) | Rhino only as `rhino`/`rhino-nonjdk` | JS aliases null |
| Phase 2 | `mvn clean verify` | **BUILD SUCCESS** | core 149, drivers 147 (+alias tests), tools 12 |
| | `mvn clean deploy -Dcentral.skipPublishing=true` | | Phase 3 |
| | `ant clean test` | | Phase 4 |
| | `ant jar` | | Phase 4 |
| | `ant dist` | | Phase 4 |
| | `java -jar build/scriptella.jar …` (JS ETL) | | Phase 3/4 |

### JDK 8 regression

| When | Command | Result | Notes |
|------|---------|--------|-------|
| pre-Phase 1 (historical) | `mvn clean test` | PASS | full reactor before experiment fixes |
| Phase 2 | `mvn clean verify` | **BUILD SUCCESS** | core 149, drivers 147, tools 12 (after one flaky JMX cancel run) |
| Phase 1 incidental | `javap` major version on JDK 17-built core class | major 52 | syntax level only; not a runtime proof |
| | packaged JS smoke if artifacts shared | | |

---

## 11. Findings log

### F1 — Default `js` / `JavaScript` depend on Nashorn; Rhino available under other names (**addressed in Phase 2**)

* **Impact (Phase 1):** blocked green Maven reactor on JDK 17  
* **Fix:** `ScriptConnection.resolveScriptEngine` — primary SPI lookup, then fixed JS aliases → `rhino`  
* **Validation:** `mvn clean verify` SUCCESS on JDK 17 and JDK 8  
* **JDK 8:** primary lookup still hits Nashorn for `js` when present; fallback only if primary returns null

### F2 — Janino and mail tests passed inside the failed drivers module run

* Janino: 8 tests across 4 classes, 0 failures/errors  
* Mail: 5 tests across 2 classes, 0 failures/errors  
* Not a substitute for packaged-runtime or SMTP end-to-end smoke

### F3 — Tools module not executed on JDK 17 in Phase 1

* Reactor skipped `scriptella-tools` after drivers failure  
* CLI launcher validation remains Phase 3

### F4 — (open) Ant packaging / Javadoc tooling / Rhino inclusion in all-in-one JAR

### F5 — (open) Java 8 regression after candidate code changes

---

## 12. Technical outcome and remaining limitations

_Fill at end of experiment. No release-placement recommendation._

```text
JDK 17 status: supported / partially supported / blocked

Validated:
- [ ] Maven build and tests
- [ ] Maven artifact generation (including deploy -Dcentral.skipPublishing=true)
- [ ] CLI runtime
- [ ] JavaScript driver (Maven classpath)
- [ ] JavaScript driver (packaged scriptella.jar)
- [ ] Janino driver
- [ ] Mail driver
- [ ] Ant tests
- [ ] Ant packaged JAR
- [ ] Ant distribution
- [ ] Java 8 regression (real JDK 8 run + class-file major check)

Minimal changes:
- ...

Remaining limitations:
- ...

Java 8 compatibility:
- preserved / not preserved / not fully verified

Follow-up technical issues:
- ...
```

### Working hypothesis (not an effort estimate)

Initial hypothesis: the known Maven test failure appears **localized to JavaScript engine alias resolution**. This must be **re-evaluated** after full Maven, packaged-runtime, and Ant validation.
