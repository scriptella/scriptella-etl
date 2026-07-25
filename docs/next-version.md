# What's coming in Scriptella 1.4

Scriptella 1.4 is the next planned release. Development takes place on the
`master` branch under version `1.4-SNAPSHOT`. Scriptella 1.3 remains the latest
published release.

There is no committed 1.4 release date yet. This page summarizes the intended
user-visible direction and will be updated as work is completed. It is not a
release announcement or compatibility promise for an unreleased snapshot.

## Themes

1.4 prioritizes **project maintainability and modernization**: a current Java
baseline, fewer obsolete dependencies, and packaging that matches how
Scriptella is actually built and run. Features are secondary.

* **Keep** optional drivers that still serve real (even if niche) users when a
  small hygiene fix is enough — e.g. Velocity reports stay supported, but the
  fat `velocity-dep.jar` goes away in favor of explicit JARs.
* **Drop** stacks that are no longer relevant — notably **HSQLDB 1.8**, which
  will be removed from tests, samples, and distributions (replacement TBD,
  H2 is a candidate). The **ODBC / JDBC-ODBC bridge adapter** has already
  been removed (Chunk 6A).
* **Do not** force library bumps that break existing scripts without a
  deliberate migration (e.g. Commons JEXL stays on **2.0.1** after a failed
  2.1.1 trial).

## Java 17 baseline

Scriptella 1.4 requires Java 17 for building and running and will produce Java
17 bytecode (class-file major version 61). Runtime and packaging work for JDK
17 is largely on `master` (scripting, Rhino, distribution layout). Applying
`release=17` to the Maven and Ant compilers is still tracked as plan Chunk
**5C**; until that lands, snapshot builds may still emit Java 8 bytecode.

JavaScript execution now uses the official Mozilla Rhino 1.9.1 JSR-223
provider. The binary and examples distributions include the matching
`rhino-engine` and `rhino` JARs, so JavaScript works through the normal
standalone commands without relying on Nashorn.

The common JavaScript language names remain supported, including `js`,
`JavaScript`, and `rhino`. Maven applications that assemble Scriptella from
modules must add `org.mozilla:rhino-engine:1.9.1` when they use JavaScript.

## Planned dependency refresh

The remaining 1.4 work aims for conservative JDK 17 compatibility rather than
upgrading every library to its newest major version. Chunk 5A impact analysis
is complete; see
[chunk-5a-dependency-impact.md](releases/1.4/chunk-5a-dependency-impact.md).

**Completed (Chunk 5B partial):**

* Janino **3.1.12** (with matching commons-compiler)
* JavaMail **`com.sun.mail:javax.mail:1.6.2`** and Activation **1.1.1**
  (still the `javax.mail` API)
* Ant **1.10.17** for the tools module Maven dependency
* Commons Logging **1.2** (embedded for JEXL)

**Rejected for 1.4 after trial:**

* Commons JEXL **2.1.1** — 2.1 introduces `var` and `return` as reserved
  words, so identifiers like `${var}` fail to parse. Stay on **2.0.1**.
  JEXL 3.6.4 remains deferred (issue #45) and would need the same keyword
  and migration analysis.

**Still planned (maintenance-first order):**

1. raise Maven and Ant compiles to **`release=17`** / class-file major **61**
   (plan Chunk 5C);
2. **remove HSQLDB 1.8 entirely** after validating a Java 17-compatible
   replacement for tests and examples (plan Chunk 6);
3. ~~**remove the ODBC driver** and sample (Chunk 6A)~~ — **done**;
4. Velocity **1.7** with **explicit JARs** (drop fat `velocity-dep.jar`); keep
   the optional Velocity driver for existing report ETLs;
5. Spring Framework **5.3.x** with a small Scriptella migration for the removed
   `SingletonBeanFactoryLocator` API;
6. reconciling Maven, Ant, the all-in-one JAR, examples, licenses, and
   distribution archives so they contain consistent dependency versions.

If a proposed upgrade requires a larger behavior or API migration, it will be
reconsidered and documented rather than introduced silently.

## Compatibility and release validation

Before release, the complete Maven, Ant, packaging, launcher, example, and
runtime matrix will be repeated on Java 17. User-visible behavior changes will
be recorded in the changelog with migration guidance where needed.

The detailed implementation scope, decisions, validation evidence, and exit
criteria are maintained in the
[Scriptella 1.4 release plan](releases/1.4/release-1.4-plan.md).

