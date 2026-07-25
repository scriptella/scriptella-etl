# What's coming in Scriptella 1.4

Scriptella 1.4 is the next planned release. Development takes place on the
`master` branch under version `1.4-SNAPSHOT`. Scriptella 1.3 remains the latest
published release.

There is no committed 1.4 release date yet. This page summarizes the intended
user-visible direction and will be updated as work is completed. It is not a
release announcement or compatibility promise for an unreleased snapshot.

## Java 17 baseline

Scriptella 1.4 requires Java 17 for building and running and produces Java 17
bytecode. The Java 17 migration has been integrated into `master`, including
Maven, Ant, standalone distribution, and scripting compatibility work.

JavaScript execution now uses the official Mozilla Rhino 1.9.1 JSR-223
provider. The binary and examples distributions include the matching
`rhino-engine` and `rhino` JARs, so JavaScript works through the normal
standalone commands without relying on Nashorn.

The common JavaScript language names remain supported, including `js`,
`JavaScript`, and `rhino`. Maven applications that assemble Scriptella from
modules must add `org.mozilla:rhino-engine:1.9.1` when they use JavaScript.

## Planned dependency refresh

The remaining 1.4 work aims for conservative JDK 17 compatibility rather than
upgrading every library to its newest major version. Planned work includes:

* upgrading the bundled Commons JEXL 2.x engine to Commons JEXL 3.6.4 while
  preserving existing expression and scripting behavior;
* refreshing the Spring, Janino, JavaMail, Velocity, Ant, and logging
  dependencies on migration paths intended to minimize application and ETL
  changes;
* removing the obsolete HSQLDB 1.8 dependency after validating a suitable
  Java 17-compatible replacement; and
* reconciling Maven, Ant, the all-in-one JAR, examples, licenses, and
  distribution archives so they contain consistent dependency versions.

These dependency targets remain subject to compatibility tests. If a proposed
upgrade requires a larger behavior or API migration, it will be reconsidered
and documented rather than introduced silently.

## Compatibility and release validation

Before release, the complete Maven, Ant, packaging, launcher, example, and
runtime matrix will be repeated on Java 17. User-visible behavior changes will
be recorded in the changelog with migration guidance where needed.

The detailed implementation scope, decisions, validation evidence, and exit
criteria are maintained in the
[Scriptella 1.4 release plan](releases/1.4/release-1.4-plan.md).

