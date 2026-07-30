# Release 1.4 Plan

**Status:** In progress (Chunks 1–3, **5A–5C**, **6**, and **6A** complete;
Chunk 7 matrix and Chunk 8 release documentation/RC remain)

**Umbrella issue:** [#44 — Scriptella 1.4: Release hardening, JDK 17 compatibility, and dependency modernization](https://github.com/scriptella/scriptella-etl/issues/44)

**JDK 17 investigation:** [#31 — Investigate JDK 17 build and runtime compatibility](https://github.com/scriptella/scriptella-etl/issues/31)

## Purpose

Prepare Scriptella 1.4 as the post-1.3 compatibility and modernization
release. This first revision details the JDK 17 workstream because the
experimental branch has produced a bounded implementation and a concrete list
of remaining packaging and runtime issues.

Issue #44 also covers test isolation, launcher path handling, dependency
modernization, and release-process hardening. The JEXL upgrade and the
remaining JDK 17 dependency refresh are now decomposed as later chunks; the
other workstreams remain in scope for 1.4 but are not yet decomposed in this
document.

No compatibility promise, release candidate, tag, publication, or website
deployment is authorized by this plan alone.

## Release themes and priorities (July 25, 2026)

Scriptella 1.4 is primarily a **maintainability and modernization** release,
not a feature release. Order work and accept or reject changes by these themes:

1. **Improved project maintenance first.** Prefer changes that make the tree
   easier to build, test, package, and understand on a current JDK: one
   dependency graph for Maven and Ant, no fat JARs, no obsolete test-only
   stacks, explicit compiler baseline, and honest optional-driver contracts.
2. **Modernization.** Java 17 baseline and bytecode, current enough libraries
   for that baseline, and distribution layouts that match real runtime needs.
3. **Abandon pieces that are no longer relevant.** Drop or hard-replace
   dead weight rather than carrying it “because it was always there.”
   **HSQLDB 1.8 must be removed entirely** from product, tests, samples, and
   distributions (Chunk 6); it is not a runtime requirement of Scriptella.
   The **ODBC / JDBC-ODBC bridge adapter** must be removed entirely (Chunk 6A):
   `sun.jdbc.odbc.JdbcOdbcDriver` was removed from the JDK in Java 8 and
   cannot work on the 1.4 Java 17 baseline.
4. **Preserve working optional drivers when a small hygiene fix is enough.**
   Do not spend large migrations on optional surface area that is not
   strategic. Example: keep the Velocity driver for the few consumers that
   still use it, but **eliminate `velocity-dep.jar`** and pin explicit JARs
   (Velocity 1.7 + Collections + Lang). Do not invest in Velocity 2.x unless
   product priority changes later.
5. **Do not rewrite user contracts only to force a library bump.** JEXL 2.1.1
   was rejected because `var`/`return` became reserved words; stay on 2.0.1
   for 1.4. JEXL 3 remains a separate future decision (issue #45).

### Priority order for remaining work

| Priority | Work | Why |
| --- | --- | --- |
| High | **Chunk 5C** — Maven/Ant `release=17`, class major 61 | **Done** (July 25, 2026) |
| High | **Chunk 6** — **Remove HSQLDB entirely**; replace test/examples DB | **Done** (July 30, 2026; H2 2.4.240) |
| High | **Chunk 6A** — **Remove ODBC driver** and sample | **Done** (July 25, 2026) |
| Medium | **5B Velocity** — 1.7 + split JARs, drop fat `velocity-dep.jar` | **Done** (July 30, 2026) |
| Medium | **5B Spring** — 5.3.39 migration (own PR) | **Done** (July 30, 2026) |
| Later | Chunk 7 matrix, Chunk 8 docs/RC | After bytecode + dependency/drop chunks are settled |

Within 5B, Velocity packaging comes before Spring because it is smaller and
purely maintenance-oriented; Spring remains required for 1.4 if the Spring
driver is kept, but it is a larger migration.

## Current baseline

* `master` is the 1.4 development line and uses version `1.4-SNAPSHOT`.
* Scriptella 1.3 remains the recommended line for users who require a JDK
  older than 17. Do not revise its published Java 8 compatibility claim.
* The `exp-jdk17` branch contains the investigation record, JavaScript alias
  fallback, and focused tests from issue #31.
* The experiment was based on an older `master`. Rebase or otherwise replay
  its intended changes onto current `master`; do not merge the branch tip
  without review.
* Current `master` already fixes the Ant Javadoc empty-package failure found
  during the experiment. The experiment record must be updated to distinguish
  that resolved issue from remaining JDK 17 work.

### Validated experimental result

The `exp-jdk17` change set was trial-merged without conflicts onto commit
`36a6bef` and validated on July 25, 2026:

| Check | Environment | Result |
| --- | --- | --- |
| `mvn clean verify` | Temurin 17.0.15, Maven 3.9.9 | Pass |
| `mvn clean verify` | Temurin 8u492, Maven 3.9.9 | Pass |
| `ant clean test` | Temurin 17.0.15, Ant 1.10.17 | Pass |
| `ant -Ddtddoc.dir=... clean dist` | Temurin 17.0.15, Ant 1.10.17, DTDDoc 1.1.0 | Pass |
| Ant-generated API documentation | Temurin 17.0.15 | Pass; populated in the binary ZIP |

The experimental JavaScript alias fix is localized and preserves the primary
engine lookup before attempting a Rhino fallback. It is useful groundwork,
but it does not by itself establish complete JDK 17 runtime support.

### Confirmed remaining gap

The Ant-built `scriptella.jar` embeds Scriptella, Commons JEXL, and Commons
Logging, but not Rhino. Its `META-INF/services/javax.script.ScriptEngineFactory`
registers JEXL only. The binary ZIP also omits `rhino.jar` and
`rhino-js-engine.jar`.

Consequently:

* Maven and Ant tests can execute JavaScript on JDK 17 because Rhino is on
  their test classpaths.
* An in-process consumer can execute JavaScript when Rhino is on the
  application classpath.
* Plain `java -jar scriptella.jar ...` cannot execute the default JavaScript
  language on JDK 17 because Nashorn is absent and Rhino is not visible.
* A script connection's `classpath` attribute does not currently solve this:
  `ScriptEngineManager` uses the classloader that loaded `ScriptConnection`,
  rather than the connection-specific driver classloader.
* The historical `-Xbootclasspath/a` pattern is not a suitable Rhino
  JSR-223 solution on modern modular JDKs.

This is an optional script-driver limitation, not a blocker for core
Scriptella operation: JEXL remains embedded and requires no external
JavaScript engine. Do not describe JavaScript as bundled on JDK 17; document
and test the optional Rhino classpath contract instead.

---

# JDK 17 Workstream

## Compatibility objective

The preferred outcome is:

* Scriptella builds, tests, packages, and runs on JDK 17.
* Existing ETL behavior and scripting aliases remain compatible.
* Maven consumers, the Ant workflow, the all-in-one JAR, the binary
  distribution, and the examples distribution have an explicit distinction
  between bundled JEXL support and optional JSR-223 engines.
* Java 17 is the minimum build and runtime baseline for Scriptella 1.4.
  Scriptella 1.3 remains available for older JDKs.

Do not let incidental compiler behavior, a dependency upgrade, or use of a
new JDK API make that baseline decision implicitly.

## Guiding rules

* Treat issue #31 as experimental evidence, not as a finished release claim.
* Prefer a small, explicit scripting compatibility layer over a scripting
  subsystem redesign.
* Preserve primary JSR-223 engine lookup. A requested engine must not be
  silently replaced when it is available.
* Limit fallback to documented JavaScript aliases; unknown languages and
  misspellings must continue to fail.
* Reconcile Maven, committed `lib/`, Ant packaging, samples, licenses, and
  service-provider metadata as one dependency system.
* Do not copy the old Rhino binaries into new release artifacts before the
  dependency audit in issue #44 selects the supported Rhino version and
  confirms its license and transitive requirements.
* Test assembled artifacts, not only reactor classpaths.
* Record exact JDK, Maven, Ant, DTDDoc, platform, and command details for
  release evidence.

## Chunk 1 — Import and Refresh the Experiment

**Status:** Complete (July 25, 2026)

**Reasoning level:** Moderate

The five `exp-jdk17` commits were replayed in order onto
`issue-31-jdk17`, based on `fe0790b`. The experiment record was refreshed for
the `1.4-SNAPSHOT` context, linked to this plan and issue #44, and updated to
show that current `master` resolves the historical Ant Javadoc failure. The
Rhino packaging and classloader findings remain open for Chunks 2 and 3.

Fresh core and drivers suites passed on Temurin 8u492 and Temurin 17.0.15 with
Maven 3.9.9 (149 core tests and 148 driver tests on each JDK). The refreshed
test suite directly proves that a registered primary engine wins without a
Rhino fallback lookup and includes nested/sub-ETL JavaScript coverage.

### Work

1. Rebase `exp-jdk17` onto current `master`, or create a focused branch from
   `master` and replay only the intended changes.
2. Review the five experimental commits individually. Preserve the useful
   investigation history without importing stale release assumptions.
3. Bring forward the localized `ScriptConnection` alias fallback and its
   focused tests.
4. Normalize line endings and remove unintended trailing whitespace.
5. Update the experiment record:
   * change artifact versions from the old `1.3-SNAPSHOT` context where
     appropriate;
   * record that current `master` fixed Ant Javadocs;
   * retain the Rhino packaging and classloader findings;
   * link this 1.4 plan and issue #44;
   * distinguish historical experiment results from fresh 1.4 validation.
6. Review the fallback implementation for clarity and compatibility:
   * requested engine lookup occurs first;
   * omitted language retains the historical JavaScript default;
   * `js`, `JS`, `javascript`, `JavaScript`, `ecmascript`, and `ECMAScript`
     have explicitly tested behavior;
   * explicit `rhino` works;
   * unrelated invalid names still fail;
   * Java 8 continues to prefer Nashorn when its primary alias is registered.

### Validation

Run focused script-driver tests on JDK 8 and JDK 17 before proceeding.

At minimum cover:

* `ScriptConnectionTest`
* `ScriptConnectionPerfTest`
* `ScriptDriverITest`
* `ScriptingQueryITest`
* Scriptella sub-ETL execution using JavaScript

Add a lookup-order test if the existing tests do not directly prove that a
registered primary engine wins over the Rhino fallback.

### Exit criteria

* The intended change is reviewable against current `master`.
* The experiment record is factually current.
* Focused scripting tests pass on the selected compatibility JDKs.
* `git diff --check` is clean, allowing intentional Markdown line breaks only
  when the repository's whitespace policy supports them.

## Chunk 2 — Decide the Java and Rhino Baselines

**Status:** Complete (July 25, 2026)

**Reasoning level:** Higher

This chunk selected Java 17 and separated Scriptella's bundled JEXL
capability from optional JavaScript support. It also fixed the Rhino and
engine-discovery contract that Chunk 3 must implement.

### Java baseline decision: require Java 17

Scriptella 1.4 requires Java 17 for building and running and publishes Java 17
bytecode. Users who require Java 8 or Java 11 should remain on Scriptella 1.3.
This is an intentional release-boundary compatibility break, not an
incidental result of a compiler or dependency upgrade.

The implementation and validation contract is:

* compile with `release=17` in Maven and Ant;
* publish class-file major version 61;
* run the full Maven, Ant, packaging, and runtime matrix on JDK 17;
* test newer LTS JDKs separately when they are added to the support claim;
* allow dependency and build-plugin upgrades to require Java 17, but do not
  raise the baseline beyond 17 incidentally.

**Note:** Chunk 2 recorded this decision. As of the Chunk 5B quick wins,
Maven and Ant still compile with `source`/`target` 1.8 (class-file major 52).
Applying `release=17` and major-version 61 is **Chunk 5C**.

Public documentation must state the minimum runtime, bytecode target, tested
JDKs, supported build JDKs, and release-packaging JDK separately. The
migration note must direct older-JDK users to Scriptella 1.3.

### Rhino and JSR-223 decision: optional Mozilla Rhino 1.9.1

Use the matching official Mozilla artifacts:

* `org.mozilla:rhino:1.9.1`
* `org.mozilla:rhino-engine:1.9.1`

This replaces `cat.inspiracio:rhino-js-engine:1.7.10` and its transitive Rhino
1.7.10 runtime. Mozilla now publishes the JSR-223 provider alongside Rhino,
so Scriptella no longer needs the third-party adapter or its separate BSD
license.

The official 1.9.1 provider uses Java 11 bytecode and runs on the Java 17
baseline. Its release is preferred over the Java-8-compatible 1.7.15.1 line
because Scriptella 1.4 no longer needs the older bytecode constraint.

The provider registers
`org.mozilla.javascript.engine.RhinoScriptEngineFactory` through
`META-INF/services/javax.script.ScriptEngineFactory` and advertises
`rhino`, `Rhino`, `javascript`, and `JavaScript`. It does not advertise `js`,
`JS`, `ecmascript`, or `ECMAScript`, so the fixed Scriptella alias fallback
remains required when Rhino is installed.

JEXL remains embedded in `scriptella.jar` and is Scriptella's bundled
expression and lightweight scripting option. The generic JSR-223 script
driver remains available, but JavaScript is not required for core execution.
Existing ETLs that choose `driver=script` with JavaScript must add a supported
engine.

In Maven, keep `rhino-engine` and its transitive `rhino` runtime out of the
published drivers dependency graph; use them in test scope. Maven consumers
that need JavaScript add `org.mozilla:rhino-engine:1.9.1` explicitly.

The Ant test path may retain the exact selected JARs under `lib/`, with
complete MPL 2.0 license and applicable notice material. Do not copy them to
the base binary or examples distributions merely because tests use them.

### Packaging contract

The base Scriptella distribution does not bundle Rhino. Do not embed or shade
Rhino into `scriptella.jar`, add it to the manifest classpath, copy it into
the binary or examples archives, or merge its provider file into Scriptella's
service metadata.

The supported layouts are:

* Plain `java -jar scriptella.jar ...` supports the bundled functionality,
  including JEXL, without a JavaScript provider.
* Maven consumers add `org.mozilla:rhino-engine:1.9.1` when they use the
  JavaScript script driver.
* Distribution users place matching `rhino-engine` and `rhino` JARs in
  `lib/` and use the Unix or Windows launcher, which loads `lib/*.jar`.
* An explicit application classpath with both Rhino JARs and Scriptella's
  launcher main class is also supported.
* Plain `java -jar` is not the optional-provider command because the JVM
  ignores an external `-classpath` when `-jar` is used.

Missing Rhino must not affect non-JavaScript ETLs. A JavaScript ETL without an
installed provider must fail with a clear unsupported-language message and
the available engines.

### Engine-discovery classloader contract

For 1.4, retain `ScriptConnection.class.getClassLoader()` as the loader passed
to `ScriptEngineManager`. The supported Maven, launcher, and explicit
classpath layouts make an optional provider visible to that application
loader.

The script connection's `classpath` attribute is not a supported
JSR-223-provider path, and 1.4 will not switch discovery to the thread context
classloader or add a multi-loader search. Embedded consumers that supply a
different engine must put its provider and dependencies on the same loader as
Scriptella's script driver. This bounded rule preserves discovery order and
failure behavior and avoids introducing duplicate-engine and isolation
semantics into the JDK 17 compatibility fix. A broader plugin-style discovery
model requires a separate design and the isolation, ordering, duplicate, and
negative tests listed by this plan.

### Exit criteria

* [x] The Java baseline is explicitly approved and documented.
* [x] The Rhino coordinates and license treatment are approved.
* [x] The intended engine-discovery classloader contract is explicit.
* [x] Maven, Ant, standalone, and examples packaging requirements are known.

## Chunk 3 — Implement the Optional JavaScript Contract

**Status:** Complete (July 25, 2026)

**Reasoning level:** Moderate

The implementation adopted official Mozilla Rhino 1.9.1 in test scope for
Maven and bundled the matching `rhino-engine` and `rhino` JARs as separate
files in the binary and examples distributions. Maven no longer publishes a
Rhino dependency from the drivers module. The distributions include the
complete MPL 2.0 text and an exact source-artifact availability notice.

The all-in-one JAR manifest names `lib/rhino-engine.jar` and `lib/rhino.jar`,
so an intact distribution supports JavaScript through `java -jar`. The Unix
and Windows launchers build their application classpath from `lib/*.jar` with
quoted distribution paths and complete argument forwarding. The Unix
launcher was exercised from the unpacked binary distribution on Temurin
17.0.15. The Windows batch contract was checked in the assembled artifact for
`lib` discovery and `%*` forwarding; native Windows execution remains part of
the broader Chunk 7 platform matrix.

Missing-provider errors for the fixed JavaScript aliases now include the
requested language, discovered engines, supported coordinates, complete
distribution restoration guidance, and the embedded-classloader rule.
Unrelated engine names retain the generic error.

Validation used Maven 3.9.9, Ant 1.10.17, DTDDoc 1.1.0, and Temurin 17.0.15
on macOS 15.6.1:

* `mvn -pl drivers -am test` passed 149 core and 150 driver tests;
* `ant clean test` passed the core, drivers, and tools suites;
* `ant -Ddtddoc.dir=... test-distribution` built both archives and passed
  separate-JVM JEXL, missing-provider, bundled-Rhino alias, nested JavaScript,
  unknown-language, archive-content, manifest, license, source-notice,
  version, and SPI-content checks;
* Maven dependency inspection showed `rhino-engine:1.9.1` and transitive
  `rhino:1.9.1` only in test scope;
* `git diff --check` passed.

### Revised packaging decision

Maintainer direction on July 25, 2026 revised the distribution portion of the
Chunk 2 decision. JavaScript is sufficiently common that the downloaded
Scriptella distributions should support it out of the box. This section
supersedes Chunk 2 wherever that chunk says release archives must omit Rhino;
the Maven-consumer and engine-discovery classloader decisions remain intact.

Bundle the official `rhino-engine` and `rhino` 1.9.1 JARs as separate files
under `lib/` in the binary and examples distributions. Do not shade their
classes into `scriptella.jar` or merge their service-provider entry into
Scriptella's own metadata.

Add `lib/rhino-engine.jar` and `lib/rhino.jar` to the all-in-one JAR manifest
classpath so both the existing `java -jar scriptella.jar ...` command and the
Unix and Windows launchers support JavaScript in an intact unpacked
distribution. A copied `scriptella.jar` without its sibling `lib/` directory
does not carry Rhino with it.

Keep Rhino in test scope for Maven. Applications assembled from Maven modules
must continue to request `org.mozilla:rhino-engine:1.9.1` explicitly rather
than receiving JavaScript transitively from `scriptella-drivers`.

### Completed work

1. Included the two matching Rhino JARs under `lib/` in the binary and examples
   archives without duplicating their classes or service metadata.
2. Added the two relative Rhino entries to the all-in-one JAR's manifest
   `Class-Path` without adding them to module JAR manifests.
3. Added distribution-visible MPL 2.0 attribution and a source-availability
   notice pointing to the exact 1.9.1 source artifacts while preserving
   Scriptella's Apache 2.0 licensing.
4. Updated the script-driver documentation and missing-provider remedy for the
   bundled layout. A missing provider in a distribution advises users to
   restore the complete archive; Maven and embedded users receive the
   coordinates and application-classloader guidance.
5. Replaced the archive-absence assertions with exact-content checks
   for both JARs, their versions, license, source notice, and provider entry.
6. Exercised JavaScript through `java -jar` and the Unix launcher, and checked
   the Windows launcher contract. The artifact test retains separate-JVM JEXL,
   alias, nested/sub-ETL, missing-provider, and unrelated-language coverage.
7. Re-ran Maven, Ant, distribution, archive, SPI, license, and source-archive
   validation on the Java 17 baseline.

### Required behavior

From the unmodified binary distribution on JDK 17:

* launcher startup works;
* representative JDBC and JEXL ETLs work;
* exactly the supported Rhino engine and runtime JARs, MPL license, and source
  notice are present under `lib/`;
* JavaScript works through `java -jar`, `bin/scriptella.sh`, and
  `bin/scriptella.bat`;
* JavaScript works when the language is omitted;
* `language=js`, `language=JavaScript`, and `language=rhino` work;
* nested or sub-ETL JavaScript execution works;
* unrelated invalid languages still fail without a Rhino fallback;
* no private checkout path or developer-local Maven repository is required.

### Missing-provider diagnostics

When JavaScript is requested without an installed provider, such as from a
Maven application that omitted the optional dependency or an incomplete
distribution, report:

* the requested `language` value;
* the engines and aliases actually discovered;
* that JavaScript requires the Rhino JSR-223 provider on JDK 17;
* the supported coordinates
  `org.mozilla:rhino-engine:1.9.1` and `org.mozilla:rhino:1.9.1`;
* for distribution users, that the official archive bundles both JARs under
  `lib/` and they should restore the complete distribution;
* for embedded users, that the provider must be visible to the same
  application classloader as Scriptella's script driver.

Do not print this Rhino-specific remedy for arbitrary misspellings or
non-JavaScript engine names. Those failures should retain the concise generic
unsupported-language diagnostic.

### Artifacts to reconcile

Inspect and update as required:

* `pom.xml`
* `drivers/pom.xml`
* `build.xml`
* `build-templates/MANIFEST.MF`
* `lib/`
* `samples/lib/`
* `samples/lib/versions.properties`
* dependency license files
* all `META-INF/services/javax.script.ScriptEngineFactory` entries
* binary and examples archive contents

Avoid duplicate classes, silently overwritten service files, inconsistent
Rhino versions, and dependencies present only in a developer checkout.

### Regression coverage

Add automated artifact-level coverage where practical:

* prove exactly the selected Rhino JARs, license, and source notice are present
  in the binary and examples archives;
* prove `scriptella.jar` does not embed Rhino classes or merge its provider
  file;
* launch the assembled distribution in a separate JVM;
* prove bundled JEXL execution from the unmodified distribution;
* prove the actionable missing-JavaScript-provider diagnostic;
* execute representative JavaScript ETLs through `java -jar` and the
  supported launcher commands;
* prove the generic negative case for an unknown engine name.

### Exit criteria

* [x] Missing-provider diagnostics are actionable and tested.
* [x] Maven and Ant dependency sets agree.
* [x] Unix and Windows launchers construct their classpaths from `lib/*.jar`.
* [x] The binary and examples distributions bundle exactly Rhino 1.9.1.
* [x] `java -jar` and the Unix distribution launcher discover bundled Rhino;
  the Windows launcher contract is assembled and awaits the Chunk 7 native
  Windows matrix.
* [x] JEXL and JavaScript work out of the box on JDK 17.
* [x] Rhino's MPL license and exact source-availability notice are complete.
* [x] Artifact tests prove the bundled layout and absence of embedded or
  duplicated Rhino content.
* [x] The supported commands are stable enough to document publicly.

## Chunk 4 — JEXL 3.6.4 Upgrade (Deferred)

**Status:** Out of scope for 1.4

**Reasoning level:** Higher

Do not implement the JEXL 3.6.4 upgrade as part of the 1.4 plan. The proposed
upgrade was explored and reverted because the compatibility policy was not
decided and an invented source-rewriting adapter was not acceptable.

Track the exploration and future proposal in GitHub issue
[#45](https://github.com/scriptella/scriptella-etl/issues/45). A later release
may decide between explicit user-script migration and a narrowly scoped,
reviewed compatibility layer.

As part of the general dependency refresh, the conservative interim target is
Commons JEXL **2.1.1**, the latest release in the JEXL 2 branch. Treat that as
a candidate only: it still requires impact analysis and must not be upgraded
automatically if characterization finds breaking behavior.

### Dependency decision

The deferred JEXL 3 proposal would replace Commons JEXL 2.0.1 with
`org.apache.commons:commons-jexl3:3.6.4`.

JEXL 2.0.1 dates from 2010 and is the central embedded expression dependency.
The last 2.x release is 2.1.1; there is no 2.2.x line. A move only to 2.1.1
would minimize source changes but would leave Scriptella on an obsolete major
line for another release.

Version 3.6.4 is selected instead of 3.7.0 for Scriptella 1.4 because 3.7.0
changes default permissions and language features in ways that can reject
existing scripts at parse time. The 3.6.4 target modernizes JEXL without
combining that migration with the new 3.7 security-default policy. A future
release may adopt the stricter defaults deliberately.

### Work

1. Update Maven dependency management and module dependencies from
   `commons-jexl` to `commons-jexl3`.
2. Replace the committed Ant JAR and reconcile its version, license, notice,
   and transitive Commons Logging requirements.
3. Port Scriptella's small direct API surface from
   `org.apache.commons.jexl2` to `org.apache.commons.jexl3`, including:
   * `JexlExpression`;
   * `JexlConnection`;
   * `JexlContextMap`.
4. Construct the shared engine through `JexlBuilder` with explicit options.
   Do not rely on version-dependent defaults for strictness, silence,
   permissions, side effects, lexical scoping, or ant-style variables.
5. Preserve Scriptella's existing trusted-ETL behavior unless a change is
   separately approved and documented.
6. Remove all JEXL 2 classes, coordinates, service metadata, and stale license
   material from Maven, Ant, the all-in-one JAR, binary archives, and examples.

### Compatibility coverage

At minimum test:

* `${...}` expression evaluation;
* missing and null variables;
* numeric coercion, comparison, concatenation, and division;
* dotted or ant-style variable names used by Scriptella;
* `date:`, `text:`, and `class:` namespaces;
* JEXL scripts with assignments, conditionals, loops, and `query.next()`;
* calls to external callback objects and static methods;
* nested ETL parameter propagation;
* concurrent use of the shared engine;
* all existing JEXL integration tests and representative samples.

Add focused characterization tests before changing an existing behavior whose
current contract is not obvious. Treat expression or script differences as
compatibility decisions, not as test expectations to update automatically.

### Packaging validation

Confirm:

* Maven and Ant resolve the same JEXL 3.6.4 dependency graph;
* `scriptella.jar` contains JEXL 3 classes and no JEXL 2 classes;
* service-provider metadata names only providers that exist in the artifact;
* binary and examples archives contain the intended JEXL version and complete
  license/notice material;
* representative JEXL ETLs work through `java -jar` and the distribution
  launchers on JDK 17.

### Exit criteria

* JEXL 3.6.4 is the only bundled JEXL version.
* Existing Scriptella expression and JEXL-driver behavior is characterized
  and preserved, or any approved incompatibility has a migration note.
* Maven, Ant, all-in-one, binary, and examples dependency sets agree.
* The full JEXL regression suite passes on JDK 17.
* `git diff --check` is clean when this deferred work is eventually implemented.

### Deferred review findings (no implementation retained)

The JEXL 3.6.4 upgrade was explored and then reverted before acceptance. The
repository remains on JEXL 2.0.1. No source-rewriting or legacy-script adapter
is part of the product.

The exploration established these points for a future, separately reviewed
chunk:

* Commons JEXL 3 requires a Java integration migration from
  `org.apache.commons.jexl2` to `org.apache.commons.jexl3`; Apache does not
  provide a general JEXL 2-to-3 source translator.
* A trial adapter that rewrote identifiers such as `var`, `let`, and `const`
  and intercepted dotted parameter lookup was rejected as an unsafe,
  undocumented semantic change. It must not be reintroduced without an
  explicit compatibility proposal.
* The migration exposed existing Scriptella assumptions that need a decision:
  `var` is used as a property name in `core/src/test/scriptella/PropertiesTest.xml`,
  dotted properties such as `url.prefix` are evaluated as flat Scriptella
  parameters, and `file + var` is covered by
  `core/src/test/scriptella/jdbc/ParametersParserTest.java`.
* The future chunk must choose between an explicit user-script migration and a
  narrowly defined, opt-in compatibility layer; define diagnostics and a
  migration guide; characterize affected samples; and test the choice on JDK
  17. It must also separately review JEXL 3.6.4 engine permissions/features
  and the Commons Logging dependency.

Do not mark Chunk 4 complete until that proposal is reviewed and its decision
is implemented and validated.

## Chunk 5A — Analyze Impact of Remaining Dependency Upgrades

**Status:** Complete (July 25, 2026)

**Reasoning level:** Moderate

**Analysis record:**
[chunk-5a-dependency-impact.md](chunk-5a-dependency-impact.md)

Impact analysis finished without changing dependency versions or production
packaging. Characterization contract tests were added for JEXL expressions,
Spring BeanFactory association, Janino compilation, Velocity evaluate/LogSystem,
mail formatting, and Ant task APIs. All candidate coordinates were verified on
Maven Central.

### Approved target baseline (for Chunk 5B)

| Area | Baseline at 5A | 1.4 target | 5B action |
| --- | --- | --- | --- |
| Spring driver | `org.springframework:spring:1.2` | Spring Framework **5.3.39** split modules | **Done** (July 30, 2026) |
| Janino driver | `janino:3.1.0` | `janino:3.1.12` + `commons-compiler:3.1.12` | **Done** (July 25, 2026) |
| Mail driver | `javax.mail:mail:1.4.1` + Activation 1.1 | `com.sun.mail:javax.mail:1.6.2` + `javax.activation:activation:1.1.1` | **Done** (July 25, 2026) |
| Velocity driver | 1.6.2 / `velocity-dep.jar` | `velocity:1.7` + Collections **3.2.2** + Lang **2.6** | **Done** (July 30, 2026) |
| User-facing Ant | `ant:1.7.1` | Ant **1.10.17** | **Done** (July 25, 2026) |
| Commons JEXL | 2.0.1 | **2.1.1** (not JEXL 3) | **Rejected** (July 25, 2026) — stay on 2.0.1 |
| Commons Logging | 1.0.4 | **1.2** | **Done** (July 25, 2026) |

Rejected for 1.4: Spring 6/7, Jakarta Mail, Velocity 2.x, JEXL 3.x, Spring 4.3
as the final pin.

### Key Spring finding

Before the migration, `EtlExecutorBean.getGlobalThreadLocal()` depended on
`org.springframework.beans.factory.access.SingletonBeanFactoryLocator`, which
exists in Spring 4.3 and is **absent** in Spring 5.3.39. A drop-in Spring 5
upgrade was not possible. 5B replaced that locator with a Scriptella-owned
classloader-safe thread-local context while preserving bug #4648 behavior,
then adopted the complete split Spring 5.3.39 graph.

### Hygiene for 5B

`ant jar` now prunes `samples/lib` before refreshing from `lib/` (keeps
samples-only `readme.txt`). Re-run `jar` if a working tree still has stale
sample libraries from earlier incomplete reverts.

### Exit criteria

* [x] Characterization coverage and a compatibility matrix exist for every
  candidate.
* [x] Each candidate has an explicit implement / implement-with-migration /
  reject recommendation.
* [x] No dependency version or production packaging change was made merely as
  part of analysis.
* [x] The Spring compatibility difference has an owner path and migration note
  before implementation.

## Chunk 5B — Implement Approved Dependency Upgrades

**Status:** Complete (July 30, 2026) — approved quick wins, Velocity split,
and Spring 5.3.39 migration complete; JEXL 2.1.1 rejected July 25, 2026

This is the implementation stage. Execute only the candidates approved by the
impact analysis in [chunk-5a-dependency-impact.md](chunk-5a-dependency-impact.md).
Work one library at a time, keep each change bisectable, and do not
automatically upgrade a library when characterization shows a breaking
API, behavior, namespace, security, or packaging change.

### Quick wins completed (July 25, 2026)

Implemented the four drop-in upgrades and reconciled Maven, Ant `lib/`,
licenses, `versions.properties`, and `samples/lib` (via `ant jar` prune/refresh):

| Library | Change | Notes |
| --- | --- | --- |
| Janino | 3.1.0 → **3.1.12** (+ commons-compiler 3.1.12) | `CodeCompiler` calls `setExtendedClass`. On 3.1.12, `ScriptEvaluator` no longer extends `ClassBodyEvaluator`, so the deprecated `setExtendedType` alias (still on `IClassBodyEvaluator`) is not visible on `ScriptEvaluator`. |
| JavaMail | `javax.mail:mail:1.4.1` → **`com.sun.mail:javax.mail:1.6.2`** | Activation **1.1.1**; package remains `javax.mail` |
| Ant (tools) | 1.7.1 → **1.10.17** | Maven compile dependency only |
| Commons Logging | 1.0.4 → **1.2** | Embedded into `scriptella.jar` for JEXL |

**Validation:** Temurin 17, Maven 3.9.x — `mvn -pl core,drivers,tools -am clean test`
passed (154 core + 162 driver + 14 tools tests). Ant 1.10.17 —
`ant clean test` and `ant jar` passed; `samples/lib` refreshed from `lib/`.

### Commons JEXL 2.1.1 rejected (July 25, 2026)

Local trial of `org.apache.commons:commons-jexl:2.1.1` failed characterization
without any Scriptella source changes. JEXL 2.1 adds **`var`** and
**`return`** as reserved words (absent from 2.0.1). Existing Scriptella usage
of `var` as a property/parameter name therefore fails to parse:

* `JexlExpressionContractTest.testParameterLookupAndConcatenation` —
  `file + var`
* `PropertiesTest` — property `var=1` and `${var}` / `$var` expansion
* `ParametersParserTest.testInvalid` — same `file + var` pattern

Representative error:

```text
org.apache.commons.jexl2.JexlException$Parsing: ... parsing error in 'var'
Caused by: org.apache.commons.jexl2.parser.ParseException: parse error
  at ...Parser.DeclareVar
```

**Decision:** keep **Commons JEXL 2.0.1** for Scriptella 1.4. Do not rewrite
tests or user-facing identifier contracts to force the upgrade. JEXL 3 (issue
#45) remains a separate future decision and must account for the same keyword
break plus the package migration.

Maven pin, `lib/commons-jexl.jar`, and `versions.properties` remain at **2.0.1**.

### Velocity split completed (July 30, 2026)

Velocity **1.7** now uses separate Velocity, Commons Collections **3.2.2**,
and Commons Lang **2.6** JARs. The fat `velocity-dep.jar` was removed from
`lib/` and the examples, while the optional Velocity driver and report sample
remain supported. Driver initialization validates all three JARs, and the
assembled-distribution contract runs the packaged primes sample to verify its
connection classpath and examples-archive contents. The binary distribution
does not bundle the optional Velocity runtime or its Commons dependencies.
Velocity 2.x stays out of scope.

### Spring migration completed (July 30, 2026)

The optional Spring driver now targets Spring Framework **5.3.39** using its
split module graph. Scriptella owns the thread-local `BeanFactory` association
used by nested `spring:` JDBC lookups, replacing the removed
`SingletonBeanFactoryLocator` API and obsolete `beanFactory.xml` resource.
Spring remains excluded from the base binary and examples distributions.

See also **Release themes and priorities**: HSQLDB removal (Chunk 6) and
Java 17 bytecode (Chunk 5C) outrank remaining 5B for overall maintainability.

### Implementation rules

1. Implement only a candidate with a reviewed recommendation that it is
   straightforward and non-breaking, or with a separately approved migration
   proposal.
2. For Commons JEXL, consider only the 2.1.1 minor-line candidate in this
   plan. Do not implement the JEXL 3.6.4 migration here; it belongs to GitHub
   issue #45 and a future release decision.
3. Preserve the focused characterization tests and add regression tests for
   every observed compatibility boundary. Never change expected results only
   to make a build green.
4. Reconcile Maven, Ant, `lib/`, `samples/lib/`, version properties,
   distributions, licenses, notices, and dependency graphs after each
   approved upgrade.
5. Stop and return to analysis if an upgrade reveals a breaking change that
   was not covered by the approved proposal.

### Validation and exit criteria

On JDK 17, run `mvn clean verify`, `ant clean test`, and
`ant -Ddtddoc.dir=/path/to/DTDDoc test-distribution`, plus focused tests for
each changed library and a clean consumer Ant build. Confirm Maven and Ant
resolve the same exact graph, optional dependencies remain optional, and
archives contain complete version/license/notice material. Mark this chunk
complete only when every implemented change is approved, tested, and has no
unreviewed breaking behavior.

## Chunk 5C — Raise Maven and Ant Builds to Java 17

**Status:** Complete (July 25, 2026)

**Reasoning level:** Moderate

Chunk 2 decided that Scriptella 1.4 requires Java 17 and publishes Java 17
bytecode. This chunk applies that contract to every production compile path.

### Applied baseline

| Path | Setting |
| --- | --- |
| Maven `maven-compiler-plugin` | `<release>17</release>` |
| Ant `build-templates/build-template.xml` `javac` | `release="17"` + `includeantruntime="false"` (main + test) |
| Coverage Ant path (`coverage.xml`) | same `release="17"` |
| PMD reporting `targetJdk` | **17** |
| Produced bytecode | major **61** (Java 17) |

### Work

1. Maven: set `maven-compiler-plugin` to `<release>17</release>` (prefer
   `release` over separate `source`/`target`). Drop obsolete 1.8-only options.
2. Ant: update every production `javac` in `build-templates/build-template.xml`
   (and any other Ant files that set 1.8) to compile with **release 17**. Prefer
   the Ant `javac` `release` attribute so the bootstrap classpath is set
   correctly on modern JDKs.
3. Set `includeantruntime="false"` on those `javac` tasks so Ant’s own jars
   are not on the compile classpath (repeatable builds; removes the Ant warning).
4. Align auxiliary tool configs that hard-code the product JDK (for example
   PMD `targetJdk` in `pom.xml`) with **17**.
5. Confirm produced classes from both `mvn clean compile` and `ant jar` are
   major version **61** (spot-check representative classes in core, drivers,
   tools, and the all-in-one JAR).
6. Run full Maven and Ant test suites on JDK 17 after the switch.
7. Update maintainer-facing notes as needed so they do not claim Java 8
   bytecode for 1.4 snapshots. User-facing README/CHANGELOG wording may wait
   for Chunk 8 if a short unreleased changelog note is already enough.

### Scope boundaries

This chunk does **not**:

* raise the baseline past 17;
* migrate the JUnit 3 suite or modernize reporting plugins solely for this
  change;
* finish remaining Chunk 5B dependency upgrades;
* authorize a release candidate or close issue #31.

If some optional or test-only path must stay on an older bytecode level, stop
and document an explicit exception rather than leaving silent 1.8 defaults.

### Validation

On Temurin 17 (or equivalent):

```bash
mvn clean test
# confirm class major version 61, e.g. on core and drivers outputs
ant clean test
ant jar
# confirm all-in-one and module JARs contain major 61 classes
```

Confirm:

* no remaining production `source`/`target` 1.8 (or 8) compile settings for
  Scriptella modules;
* `javac` no longer warns about obsolete source/target 8 for those tasks;
* Ant no longer warns about unset `includeantruntime` on updated tasks;
* tests remain green on JDK 17.

### Completion notes (July 25, 2026)

* Maven: `maven-compiler-plugin` **3.13.0** with `<release>17</release>`; PMD
  `targetJdk` **17**.
* Ant: `build-templates/build-template.xml` and `coverage.xml` use
  `release="17"` and `includeantruntime="false"`.
* Tools Ant build: after `includeantruntime="false"`, `tools/build.xml`
  must declare `${ant.home}/lib/ant.jar` on the compile classpath (Ant task
  sources need `Task` / `BuildException`; do not re-enable ant-runtime
  leakage on other modules).
* Validation: Temurin **17.0.20**, Maven 3.8.1, Ant **1.10.17** (installed under
  the parent directory `../ant` → `apache-ant-1.10.17`):
  * `mvn -pl core,drivers,tools -am clean test` SUCCESS
  * `ant clean test` SUCCESS; `ant jar` SUCCESS
  * class major **61** on module outputs and all-in-one `scriptella.jar`

### Exit criteria

* [x] Maven and Ant production compiles use **release 17**.
* [x] Published/build class files are major version **61** (Maven + Ant verified).
* [x] `mvn clean test` and `ant clean test` pass on JDK 17.
* [x] Stale 1.8 product-baseline settings in the build are gone.
* [x] Chunk 7 may assert major 61 without a known compiler gap.

## Chunk 6 — Remove HSQLDB and Refresh Database Examples

**Status:** Complete (July 30, 2026) — HSQLDB removed; tests and in-repository
examples use H2 2.4.240

**Reasoning level:** Higher

HSQLDB 1.8.0.10 is used throughout the Scriptella test harness, Ant classpaths,
and committed examples, but it is not a core Scriptella runtime requirement.
**Drop it entirely** from the 1.4 dependency and distribution system rather
than carrying forward another legacy embedded database. Keep the replacement
open until a compatibility spike compares the available Java 17-compatible
embedded databases and their licenses. The compatibility spike selected
**`com.h2database:h2:2.4.240`**: it supports the Java 17 baseline, provides a
maintained JDBC driver, supports both in-memory and file databases, and is
available under MPL 2.0 or EPL 1.0. Scriptella redistributes it under MPL 2.0.
Partial removal is not success: no HSQLDB JARs, coordinates,
licenses, sample URLs, or docs may remain in product or examples checkouts.

The examples currently shipped for 1.4 live under `scriptella-etl/samples` and
are assembled by this repository's `examples` target; no separate
`scriptella-examples` checkout is present in this workspace or required by the
current release build.

### Work

1. Inventory every HSQLDB reference in Maven, Ant, committed `lib/`, tests,
   samples, generated archive inputs, documentation, and launcher commands.
2. Run a small compatibility spike against one or more replacement databases.
   Record the selected coordinates and exact version only after checking Java
   17 support, SQL behavior, JDBC driver naming, shutdown semantics, licensing,
   archive size, and maintenance status.
3. Replace HSQLDB in the Scriptella core, drivers, and tools test suites,
   including JDBC, Spring, launcher, template, transaction, and nested-query
   coverage. Preserve test intent; do not merely change expected failures to
   obtain a green build.
4. Update the in-repository examples in the same change window: sample ETLs,
   Ant builds, properties, README material, database driver names, URLs,
   initialization scripts, and generated example archives.
5. Remove HSQLDB coordinates, JARs, licenses, version properties, classpath
   entries, and stale `org.hsqldb` references from product code, examples, and
   release packaging.
6. Add characterization coverage for SQL and lifecycle differences discovered
   during the migration, especially shutdown, identity columns, aliases,
   generated keys, transactions, file databases, and text-table samples.

### Compatibility and packaging validation

Validate all of the following from a clean product checkout:

* Maven and Ant tests pass without HSQLDB on any classpath;
* the selected replacement works on the Java 17 baseline;
* Spring, mail, Janino, JDBC, and template examples that use a database still
  execute with the replacement;
* binary and examples archives contain no HSQLDB JAR, license, coordinate, or
  provider-specific classpath instruction;
* the replacement's license and source/notice material are complete wherever
  it is redistributed;
* no private checkout path or developer-local Maven repository is needed;
* the in-repository examples archive builds and its documented database samples
  run against the same database choice.

### Exit criteria

* HSQLDB is absent from Maven, Ant, source, tests, samples, and release
  archives.
* The replacement database and version are explicitly recorded with their
  compatibility and licensing rationale.
* Existing database-backed regression and example behavior is preserved, or
  approved differences have migration notes.
* Scriptella tests and examples use matching coordinates, URLs, driver names,
  and packaging rules.
* The full Java 17 test and example matrix passes.
* `git diff --check` is clean.

### Completion evidence (July 30, 2026)

* `mvn clean test` passed all core, drivers, and tools tests on Java 17.
* `ant clean test` passed with Ant 1.10.17 on Java 17.
* `ant examples` assembled `scriptella-examples-1.4-SNAPSHOT.zip`; the archive
  contains H2 2.4.240 plus its license/source material and no HSQLDB artifact
  or reference.
* The music-store, database-upgrade initialization, and CSV examples executed
  successfully from an unpacked examples archive using H2 2.4.240.

## Chunk 6A — Remove ODBC / JDBC-ODBC Bridge Adapter

**Status:** Complete (July 25, 2026)

**Reasoning level:** Low

Scriptella’s `odbc` driver is a thin wrapper around
`sun.jdbc.odbc.JdbcOdbcDriver` (the historical Sun JDBC-ODBC bridge). That
bridge was **removed from the JDK in Java 8**. On the Scriptella 1.4 baseline
(Java 17) the adapter cannot function; keeping it advertises support that is
false and adds tests, samples, and auto-driver mappings for a dead path.

**Decision:** remove the ODBC adapter entirely from 1.4. Do not replace it
with a third-party ODBC bridge in this release. Users who need Access/ODBC
data should use a maintained JDBC driver (or external tooling) and
`GenericDriver` / an explicit JDBC driver class — outside this adapter.

Users who still require the historical `odbc` alias must remain on
**Scriptella 1.3** (or supply their own bridge JAR and a plain JDBC
connection without the removed Scriptella driver name).

### Scope inventory (product checkout)

| Area | Location |
| --- | --- |
| Driver implementation | `drivers/src/java/scriptella/driver/odbc/` (`Driver.java`, `package.html`) |
| Auto URL mapping | `drivers/src/conf/scriptella/driver/auto/url.properties` (`jdbc:odbc:=odbc`) |
| Sample | `samples/odbc/` (`etl.xml`, `build.xml`, `readme.txt`) |
| Core tests | `core/.../DriverFactoryTest` (JDBC-ODBC skip/load paths) |
| Drivers tests | `drivers/.../auto/AutoDriverTest` (expects driver name `odbc`) |
| Cross-docs | e.g. `drivers/.../janino/package.html` reference to ODBC sample |
| Historical notes | `forrest/status.xml` (archive only; no need to rewrite history) |

Also check `scriptella-examples` and any distribution packaging lists that
copy `samples/odbc` into the examples ZIP. Remove or replace those entries
in the same change window so published examples do not ship a dead sample.

### Work

1. Delete the `scriptella.driver.odbc` package (source and package docs).
2. Remove the `jdbc:odbc:` → `odbc` entry from auto-driver `url.properties`.
3. Remove `samples/odbc` and any Ant/dist rules that package that sample.
4. Update or delete tests that load `sun.jdbc.odbc.JdbcOdbcDriver` or assert
   the `odbc` Scriptella driver name (`DriverFactoryTest`, `AutoDriverTest`).
   Prefer deleting ODBC-only assertions over leaving permanent skip flags.
5. Fix remaining in-tree docs that present ODBC as a supported Scriptella
   driver or point at the ODBC sample as a working example.
6. Record a **CHANGELOG** “Removed” entry: ODBC / JDBC-ODBC bridge adapter
   dropped; not usable on modern JDKs; 1.3 retains the old driver for
   historical checkouts only.
7. Confirm no SPI, service loader, or driver discovery table still lists
   `odbc` as a built-in name (aside from intentional historical changelog).

### Out of scope

* Implementing a new Access/ODBC integration.
* Bundling a third-party JDBC-ODBC bridge.
* Changing generic JDBC URL handling for non-`odbc` drivers.
* HSQLDB removal (Chunk 6) or other database work.

### Validation

```bash
mvn -pl core,drivers -am clean test
# after Ant is available in the environment:
ant clean test
```

Confirm:

* no compile references to `scriptella.driver.odbc` or
  `sun.jdbc.odbc.JdbcOdbcDriver` remain in production or test source;
* auto-driver resolution no longer maps `jdbc:odbc:`;
* examples/binary packaging (if exercised) does not include `samples/odbc`;
* full drivers suite stays green without ODBC skips.

### Completion notes

Removed `scriptella.driver.odbc`, `samples/odbc`, the `jdbc:odbc:=odbc` auto
map entry, and ODBC-only test paths. `DriverFactoryTest` now covers classpath
JDBC (`hsqldb`) and short-name Scriptella drivers (`jexl`). `AutoDriverTest`
case-insensitive URL matching uses `jdbc:H2:`. CHANGELOG records the removal.

### Exit criteria

* [x] `scriptella.driver.odbc` package is gone.
* [x] Auto URL map has no `jdbc:odbc` → `odbc` entry.
* [x] `samples/odbc` is gone from product.
* [x] Tests no longer depend on the JDK JDBC-ODBC bridge.
* [x] CHANGELOG and user-facing docs state the removal and point older needs
      at Scriptella 1.3 or plain JDBC with an external driver.
* [x] Focused Maven tests green after the change.

## Chunk 7 — Full Compatibility and Distribution Matrix

**Status:** Pending

**Reasoning level:** Higher

Run from a clean checkout after all approved JDK and dependency changes.
Chunk **5C** must be complete so bytecode is already major version 61 before
this matrix is treated as release evidence.

### Maven

On JDK 17:

```bash
mvn clean verify
mvn clean deploy -Dcentral.skipPublishing=true
```

Confirm:

* all reactor tests pass;
* source, binary, Javadoc, and test artifacts are produced as expected;
* the published dependency graph does not impose Rhino on consumers;
* a separate consumer that adds `rhino-engine:1.9.1` discovers Rhino;
* compiled bytecode has class-file major version 61 (Chunk 5C);
* compilation is constrained to the Java 17 API.

### Ant

On JDK 17:

```bash
ant clean test
ant jar
ant -Ddtddoc.dir=/path/to/DTDDoc clean dist
```

Confirm:

* tests fail the build when failures occur;
* the all-in-one JAR registers JEXL and does not contain Rhino;
* binary, source, and examples archives are readable;
* generated API and DTD documentation is populated;
* archive paths, versions, licenses, and dependency copies are correct.

If release packaging is intentionally supported on fewer JDKs than runtime
execution, document that distinction rather than implying a broader claim.

### Runtime smoke matrix

Test at least:

* command-line `-version` and help;
* representative JDBC ETL;
* JEXL ETL from the unmodified distribution;
* missing-provider diagnostics for a JavaScript ETL;
* optional Rhino JavaScript for every supported alias after adding the two
  provider JARs under `lib/`;
* nested or sub-ETL JavaScript execution with optional Rhino installed;
* Janino ETL;
* mail driver unit/integration coverage selected by issue #44;
* examples archive execution;
* Maven consumer execution;
* user-facing Ant task execution.

Capture exact commands and sanitized results. Do not rely only on in-process
unit tests for standalone or distribution claims.

### Exit criteria

* The approved Java build/runtime matrix is green.
* Maven, Ant, standalone, binary distribution, and examples behavior agree.
* No known packaging caveat contradicts the intended public compatibility
  statement.

## Chunk 8 — Documentation and Adoption

**Status:** Pending

**Reasoning level:** Moderate

Update:

* `README.md`
* `CHANGELOG.md`
* this plan
* the issue #31 technical status and final comment
* issue #44 progress
* relevant launcher and scripting examples
* `docs/releases/RELEASE-RUNBOOK.md` when the validated release procedure
  changes
* `scriptella.github.io/` compatibility and download wording when a release
  candidate is intentionally prepared

Documentation must state separately:

* minimum runtime;
* bytecode target;
* tested JDKs;
* supported build JDKs;
* supported release-packaging JDK;
* the correct standalone launcher command;
* how Maven, Ant, and embedded consumers supply scripting engines.

Remove or replace modern-JDK guidance that depends on
`-Xbootclasspath/a`.

### Issue closure

Close issue #31 only after:

* its experiment has been adopted or deliberately superseded;
* the optional JavaScript contract and missing-provider diagnostic are
  documented and validated;
* the final validation matrix is linked;
* remaining modernization work has named follow-up ownership under issue #44
  or a narrower issue.

Merging the alias fallback alone is progress, not sufficient reason to close
the compatibility investigation.

---

# Merge and Release Gates

## JDK 17 implementation merge gate

The JDK 17 implementation is ready for `master` when:

* it is based on current `master`;
* the Java and Rhino baseline decisions are recorded;
* the alias and discovery behavior has focused regression coverage;
* the optional-provider design and diagnostics are implemented;
* Maven and Ant dependencies and licenses are reconciled;
* the JDK 17 validation matrix passes;
* unmodified-distribution JEXL and optional-provider JavaScript smokes pass;
* documentation does not overstate support;
* the review diff is clean and contains no stale experimental conclusions.

Small preparatory changes may merge earlier when independently useful and
fully tested. Each such merge must leave issue #31 open and must not claim
complete JDK 17 support.

The JEXL 3.6.4 upgrade is intentionally a separate follow-on change and does
not block merging an otherwise complete JDK 17 implementation. The final 1.4
distribution matrix runs only after both changes are integrated.

## Stop and reassess conditions

Pause the work and update issue #44 if:

* scripting provider discovery requires a broad classloader redesign;
* keeping Rhino out of release artifacts makes the supported optional-provider
  workflow impractical;
* existing ETL behavior becomes uncertain;
* the release must support materially different standalone launch models;
* the compatibility scope expands beyond a bounded 1.4 workstream.

Do not reduce regression coverage or silently narrow existing behavior merely
to obtain a green JDK 17 build.

## Output

The completed workstream should produce:

* an explicit Java baseline decision;
* supported JDK 17 build, runtime, and packaging behavior;
* preserved and tested JavaScript aliases;
* bundled JEXL operation plus an actionable optional Rhino strategy;
* a separately reviewed upgrade to Commons JEXL 3.6.4;
* a conservative JDK 17 refresh of the retained optional-driver dependencies;
* reconciled dependencies, SPI metadata, and licenses;
* a repeatable compatibility matrix;
* accurate public and maintainer documentation;
* a final resolution of issue #31 under the broader 1.4 work in issue #44.
