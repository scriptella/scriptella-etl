# Chunk 5A — Dependency Upgrade Impact Analysis

**Status:** Complete (July 25, 2026)

**Scope:** Analysis only. No dependency versions or production packaging
were changed for this chunk. Characterization tests lock behavior that Chunk
5B must preserve.

**Related plan:** [release-1.4-plan.md](release-1.4-plan.md) Chunks 5A and 5B  
**Umbrella:** [#44](https://github.com/scriptella/scriptella-etl/issues/44)

---

## Current baseline inventory

### Maven dependency management (`pom.xml`)

| Coordinate | Version | Modules / scope |
| --- | --- | --- |
| `org.apache.commons:commons-jexl` | 2.0.1 | core compile; drivers compile; tools test |
| `commons-logging:commons-logging` | 1.0.4 | core/drivers/tools test; **embedded** into `scriptella.jar` via Ant |
| `org.springframework:spring` | 1.2 | drivers optional compile |
| `org.codehaus.janino:janino` | 3.1.0 | drivers optional (pulls `commons-compiler:3.1.0`) |
| `javax.mail:mail` | 1.4.1 | drivers optional |
| `org.apache.velocity:velocity` | 1.6.2 | drivers optional (transitive Collections 3.2.1, Lang 2.4) |
| `org.apache.ant:ant` | 1.7.1 | tools compile (pulls `ant-launcher:1.7.1`) |

HSQLDB/H2 and Rhino are owned by other chunks and are out of scope here.

### Ant / committed `lib/`

| File | Recorded version (`lib/versions.properties`) | Role |
| --- | --- | --- |
| `commons-jexl.jar` | 2.0.1 | Embedded into all-in-one JAR |
| `commons-logging.jar` | 1.0.4 | Embedded into all-in-one JAR |
| `spring.jar` | 1.2 | Optional; excluded from binary/examples archives |
| `janino.jar` + `janino-commons-compiler.jar` | 3.1.0 | Optional driver JARs under `lib/` / samples |
| `j2ee/mail.jar` + `activation.jar` | 1.4.1 / 1.1 | Optional |
| `velocity-dep.jar` | 1.6.2 | Fat JAR (Velocity + transitive deps) |

Binary packaging embeds only JEXL + Commons Logging into `scriptella.jar`.
Spring is explicitly excluded from samples/lib copy and from the binary ZIP
dependency set. Optional drivers remain separate JARs for Ant consumers.

### Hygiene finding (incomplete JEXL 3 revert)

`samples/lib/` previously retained leftover JEXL 3 artifacts from the reverted
Chunk 4 exploration because `ant jar` copied into `samples/lib` without
removing stale files.

**Mitigation:** the `jar` target in `build.xml` now deletes `samples/lib`
contents (except samples-only `readme.txt`) before refreshing from `lib/`.
Do not treat historical leftover filenames as the intended 1.4 baseline.

### Scriptella source usage of Commons Logging

No Scriptella production class imports `org.apache.commons.logging.*`. The
JAR is present solely for Commons JEXL (and historically for Spring).

---

## Candidate verification (Maven Central, July 25 2026)

All planned candidate artifacts returned HTTP 200 from Maven Central except
where noted.

| Candidate | Available | Bytecode major | License (summary) |
| --- | --- | --- | --- |
| Spring Framework modules 5.3.39 (`spring-core`, `spring-beans`, `spring-context`, `spring-jdbc`, `spring-jcl`) | Yes | 52 (Java 8) | Apache 2.0 |
| Monolithic `org.springframework:spring:5.3.39` | **No** (split modules only) | — | — |
| Monolithic `org.springframework:spring:4.3.30.RELEASE` | **No** | — | — |
| Spring modules 4.3.30.RELEASE | Yes | 50 (Java 6) | Apache 2.0 |
| `org.codehaus.janino:janino:3.1.12` + `commons-compiler:3.1.12` | Yes | 51 (Java 7) | BSD-3-Clause (Janino) |
| `com.sun.mail:javax.mail:1.6.2` | Yes | 51 (Java 7) | CDDL 1.1 / GPL 2 + CPE |
| `javax.activation:activation:1.1.1` | Yes | (legacy) | CDDL / GPL + CPE |
| `org.apache.velocity:velocity:1.7` | Yes | 48 (Java 1.4) | Apache 2.0 |
| `commons-collections:commons-collections:3.2.2` | Yes | — | Apache 2.0 |
| `commons-lang:commons-lang:2.6` | Yes | — | Apache 2.0 |
| `org.apache.ant:ant:1.10.17` | Yes | 52 (Java 8) | Apache 2.0 |
| `org.apache.commons:commons-jexl:2.1.1` | Yes | 49 (Java 5) | Apache 2.0 |
| `commons-logging:commons-logging:1.2` | Yes | — | Apache 2.0 |
| `commons-logging:commons-logging:1.3.5` | Yes | — | Apache 2.0 |

---

## Per-library analysis

### 1. Spring driver

**Current API surface in Scriptella**

| Class | Spring types used |
| --- | --- |
| `Driver` | `BeanFactory` |
| `EtlExecutorBean` | `BeanFactory`, `BeanFactoryAware`, `InitializingBean`, `BeansException`, `BeanFactoryLocator`, **`SingletonBeanFactoryLocator`**, `BeanFactoryReference`, `StaticApplicationContext`, `Resource` |
| `BatchEtlExecutorBean` | `BeanFactory`, `BeanFactoryAware`, `InitializingBean`, `BeansException`, `Resource` |
| Tests (`springbeans.xml`) | DTD `spring-beans.dtd`, `DriverManagerDataSource`, property injection, `init-method` |

**Public/user contracts**

* ETL XML `driver="spring"` with URL = bean name (optional `spring:` prefix)
* `EtlExecutorBean` / `BatchEtlExecutorBean` as Spring beans: `configLocation`,
  `properties`, `autostart`, progress indicator, `Callable` execution
* Thread-local association of the host `BeanFactory` for the duration of
  `execute()` so nested `spring:` JDBC lookups work (bug #4648)

**Blocker for Spring 5.3.39**

`org.springframework.beans.factory.access.SingletonBeanFactoryLocator` (and the
entire `beans.factory.access` package) is **present in Spring 4.3** and
**absent in Spring 5.3.39**. `EtlExecutorBean.getGlobalThreadLocal()` depends
on it plus the committed
`drivers/src/conf/scriptella/driver/spring/beanFactory.xml` locator document.

Other types used by Scriptella remain available in Spring 5.3 modules
(`BeanFactory`, `BeanFactoryAware`, `InitializingBean`,
`StaticApplicationContext`, `ClassPathXmlApplicationContext`,
`org.springframework.core.io.Resource`,
`org.springframework.jdbc.datasource.DriverManagerDataSource`).

**Packaging impact**

* Replace monolithic `spring:1.2` / `spring.jar` with split modules, at least:
  `spring-core`, `spring-beans`, `spring-context`, `spring-jdbc`, and transitive
  `spring-jcl` / `spring-expression` / `spring-aop` as required by the graph.
* Keep Spring **optional** and **out of** the base binary ZIP (current policy).
* `spring-jcl` re-implements the Commons Logging API. When Spring is on the
  classpath alongside the Commons Logging classes embedded for JEXL, consumers
  must not mix incompatible logging bridges. Prefer documenting that Spring
  apps rely on Spring’s JCL bridge and that Scriptella’s embedded logging is
  for JEXL only.

**XML / DTD**

Tests and samples still use the historical Spring beans DTD. Spring 5 continues
to accept classic bean XML; re-validate `springbeans.xml` after upgrade. Prefer
keeping the existing DTD form if tests pass, rather than forcing XSD migration
in 1.4.

**Recommendation: implement only with an explicit migration (not drop-in)**

Required migration steps for 5B (or a follow-on PR):

1. Replace `getGlobalThreadLocal()` with a Scriptella-owned JVM-global
   `ThreadLocal<BeanFactory>` (or equivalent classloader-safe singleton) that
   preserves the bug #4648 contract without `SingletonBeanFactoryLocator`.
2. Remove dependency on `beanFactory.xml` for the locator path if it becomes
   unused; keep any still-needed factory resources.
3. Switch Maven optional deps and Ant `lib/` from monolithic Spring 1.2 to the
   Spring Framework 5.3.39 modules listed above.
4. Keep characterization + `SpringDriverTest` green on JDK 17.
5. Document that the Spring driver targets Spring Framework 5.3.x (`javax`),
   not Spring 6/7 (`jakarta`).

**Do not** treat Spring 4.3.30 as the 1.4 target: it still has the locator API
but is not a supported Java 17 line. **Do not** move to Spring 6/7 in 1.4.

---

### 2. Janino driver

**API surface**

`CodeCompiler` uses:

* `org.codehaus.janino.ScriptEvaluator`
* `setThrownExceptions`, `setParentClassLoader`, `setExtendedClass` (was
  `setExtendedType` on 3.1.0; see below), `setStaticMethod`, `setMethodName`,
  `setClassName`, `setDebuggingInformation`, `cook`, `getMethod`
* `org.codehaus.commons.compiler.LocatedException` + `getLocation().getLineNumber()`
* Instantiation via `Class.newInstance()` (deprecated on modern JDKs, still works on 17)

**Janino 3.1.12**

* `LocatedException` / `getLocation()` remain.
* Patch-line upgrade from 3.1.0 for Scriptella’s compile/query path, with one
  compile-site adjustment:
  * On **3.1.0**, `ScriptEvaluator` extended `ClassBodyEvaluator`, so the
    deprecated `setExtendedType` alias was visible via inheritance.
  * On **3.1.12**, `ScriptEvaluator` extends `MultiCookable` and implements
    `IScriptEvaluator`, which declares only `setExtendedClass`. The deprecated
    `setExtendedType` alias still exists on `IClassBodyEvaluator` /
    `ClassBodyEvaluator`, but **not** on `ScriptEvaluator`, so
    `evaluator.setExtendedType(...)` no longer compiles when `evaluator` is a
    `ScriptEvaluator`. Call `setExtendedClass` instead (same meaning).

**Recommendation: implement**

3.1.0 → 3.1.12 for both `janino` and `commons-compiler`, with the
`setExtendedClass` call-site update. Update Maven, `lib/janino.jar`,
`lib/janino-commons-compiler.jar`, samples copies, licenses, and
`versions.properties`. Optional: replace `Class.newInstance()` with
`getDeclaredConstructor().newInstance()` as a pure modernization while
touching the file.

Preserve `JaninoConnectionTest`, `JaninoBaseClassesTest`,
`JaninoGetNativeDbConnectionITest`, and error-line reporting behavior.

---

### 3. Mail driver

**API surface**

`MailConnection` uses only the classic `javax.mail.*` / `javax.mail.internet.*`
API: `Session`, `Transport`, `MimeMessage`, `MimeMultipart`, `MimeBodyPart`,
`InternetAddress`, `Message.RecipientType`.

**Candidate `com.sun.mail:javax.mail:1.6.2`**

* Keeps the `javax.mail` package (not Jakarta).
* Depends on `javax.activation:activation` (pin **1.1.1**).
* GroupId/artifactId change from `javax.mail:mail:1.4.1`.
* License remains CDDL/GPL+CPE family; refresh committed license text under
  `lib/j2ee/` if the bundled text differs.

**Recommendation: implement**

Drop-in for Scriptella source. Update Maven coordinates, Ant `lib/j2ee` JARs,
samples, versions, and licenses. Re-run `MaiConnectionTest` / `MailDriverTest`
(message formatting is unit-tested with a stubbed `send`; no live SMTP required).

---

### 4. Velocity driver

**API surface**

`VelocityConnection` uses:

* `VelocityEngine` + `init()` + `evaluate(Context, Writer, String, Reader)`
* `VelocityEngine.RUNTIME_LOG_LOGSYSTEM` with a custom `LogSystem`
* `Context` adapter over Scriptella parameters
* Property `velocimacro.library=""`

**Velocity 1.7**

* `LogSystem`, `RUNTIME_LOG_LOGSYSTEM`, and `evaluate(..., Reader)` are still
  present (LogSystem is legacy but retained in 1.7).
* POM declares compile deps: Commons Collections **3.2.1**, Commons Lang **2.4**,
  plus optional/provided oro, jdom, commons-logging, etc.
* Plan target: use separate JARs, not `velocity-dep.jar`, and pin at least
  Collections **3.2.2** and Lang **2.6**.

**Packaging**

Replacing `velocity-dep.jar` with `velocity.jar` + explicit transitive JARs is
the main work. Confirm runtime needs for Scriptella’s evaluate-only path; do
not ship provided-scope curiosities (servlet-api, ant, logkit) in distributions.

**Recommendation: implement**

API-compatible 1.6.2 → 1.7 with packaging modernization. Run
`VelocityConnectionTest` and `VelocityScriptTest`. Document that Velocity 2.x
is out of scope (LogChute/SLF4J migration).

---

### 5. User-facing Ant integration (`scriptella-tools`)

**API surface**

* `Task`, `BuildException`, `Project`
* `DirectoryScanner`, `FileSet`
* `taskdefs.Java` (forked execution, classpath, maxmemory)

All remain on Ant **1.10.17**. Ant 1.10 is the line already used for Scriptella’s
own JDK 17 validation builds.

**Recommendation: implement**

Bump Maven `org.apache.ant:ant` from 1.7.1 → 1.10.17. Re-run tools Ant task
tests. Note: tools currently declare Ant as a **non-optional** compile
dependency; that packaging policy is unchanged by the version bump. Consumer
Ant installations remain the runtime for the task; the Maven artifact mainly
supports compilation and tests.

---

### 6. Commons JEXL 2.1.1 (candidate only)

**API surface**

* Core: `JexlEngine`, `Expression`, `JexlContext`, `TokenMgrError`,
  `setFunctions`, `createExpression`
* Driver: `Script`, `createScript`, shared engine from
  `JexlExpression.newJexlEngine()`
* Namespaces registered: `date`, `text`, `class`
* Context `has()` always returns true so missing parameters stay silent

**2.1.1 vs 2.0.1**

* Same `org.apache.commons.jexl2` package (not JEXL 3).
* 2.1 added language features (e.g. switch); 2.1.1 is a micro fix for a 2.1
  regression.
* Declares dependency on `commons-logging:1.1.1`.

**Risk**

Low relative to JEXL 3, but still a language/engine bump. Must re-run full
expression and JEXL driver suites; treat any result difference as a
compatibility decision. JEXL 3.6.4 remains out of scope (issue #45).

**Recommendation: implement** after characterization suite stays green on a
local trial in 5B. If any behavioral delta appears, stop and document rather
than rewriting expectations.

---

### 7. Commons Logging

**Current:** 1.0.4 embedded in `scriptella.jar` for JEXL.

**Candidates**

| Version | Notes |
| --- | --- |
| 1.1.1 | Matches JEXL 2.1.1’s declared dependency |
| 1.2 | Last widely used 1.2.x line; common pin |
| 1.3.5 | Newer; still Apache 2.0; avoid if it pulls heavier requirements without benefit |

**Spring interaction:** Spring 5 brings `spring-jcl` (Commons Logging API
facade). Keep a **single** non-Spring Commons Logging version for the embedded
JEXL path; do not add a second bridge.

**Recommendation: implement pin to 1.2** when JEXL moves to 2.1.1 (or 1.1.1 if
5B prefers exact JEXL POM alignment). Reconcile Maven test scope, Ant `lib/`,
and the all-in-one JAR embed rules. Prefer not introducing 1.3.x unless 1.2 is
unavailable (it is available).

---

## Dependency graph / packaging notes

### Current drivers optional graph (Maven)

```
scriptella-drivers
  +- commons-jexl:2.0.1 (compile)
  +- javax.mail:mail:1.4.1 (optional)
  +- janino:3.1.0 → commons-compiler:3.1.0 (optional)
  +- spring:1.2 (optional, monolithic)
  +- velocity:1.6.2 → collections:3.2.1, lang:2.4 (optional)
  +- rhino-engine:1.9.1 (test only)
```

### Intended 5B graph (if all “implement” items approved)

```
scriptella-drivers
  +- commons-jexl:2.1.1 (compile) → commons-logging (managed pin)
  +- com.sun.mail:javax.mail:1.6.2 (optional) → activation:1.1.1
  +- janino:3.1.12 → commons-compiler:3.1.12 (optional)
  +- spring-context:5.3.39 (+ beans/core/jdbc as needed) (optional)
  +- velocity:1.7 (optional) + collections:3.2.2 + lang:2.6 (managed)
scriptella-tools
  +- ant:1.10.17
```

### Conflicts to watch during 5B

* Dual Commons Logging implementations when Spring is present (`spring-jcl` vs
  `commons-logging` JAR).
* Velocity fat JAR residual classes if both `velocity-dep.jar` and split JARs
  appear under `lib/`.
* `samples/lib` stale files after copy-without-delete (`ant jar` does not prune).
* Spring module set incomplete → `NoClassDefFoundError` for
  `DriverManagerDataSource` or expression support.

---

## Characterization coverage

### Existing tests treated as the primary characterization suite

| Area | Tests / samples |
| --- | --- |
| Spring | `SpringDriverTest` + `springbeans.xml` (autostart, batch, datasource, bug #4648 ETL) |
| Janino | `JaninoConnectionTest`, `JaninoBaseClassesTest`, `JaninoGetNativeDbConnectionITest`, perf |
| Mail | `MaiConnectionTest`, `MailDriverTest` |
| Velocity | `VelocityConnectionTest`, `VelocityScriptTest` |
| JEXL | `JexlConnectionTest`, `JexlDriverITest`, `JexlQueryITest`, `EtlVariableITest`, core expression/properties tests (`PropertiesTest` uses property name `var`, dotted `url.prefix`) |
| Ant tools | `EtlTaskBaseTest`, `EtlTemplateTaskTest` |

### Added contract tests (this chunk)

| Test | Purpose |
| --- | --- |
| `core/.../JexlExpressionContractTest` | Pins `${}` evaluation, namespaces, silent missing parameters, set-rejected context |
| `drivers/.../spring/SpringBeanFactoryContractTest` | Pins thread-local BeanFactory association used by `spring:` URLs |
| `drivers/.../janino/JaninoCompilerContractTest` | Pins compile inheritance, query/script execution, compile failure type |
| `drivers/.../velocity/VelocityEngineContractTest` | Pins LogSystem integration and evaluate-to-writer contract |
| `drivers/.../mail/MailConnectionContractTest` | Pins mailto URL validation and text/html formatting contracts |
| `tools/.../AntApiContractTest` | Pins Task/BuildException/FileSet/Java task symbols used by Scriptella |

These tests run against the **current** dependency baseline so 5B upgrades must
keep them green (or obtain an explicit behavior exception).

---

## Compatibility matrix and recommendations

| Library | Current (at 5A) | Target | Risk | 5B action |
| --- | --- | --- | --- | --- |
| Spring | `spring:1.2` monolith | Spring Framework **5.3.39** modules | **High** — `SingletonBeanFactoryLocator` removed | **Remaining — implement with migration** |
| Janino | 3.1.0 | **3.1.12** (+ commons-compiler) | Low | **Done** (July 25, 2026); call `setExtendedClass` on `ScriptEvaluator` (see hierarchy note above) |
| JavaMail | `javax.mail:mail:1.4.1` | **`com.sun.mail:javax.mail:1.6.2`** + activation **1.1.1** | Low | **Done** (July 25, 2026) |
| Velocity | 1.6.2 / `velocity-dep.jar` | **1.7** + Collections **3.2.2** + Lang **2.6** (no fat JAR) | Low–medium (packaging) | **Remaining** |
| Ant (Maven tools) | 1.7.1 | **1.10.17** | Low | **Done** (July 25, 2026) |
| Commons JEXL | 2.0.1 | **2.1.1** | Low–medium (engine) | **Remaining** with full suite |
| Commons Logging | 1.0.4 | **1.2** | Low | **Done** (July 25, 2026) |
| Spring 6/7, Jakarta Mail, Velocity 2, JEXL 3 | — | — | Out of scope | **Reject** for 1.4 |
| Spring 4.3.x as final pin | — | — | Unsupported Java 17 story | **Reject** as 1.4 target |

### Suggested 5B order (bisectable)

1. Hygiene: restore `samples/lib` to match `lib/` — **done** (`build.xml` prune + `ant jar`).
2. Janino 3.1.12 — **done**
3. JavaMail 1.6.2 + Activation 1.1.1 — **done**
4. Ant 1.10.17 — **done**
5. Commons Logging pin — **done**; Commons JEXL 2.1.1 — **remaining**
6. Velocity 1.7 + split transitive JARs — **remaining**
7. Spring 5.3.39 migration (own PR if preferred) — **remaining**

After each step: `mvn clean verify` and relevant Ant tests on JDK 17.

---

## Exit criteria checklist

* [x] Characterization coverage and compatibility matrix for every candidate  
* [x] Explicit implement / implement-with-migration / reject recommendations  
* [x] No dependency version or production packaging change as part of analysis  
  (contract tests and this document only; samples/lib leftover noted for 5B)  
* [x] Non-straightforward Spring change has an owner path (rewrite + modules)  
* [x] Candidate coordinates verified on Maven Central with licenses recorded  

---

## Evidence commands (analysis machine)

* JDK: Temurin 17 (via `/usr/libexec/java_home -v 17`)
* `mvn -pl core,drivers,tools dependency:tree`
* Focused suites for Spring, Janino, mail, Velocity, JEXL, Ant task tests:
  green on current baseline before contract tests were added
* JAR API inspection of candidate artifacts under `/tmp/dep5a` via `javap` /
  `jar tf` against Maven Central downloads
