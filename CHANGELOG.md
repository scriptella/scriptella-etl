# Changelog

This file records user-visible changes to Scriptella. The historical Forrest
status file (`forrest/status.xml`) is retained as source material for older
releases, but this is the primary changelog from Release 1.3 onward.

## [1.3 RC1] — 2026-07-16

Release 1.3 RC1 establishes a maintainable Java 8 baseline while preserving
the existing Maven modules, Ant-built all-in-one JAR, and distribution
archives. Official final 1.3 artifacts have not yet been published; 1.2
remains the latest generally available release.

### Added

* Added a shell-command driver for executing external commands as Scriptella
  scripts or queries ([#32]).
* Added regression coverage for DTD validation of `<include>` inside `<script>`
  ([#29]). The maintained DTD was already correct, so no runtime or DTD change
  was required.
* Added regression coverage confirming that JDBC `ID` result columns take
  precedence over a same-named global property ([#20]). The reported defect was
  not reproducible in the supported Java 8/HSQLDB path.

### Changed

#### Compatibility

* Set Java 8 as the required runtime baseline and Maven bytecode target.
* Switched JavaScript execution from Nashorn-specific behavior to Rhino through
  a JSR 223 adapter, addressing JavaScript compatibility issue [#2].
* Updated project metadata and documentation to describe focused maintenance
  development and the supported release environment.

#### Build and packaging

* Restored reproducible Maven module builds and tests with Maven 3.6 or newer.
* Preserved the Ant 1.10.17 workflow for the all-in-one JAR and binary, source,
  and examples ZIP archives.
* Made Ant test failures fail the build instead of allowing a successful exit.
* Bundled the Rhino engine and JSR 223 adapter, with license material, so Ant
  builds work from a fresh checkout without requiring Maven dependency staging.
* Removed Apache Forrest website generation from the project build while
  retaining Javadoc and DTD documentation generation. Full distribution builds
  continue to use the external DTDDoc 1.1.0 tool.
* Updated the Maven compiler and Javadoc plugins where required for the release
  build; obsolete Cobertura execution was removed from the normal build path.
* Migrated publication from the retired OSSRH service to the Sonatype Central
  Publisher Portal, with manual approval retained before public release.

#### Dependencies

* Reconciled Maven and standalone-distribution Velocity versions at 1.6.2 and
  added explicit distribution license material.
* Added Rhino 1.7.10 and `rhino-js-engine` 1.7.10 for JavaScript execution.
* Aligned the examples distribution with the Maven-managed JavaMail 1.4.1 and
  Activation 1.1 versions and added explicit license material.
* Kept remaining runtime and integration dependencies unchanged to limit
  release risk.

#### Website and documentation

* Replaced the Forrest-generated public website with directly maintained HTML5
  and CSS while preserving the principal public URLs and useful deep links.
* Removed obsolete Forrest assets, generated PDF page variants, and broken-link
  reports from the website repository.
* Updated the README, reference material, project status, build requirements,
  support links, and release wording.

### Upgrade notes

* Java 8 is the supported baseline. Release 1.3 artifacts target Java 8
  bytecode; newer runtimes are not part of the 1.3 compatibility promise.
* JavaScript ETL should explicitly select `language=rhino`. Scripts that depend
  on Nashorn-specific behavior should be tested and adjusted before upgrading.
* The shell driver executes operating-system commands with the permissions of
  the Scriptella process; review command input and deployment permissions before
  enabling it in untrusted workflows.
* Maven consumers continue to use the `org.scriptella` group ID introduced in
  1.2. The module layout and artifact IDs are unchanged.
* Building the full standalone distribution requires Ant 1.10.17 and an
  external DTDDoc 1.1.0 installation; normal Maven builds do not require
  DTDDoc.

### Deferred and known limitations

* JDK 17 compatibility is evaluated as a bounded feasibility decision after RC1.
  If the diagnosis and implementation fit within the two-chunk boundary, it may
  become part of an RC2 release. Otherwise, it is deferred to Scriptella 1.4.
* Java 21 compatibility is future work after JDK 17 unless later planning
  changes that target.
* Broad Spring, JavaMail/Jakarta, reporting-plugin, and other dependency
  upgrades remain deferred.
* Issue [#20] remains deferred unless a concrete failing ETL and database/driver
  combination is provided.

## [1.2] — 2019-10-03

* Changed published Maven coordinates to the `org.scriptella` group ID.
* Updated the Janino driver to 3.1.0.

## [1.1] — 2012-12-28

* Added JEXL 2.0 support.
* Added flexible parsing and formatting rules for text and CSV drivers.
* Added JDBC query and script batch execution support.
* Added detection of a missing `query.next()` call.
* Fixed SQL parsing of ternary expressions.

## [1.0] — 2010-05-05

* Added CUBRID support and improved JDBC driver auto-detection.
* Added the `nostat` command-line option.
* Updated the HSQLDB, Velocity, Janino, and H2 dependencies.
* Fixed class loading in EAR deployments, HTTP URL writes, lazy connection
  initialization, DB2 null handling, and CSV/text null expansion.

For changes before 1.0 and additional historical detail, see
[`forrest/status.xml`](forrest/status.xml).

[#2]: https://github.com/scriptella/scriptella-etl/issues/2
[#20]: https://github.com/scriptella/scriptella-etl/issues/20
[#29]: https://github.com/scriptella/scriptella-etl/issues/29
[#32]: https://github.com/scriptella/scriptella-etl/issues/32
[1.3 RC1]: https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.3-rc1
[1.2]: https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.2
[1.1]: https://github.com/scriptella/scriptella-etl/releases/tag/scriptella-parent-1.1
[1.0]: https://github.com/scriptella/scriptella-etl/releases/tag/1.0
