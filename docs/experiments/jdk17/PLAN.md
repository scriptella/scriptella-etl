# Experiment: JDK 17 Build and Runtime Compatibility

**Issue:** [#31](https://github.com/scriptella/scriptella-etl/issues/31)  
**Branch:** `exp-jdk17`  
**Status:** In progress — plan revised for technical scope only

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
| Maven | record with `mvn -v` |
| Ant | 1.10.17 (workspace `apache-ant-1.10.17/` if used) |
| DTDDoc | workspace `DTDDoc/` if used for full `ant dist` |
| Branch base | `master` at start of experiment |

```bash
export JAVA_HOME=/Users/pvr/Library/Java/JavaVirtualMachines/temurin-17.0.15/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -v
```

---

## 4. Preliminary findings

Recorded before production code changes. These are **not** certification of full JDK 17 support.

### Commands already run

| Command | JDK | Result |
|---------|-----|--------|
| `mvn clean test` (reactor) | 8 | PASS |
| `mvn -pl core test` | 17 | PASS |
| `mvn -pl tools -am test` | 17 | PASS |
| `mvn -pl drivers test` (full module) | 17 | FAIL — 141 tests, **9 errors**, all in `scriptella.driver.script.*` |
| `mvn clean verify` | 17 | Not yet re-run on this branch (expected fail on drivers) |
| `mvn clean deploy -Dcentral.skipPublishing=true` | 17 | Not yet run |
| `ant clean test` / `ant jar` / `ant dist` | 17 | Not yet run |
| Packaged `java -jar …` JavaScript smoke | 17 | Not yet run |
| JDK 8 regression after candidate fixes | 8 | Not yet run |

### First concrete blocker: JavaScript engine name resolution

```text
scriptella.configuration.ConfigurationException:
Specified language=js not supported.
Available values are: [[JEXL, Jexl, jexl], [rhino-nonjdk, rhino]]
```

Failing tests observed so far:

* `ScriptConnectionTest` (4)
* `ScriptConnectionPerfTest` (2)
* `ScriptDriverITest` (2)
* `ScriptingQueryITest` (1)

Mechanism:

* On JDK 8, default `language=js` resolves to **Nashorn** (SPI names include `js`, `JavaScript`, `ECMAScript`, …).
* On JDK 17, Nashorn is gone.
* Bundled Rhino (`cat.inspiracio:rhino-js-engine:1.7.10` + Rhino 1.7.10) **executes correctly** under SPI names **`rhino`** and **`rhino-nonjdk` only**.
* `ScriptConnection` defaults to `js` and does not fall back when that name is missing.

| Engine name | JDK 8 + Rhino JARs | JDK 17 + Rhino JARs |
|-------------|--------------------|---------------------|
| `js` / `JavaScript` | Nashorn | **null** |
| `rhino` | Rhino | Rhino (`eval` OK) |

### Scope of preliminary module results

Preliminary module tests found **no failures outside the script-driver tests exercised so far**. Full reactor, deploy-skip packaging, mail scenarios, Ant packaging, and **packaged-runtime** validation remain **pending**. Do not treat the core/tools passes as full certification.

### Packaging note (why Maven green ≠ distribution green)

Maven can resolve `rhino-js-engine` on the test classpath. That does **not** prove that the Ant-built `scriptella.jar` or distribution ZIPs include Rhino where the launcher can load it on JDK 17. The top-level `build.xml` treats bundled libraries selectively; packaged-runtime smoke is required.

---

## 5. Phase 1 — Reproduce and freeze baseline

Run under JDK 17 and record exact exit codes in the **Command log**.

- [ ] `mvn clean verify`
- [ ] Capture surefire detail for `scriptella.driver.script.*`
- [ ] Optionally re-confirm ScriptEngine SPI names (Rhino probe) for the log
- [ ] Note status of Janino and mail-related tests already included in the reactor (no deep dive yet)

**Do not** expand into dependency upgrades in this phase.  
**Do not** block this phase on Ant or DTDDoc.

---

## 6. Phase 2 — Minimal JavaScript compatibility change

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
| preliminary | `mvn -pl drivers test` | FAIL 9 errors | `language=js` vs Rhino SPI names only |
| | `mvn clean verify` | | |
| | `mvn clean deploy -Dcentral.skipPublishing=true` | | |
| | `ant clean test` | | |
| | `ant jar` | | |
| | `ant dist` | | |
| | `java -jar build/scriptella.jar …` (JS ETL) | | |

### JDK 8 regression

| When | Command | Result | Notes |
|------|---------|--------|-------|
| | `mvn clean verify` | | |
| | class-file major version spot-check | | |
| | packaged JS smoke if artifacts shared | | |

---

## 11. Findings log

### F1 — Default `js` depends on Nashorn; Rhino available under other names

* **Impact:** blocks green Maven drivers suite on JDK 17  
* **Scope observed so far:** script driver tests only  
* **Direction:** fixed alias list → try `rhino` only after primary lookup fails  
* **JDK 8:** primary lookup still hits Nashorn for `js`; fallback unused for those names

### F2 — Module tests outside script package not yet fully certified

* Core/tools module tests passed under JDK 17 in a preliminary run  
* Full reactor, deploy-skip, mail, Ant, and packaged-runtime remain pending

### F3 — (open) Ant packaging / Javadoc tooling / Rhino inclusion in all-in-one JAR

### F4 — (open) Java 8 regression after candidate code changes

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
