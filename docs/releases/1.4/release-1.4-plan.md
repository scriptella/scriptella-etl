# Release 1.4 Plan

**Status:** Initial planning

**Umbrella issue:** [#44 — Scriptella 1.4: Release hardening, JDK 17 compatibility, and dependency modernization](https://github.com/scriptella/scriptella-etl/issues/44)

**JDK 17 investigation:** [#31 — Investigate JDK 17 build and runtime compatibility](https://github.com/scriptella/scriptella-etl/issues/31)

## Purpose

Prepare Scriptella 1.4 as the post-1.3 compatibility and modernization
release. This first revision details the JDK 17 workstream because the
experimental branch has produced a bounded implementation and a concrete list
of remaining packaging and runtime issues.

Issue #44 also covers test isolation, launcher path handling, dependency
modernization, and release-process hardening. The JEXL upgrade is now
decomposed as a later chunk; the other workstreams remain in scope for 1.4
but are not yet decomposed in this document.

No compatibility promise, release candidate, tag, publication, or website
deployment is authorized by this plan alone.

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

**Status:** In progress — bundled-distribution revision pending

**Reasoning level:** Moderate

The first implementation checkpoint adopted official Mozilla Rhino 1.9.1 in
test scope. Maven no longer publishes a Rhino dependency from the drivers
module, and the Ant test path uses matching `rhino-engine` and `rhino` JARs
with the complete MPL 2.0 text.

The Unix and Windows launchers now build their application classpath from
`lib/*.jar` with quoted distribution paths and complete argument forwarding.
The Unix launcher was exercised from the unpacked binary distribution on
Temurin 17.0.15. The Windows batch contract was checked in the assembled
artifact for `lib` discovery and `%*` forwarding; native Windows execution
remains part of the broader Chunk 5 platform matrix.

Missing-provider errors for the fixed JavaScript aliases now include the
requested language, discovered engines, supported coordinates, distribution
launcher layout, the `java -jar` limitation, and the embedded-classloader
rule. Unrelated engine names retain the generic error.

Validation used Maven 3.9.9, Ant 1.10.17, DTDDoc 1.1.0, and Temurin 17.0.15
on macOS 15.6.1:

* `mvn -pl drivers -am test` passed 149 core and 150 driver tests;
* `ant clean test` passed the core, drivers, and tools suites;
* `ant -Ddtddoc.dir=... test-distribution` built both archives and passed
  separate-JVM JEXL, missing-provider, optional-Rhino alias, nested
  JavaScript, unknown-language, archive-content, and SPI-content checks;
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

### Remaining work

1. Include the two matching Rhino JARs under `lib/` in the binary and examples
   archives without duplicating their classes or service metadata.
2. Add the two relative Rhino entries to the all-in-one JAR's manifest
   `Class-Path`; do not add them to module JAR manifests.
3. Add distribution-visible MPL 2.0 attribution and a source-availability
   notice pointing to the exact 1.9.1 source artifacts. Preserve Scriptella's
   Apache 2.0 licensing and do not imply that Rhino is Apache-licensed.
4. Update the script-driver documentation and missing-provider remedy for the
   bundled layout. A missing provider in a distribution should advise users to
   restore the complete archive; Maven and embedded users should receive the
   coordinates and application-classloader guidance.
5. Replace the current archive-absence assertions with exact-content checks
   for both JARs, their versions, license, source notice, and provider entry.
6. Exercise JavaScript through `java -jar`, the Unix launcher, and the Windows
   launcher where the platform matrix permits. Retain separate-JVM JEXL,
   alias, nested/sub-ETL, missing-provider, and unrelated-language coverage.
7. Re-run Maven, Ant, distribution, archive, SPI, license, and source-archive
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
* [ ] The binary and examples distributions bundle exactly Rhino 1.9.1.
* [ ] `java -jar` and the distribution launchers discover bundled Rhino.
* [ ] JEXL and JavaScript work out of the box on JDK 17.
* [ ] Rhino's MPL license and exact source-availability notice are complete.
* [ ] Artifact tests prove the bundled layout and absence of embedded or
  duplicated Rhino content.
* [ ] The supported commands are stable enough to document publicly.

## Chunk 4 — Upgrade Bundled JEXL to 3.6.4

**Status:** Pending

**Reasoning level:** Higher

This is a follow-on dependency-modernization chunk. Do not mix it into the
Chunk 3 JDK 17 and optional-provider implementation review. Start it after
that work is stable so JDK migration failures and JEXL behavior changes remain
distinguishable.

### Dependency decision

Replace Commons JEXL 2.0.1 with
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
* `git diff --check` is clean.

## Chunk 5 — Full Compatibility and Distribution Matrix

**Status:** Pending

**Reasoning level:** Higher

Run from a clean checkout after all approved JDK and dependency changes.

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
* compiled bytecode has class-file major version 61;
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

## Chunk 6 — Documentation and Adoption

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
* reconciled dependencies, SPI metadata, and licenses;
* a repeatable compatibility matrix;
* accurate public and maintainer documentation;
* a final resolution of issue #31 under the broader 1.4 work in issue #44.
