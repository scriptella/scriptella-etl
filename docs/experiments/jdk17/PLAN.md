# Experiment: JDK 17 Build and Runtime Compatibility

**Issue:** [#31](https://github.com/scriptella/scriptella-etl/issues/31)  
**Branch:** `exp-jdk17`  
**Status:** Phases 1–4 complete — technical outcome recorded (partially supported)

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

**Status: complete.**

### Checklist

- [x] `mvn clean verify` — **BUILD SUCCESS**
- [x] `mvn clean deploy -Dcentral.skipPublishing=true` — **BUILD SUCCESS** (Central publishing skipped; artifacts installed to local `~/.m2`)
- [x] Representative **JDBC** — packaged CLI smoke with HSQLDB on connection classpath: **success**
- [x] **JavaScript / Rhino** — Maven tests (default/`js`/`JavaScript`/`rhino`); packaged runtime documented below
- [x] **Janino** — Maven tests green; packaged CLI with Janino on classpath: **success**
- [x] **Mail** — Maven unit tests green (`MaiConnectionTest` 4, `MailDriverTest` 1); no live SMTP end-to-end
- [x] **Command-line launcher** — tools tests green; `java -jar build/scriptella.jar -version` → `1.3-SNAPSHOT`
- [x] **Javadoc (Maven)** — `-javadoc.jar` attached and installed for core/drivers/tools

Maven does **not** produce the all-in-one `scriptella.jar`. That artifact comes from Ant (Phase 4 / below).

### Maven deploy-skip artifacts (local install)

Produced under `~/.m2/repository/org/scriptella/*/1.3-SNAPSHOT/`:

* module JARs, `-sources.jar`, `-javadoc.jar` for core, drivers, tools  
* core also installs `-tests.jar`  
* parent POM  

### Packaged-runtime JavaScript (Ant-built `build/scriptella.jar`)

| Observation | Detail |
|-------------|--------|
| Fat JAR embeds Rhino? | **No.** Only merges `commons-jexl` + `commons-logging` (+ Scriptella classes). SPI service file lists **JEXL only**. |
| Binary ZIP ships Rhino under `lib/`? | **No.** `build.xml` zip only includes `commons-*.jar` (+ module JARs). |
| Rhino available in tree? | Yes: `lib/rhino.jar`, `lib/rhino-js-engine.jar`; `ant jar` also copies them to `samples/lib/`. |
| `java -jar scriptella.jar` alone (JDK 17) | **Fails:** `language=js not supported` (only JEXL registered). Same class of problem as Phase 1, plus no Rhino on launcher classpath. |
| Connection `classpath=` to Rhino JARs | **Does not help** for JS: `ScriptConnection` builds `ScriptEngineManager` with `ScriptConnection.class.getClassLoader()`, not the connection driver classloader. |
| `-Xbootclasspath/a:rhino… -jar scriptella.jar` | **Fails** on JDK 17: `NoClassDefFoundError: javax/script/ScriptEngineFactory` when Rhino factory loads from boot path (module/boot-loader mismatch). |
| Working packaged pattern | `java -cp build/scriptella.jar:lib/rhino.jar:lib/rhino-js-engine.jar scriptella.tools.launcher.EtlLauncher <etl>` — **success** for default/`js`, `JavaScript`, and `rhino`. |

**Implication:** On JDK 8, pure `java -jar` could still run JS via **Nashorn**. On JDK 17, users need **Rhino on the application classpath** (not only in `lib/` next to an ignored `-jar` layout) unless Rhino is later embedded in the fat JAR or dist launcher scripts are adjusted. That packaging gap is **separate** from the Phase 2 alias fix.

### Other packaged smokes (JDK 17)

| Scenario | Result |
|----------|--------|
| JDBC ETL via `java -jar` + HSQLDB on connection classpath | success |
| Janino via `java -cp scriptella.jar:janino…` or `-Xbootclasspath/a:janino… -jar` | success (`janino-ok`) |
| Class-file major of `EtlLauncher` in fat JAR | **52** (Java 8) |
| Mail live SMTP | not exercised |

---

## 8. Phase 4 — Ant / package validation (JDK 17)

**Status: complete.** Ant 1.10.17; `JAVA_HOME` = Temurin 17.0.15; DTDDoc at `/Users/pvr/dev/prj/scriptella/DTDDoc`.

```bash
ant clean test
ant jar
ant -Ddtddoc.dir=/Users/pvr/dev/prj/scriptella/DTDDoc clean dist
```

| Command | Outcome label | Notes |
|---------|---------------|-------|
| `ant clean test` | **Supported** | **308** tests, 0 failures, 0 errors (`BUILD SUCCESSFUL`) |
| `ant jar` | **Supported** | Produces `build/scriptella.jar` (+ module JARs); copies selected `lib/*` including Rhino into `samples/lib` |
| `ant dist` (with DTDDoc) | **Supported with prerequisites** (+ docs caveat) | Requires `dtddoc.dir`. DTDDoc 1.1.0 **succeeds**. Binary/src/examples ZIPs produced. **Ant Javadoc** step reports `error: No source files for package scriptella.driver` (JDK 17 `javadoc`); `docs/api` in the binary ZIP is **empty**. Dist still exits **BUILD SUCCESSFUL**. |

### Rhino in Ant packaging (confirmed)

| Artifact | Rhino included? |
|----------|-----------------|
| `build/scriptella.jar` (all-in-one) | **No** |
| Binary `scriptella-*-SNAPSHOT.zip` `lib/` | **No** (commons only + module JARs) |
| `samples/lib` after `ant jar` | **Yes** (rhino + rhino-js-engine) |
| Examples ZIP | includes samples tree (thus can include samples/lib when present) |

### Ant Javadoc note

`build-docs.xml` uses `packageset` over `drivers/src/java`, which includes empty parent package `scriptella.driver`. JDK 17 Javadoc treats “No source files for package …” as a hard error; the Ant task still allowed the dist target to complete with empty API docs. **Maven** Javadoc jars on JDK 17 were fine.

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
| Phase 3 | `mvn clean verify` | **BUILD SUCCESS** | reconfirm |
| Phase 3 | `mvn clean deploy -Dcentral.skipPublishing=true` | **BUILD SUCCESS** | local install + javadoc jars; Central skipped |
| Phase 3/4 | `ant jar` | **BUILD SUCCESSFUL** | fat JAR without Rhino |
| Phase 3 | `java -jar build/scriptella.jar` JS ETL alone | **fail** | no Rhino on launcher CP |
| Phase 3 | `java -cp scriptella.jar:rhino… EtlLauncher` JS ETL | **success** | default / JavaScript / rhino |
| Phase 3 | JDBC / Janino packaged smokes | **success** | see Phase 3 |
| Phase 4 | `ant clean test` | **BUILD SUCCESSFUL** | 308 tests, 0 fail/err |
| Phase 4 | `ant -Ddtddoc.dir=… clean dist` | **BUILD SUCCESSFUL** | DTDDoc OK; Ant Javadoc API empty (error logged) |

### JDK 8 regression

| When | Command | Result | Notes |
|------|---------|--------|-------|
| pre-Phase 1 (historical) | `mvn clean test` | PASS | full reactor before experiment fixes |
| Phase 2 | `mvn clean verify` | **BUILD SUCCESS** | core 149, drivers 147, tools 12 (after one flaky JMX cancel run) |
| Phase 1/3 | class-file major (JDK 17-built) | major **52** | core class + fat-jar `EtlLauncher` |
| | packaged JS on JDK 8 without Rhino | | still expected to use Nashorn with pure `-jar` (not re-run in Phase 3) |

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

### F4 — Packaged all-in-one JAR does not embed Rhino; binary ZIP omits Rhino from `lib/`

* Pure `java -jar scriptella.jar` JS fails on JDK 17 (Nashorn gone + no Rhino on classpath)
* Working pattern: put Rhino JARs on the **application** classpath (not connection classpath alone; not `-Xbootclasspath/a` with `-jar` on JDK 17)
* Historical docs that recommend `-Xbootclasspath/a` with `-jar` are **unsafe for Rhino on JDK 17**

### F5 — Ant Javadoc on JDK 17 fails for empty `scriptella.driver` package; dist still succeeds with empty `docs/api`

* DTDDoc path works with `dtddoc.dir`
* Maven Javadoc attachment on JDK 17 works

### F6 — Java 8 regression after Phase 2 change set

* Full `mvn clean verify` on Temurin 8: SUCCESS
* Representative class-file major version 52 on JDK 17-built artifacts

---

## 12. Technical outcome and remaining limitations

```text
JDK 17 status: partially supported

Validated:
- [x] Maven build and tests (JDK 17 + JDK 8 after Phase 2)
- [x] Maven artifact generation (deploy -Dcentral.skipPublishing=true)
- [x] CLI runtime (launcher -version, JDBC ETL via -jar, Janino via -cp/-jar+boot)
- [x] JavaScript driver (Maven classpath) — alias fallback
- [~] JavaScript driver (packaged scriptella.jar) — works only with Rhino on app classpath; pure -jar fails
- [x] Janino driver (Maven + packaged)
- [x] Mail driver (Maven unit tests; no live SMTP)
- [x] Ant tests (308 / 0 / 0)
- [x] Ant packaged JAR (builds; Rhino not embedded)
- [x] Ant distribution (builds with DTDDoc; API javadocs empty under Ant on JDK 17)
- [x] Java 8 regression (real JDK 8 mvn clean verify + major version 52)

Minimal changes:
- ScriptConnection: fixed JS alias list → Rhino fallback after primary SPI lookup fails
- ScriptConnectionTest: default/js/JavaScript/rhino/invalid/alias-set coverage
- docs/experiments/jdk17/PLAN.md: experiment record

Remaining limitations:
- All-in-one JAR / binary ZIP do not ship Rhino; pure java -jar JS broken on JDK 17
- connection classpath does not feed ScriptEngineManager for the script driver
- -Xbootclasspath/a + -jar unsuitable for Rhino SPI on JDK 17
- Ant Javadoc (docs/api) empty on JDK 17 due to empty package scriptella.driver
- Mail: no live SMTP validation in this experiment
- Binary dist lib/ layout still omits rhino even though samples/lib receives it

Java 8 compatibility:
- preserved (Maven suite green on JDK 8; bytecode major 52; Nashorn still preferred when present)

Follow-up technical issues:
- Consider embedding Rhino (+ SPI) in scriptella.jar and/or shipping rhino*.jar in binary dist lib/
- Adjust launcher docs: prefer -cp over -jar when extra engines are required; fix bootclasspath guidance for modern JDKs
- Optionally teach ScriptConnection/ScriptEngineManager to use the connection ClassLoader for SPI discovery
- Fix Ant build-docs.xml / packageset so JDK 17 Javadoc does not fail on empty scriptella.driver
```

### Working hypothesis (updated)

The Phase 1 Maven failure **was** localized to JavaScript engine **alias** resolution and is fixed. Full validation shows an additional **packaging/classpath** gap: Rhino is a compile/test dependency and sits under `lib/`, but the all-in-one JAR and binary ZIP do not put it where a pure `java -jar` launcher can load engines on JDK 17. That is a separate technical follow-up from the alias change.
