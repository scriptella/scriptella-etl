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
modernization, and release-process hardening. Those workstreams remain in
scope for 1.4 but are not yet decomposed into chunks in this document.

No compatibility promise, release candidate, tag, publication, or website
deployment is authorized by this plan alone.

## Current baseline

* `master` is the 1.4 development line and uses version `1.4-SNAPSHOT`.
* Scriptella 1.3 retains its documented Java 8 baseline. Do not revise the
  published 1.3 compatibility claim.
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

Do not describe Scriptella 1.4 as supporting JavaScript on JDK 17 until the
packaged-runtime contract is selected, implemented, documented, and tested.

---

# JDK 17 Workstream

## Compatibility objective

The preferred outcome is:

* Scriptella builds, tests, packages, and runs on JDK 17.
* Existing ETL behavior and scripting aliases remain compatible.
* Maven consumers, the Ant workflow, the all-in-one JAR, the binary
  distribution, and the examples distribution have an explicit and tested
  JavaScript classpath contract.
* The Java baseline decision is deliberate. Either:
  * retain Java 8 source, bytecode, and runtime compatibility while adding
    JDK 17 support; or
  * make Java 17 the minimum for 1.4 and document the migration and its
    consequences.

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

**Status:** Ready

**Reasoning level:** Moderate

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

**Status:** Pending

**Reasoning level:** Higher

This chunk is part of the dependency and compatibility audit required by
issue #44. Do not finalize packaging around an obsolete dependency set.

### Java baseline decision

Compare these policies:

1. Java 8 remains the minimum, Java 8 bytecode is retained, and builds and
   runtime behavior are supported on both Java 8 and Java 17.
2. Java 17 becomes the minimum build and runtime baseline for Scriptella 1.4.

Record:

* user and integration compatibility impact;
* compiler source, target, or `--release` configuration;
* CI and release environment impact;
* Maven and Ant behavior;
* dependency and plugin constraints;
* documentation and migration requirements.

The plan currently prefers retaining Java 8 compatibility if it remains
bounded and does not prevent supported dependency upgrades. That preference
is not the final decision.

### Rhino and JSR-223 decision

Inventory and select:

* the supported Rhino version;
* the matching `rhino-js-engine` provider;
* Maven scopes and transitive dependencies;
* committed JARs in `lib/` and `samples/lib/`;
* license and notice files;
* service-provider entries and engine aliases;
* compatibility with each selected Java runtime.

Also determine whether the script driver should continue using
`ScriptConnection.class.getClassLoader()` or support provider discovery
through a connection, application, or context classloader. Any classloader
change requires tests for isolation, discovery order, duplicate engines, and
failure behavior.

### Exit criteria

* The Java baseline is explicitly approved and documented.
* The Rhino coordinates and license treatment are approved.
* The intended engine-discovery classloader contract is explicit.
* Maven, Ant, standalone, and examples packaging requirements are known.

## Chunk 3 — Implement Packaged JavaScript Runtime Support

**Status:** Pending

**Reasoning level:** Higher

### Packaging decision

Select one supported standalone model after a small prototype:

1. Embed Rhino classes and the Rhino JSR-223 provider in the all-in-one JAR,
   deliberately merging service metadata and including required notices.
2. Ship Rhino as separate binary-distribution libraries and make the launcher
   load them through a documented mechanism, such as an appropriate manifest
   classpath or distribution launcher script.
3. Support both forms only if maintaining both has a concrete user benefit
   and low ongoing cost.

Simply placing Rhino JARs next to `scriptella.jar` is insufficient for
`java -jar`; the selected design must prove that the runtime actually loads
the provider.

### Required behavior

From the unpacked binary distribution on JDK 17:

* launcher startup works;
* a non-script ETL works;
* JavaScript works when the language is omitted;
* `language=js`, `language=JavaScript`, and `language=rhino` work;
* invalid languages report the available engines and fail clearly;
* nested or sub-ETL JavaScript execution works;
* no private checkout path or developer-local Maven repository is required.

If Java 8 remains supported, repeat the same packaged smokes there and verify
that adding Rhino does not unexpectedly replace Nashorn for primary aliases.

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

* inspect expected archive entries;
* inspect the final service-provider file;
* launch the assembled distribution in a separate JVM;
* execute representative JavaScript ETLs through the supported public
  launcher command;
* prove the negative case for an unknown engine name.

### Exit criteria

* The standalone JavaScript contract works on JDK 17.
* Maven and Ant dependency sets agree.
* Binary and examples artifacts contain exactly the intended dependencies.
* License and notice material is complete.
* The supported launcher command is stable enough to document publicly.

## Chunk 4 — Full Compatibility and Distribution Matrix

**Status:** Pending

**Reasoning level:** Higher

Run from a clean checkout after all approved JDK and dependency changes.

### Maven

On every supported build JDK:

```bash
mvn clean verify
mvn clean deploy -Dcentral.skipPublishing=true
```

Confirm:

* all reactor tests pass;
* source, binary, Javadoc, and test artifacts are produced as expected;
* module consumers discover Rhino using normal Maven dependencies;
* compiled bytecode matches the selected Java baseline;
* no class accidentally references APIs newer than the retained minimum.

If Java 8 bytecode is retained, use an appropriate API-level check in addition
to sampling class-file major version 52.

### Ant

On every supported Ant build JDK:

```bash
ant clean test
ant jar
ant -Ddtddoc.dir=/path/to/DTDDoc clean dist
```

Confirm:

* tests fail the build when failures occur;
* the all-in-one JAR has the intended engine providers;
* binary, source, and examples archives are readable;
* generated API and DTD documentation is populated;
* archive paths, versions, licenses, and dependency copies are correct.

If release packaging is intentionally supported on fewer JDKs than runtime
execution, document that distinction rather than implying a broader claim.

### Runtime smoke matrix

Test at least:

* command-line `-version` and help;
* representative JDBC ETL;
* JavaScript ETL for every supported alias;
* Rhino JavaScript from an unpacked binary distribution;
* nested or sub-ETL JavaScript execution;
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

## Chunk 5 — Documentation and Adoption

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
* the packaged JavaScript limitation is fixed or explicitly excluded from the
  approved support claim;
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
* the packaged-runtime design is implemented;
* Maven and Ant dependencies and licenses are reconciled;
* the dual-JDK or selected-JDK validation matrix passes;
* unpacked-distribution JavaScript smokes pass;
* documentation does not overstate support;
* the review diff is clean and contains no stale experimental conclusions.

Small preparatory changes may merge earlier when independently useful and
fully tested. Each such merge must leave issue #31 open and must not claim
complete JDK 17 support.

## Stop and reassess conditions

Pause the work and update issue #44 if:

* retaining Java 8 blocks required supported dependency upgrades;
* scripting provider discovery requires a broad classloader redesign;
* embedding Rhino creates unacceptable duplicate-class, SPI, licensing, or
  security problems;
* Maven and Ant distributions cannot share a coherent dependency model;
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
* a working Rhino strategy for Maven, Ant, standalone, and examples users;
* reconciled dependencies, SPI metadata, and licenses;
* a repeatable compatibility matrix;
* accurate public and maintainer documentation;
* a final resolution of issue #31 under the broader 1.4 work in issue #44.
