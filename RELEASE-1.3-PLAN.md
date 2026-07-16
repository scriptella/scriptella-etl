# Release 1.3 — Low-Effort Modernization Plan

> **Note:** If any chunk requires installing or upgrading tools (JDK, Maven, Ant, etc.), I'll flag it here — I can't install system software myself. Let me know if you need guidance on what to install.

## Goal

Modernize Scriptella with the minimum practical effort, publish a credible 1.3 release, replace the Forrest-based website with directly maintained HTML and CSS, and learn what continued maintenance of the project realistically requires.

This is not intended to be a comprehensive modernization or architectural rewrite.

The preferred approach is to preserve existing behavior, build processes, packaging, documentation, and public URLs where they still work. Changes should be small, targeted, and justified by release needs.

## Guiding Principles

* Prefer small, targeted changes over architectural rewrites.
* Preserve existing behavior and release processes when they still work.
* Keep Maven as the normal module build and Maven artifact publication mechanism.
* Keep the currently supported Ant-based build and distribution workflow for Release 1.3.
* Do not attempt to support every modern Ant version.
* Preserve Scriptella’s user-facing Ant integration.
* Replace the Forrest-based website with directly maintained HTML and CSS.
* Remove PDF versions of website pages.
* Avoid broad dependency upgrades unless required for compatibility, security, artifact availability, or release publication.
* Do not upgrade dependencies merely because newer versions exist.
* When work proves substantially more difficult, risky, or invasive than expected, stop and raise it for reconsideration.
* Requirements may be reduced, deferred, or changed when the expected benefit does not justify the implementation effort.
* Unexpected complexity should become documented follow-up work rather than automatically expanding Release 1.3.
* Use Release 1.3 to establish a maintainable baseline and evaluate whether further investment in Scriptella is justified.

## Work Classification

Every discovered task should be placed into one of these categories.

### Required now

Blocks building, testing, packaging, publishing, deploying the website, or producing a credible 1.3 release.

### Easy improvement

A small, low-risk modernization that fits naturally into the current work.

### Unexpectedly difficult

Requires substantial investigation, migration, compatibility work, or architectural change.

Work should pause and the requirement should be reconsidered before proceeding.

### Future work

Potentially useful, but not necessary for Release 1.3 or for assessing project health.

## Reasoning Levels

The reasoning level estimates how much ambiguity, compatibility judgment, and risk analysis a chunk requires. It is independent of elapsed effort: a long mechanical migration may need less reasoning than a short release-policy decision.

* **Lower:** bounded, repeatable work with clear validation criteria.
* **Moderate:** some investigation or cross-file judgment, but limited architectural or release risk.
* **Higher:** ambiguous failures, compatibility decisions, dependency or licensing reconciliation, broad validation, or externally consequential release actions.

---

# Scope Summary

## Workstream A: Website Modernization (`scriptella.github.io/`)

### Approach

* Plain HTML5 and modern CSS.
* One primary stylesheet.
* No static-site generator.
* No Apache Forrest.
* No PDF page variants.
* Content should remain close to the original, with minor polish, simplification, and factual corrections.
* Preserve the majority of useful existing URLs.
* Preserve important deep-link anchors where practical.
* Keep generated Javadocs and DTD documentation separate and largely unchanged.

| Step | What                                                                                          | Key dirs/files                                                                                                       |
| ---- | --------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| A1   | Create a modern HTML shell and one shared stylesheet. Replace the Forrest `skin/` dependency. | `style.css`, shared page structure                                                                                   |
| A2   | Convert representative pages first to validate navigation and layout.                         | `index.html`, `download.html`, `reference/index.html`, one `howto/` page                                             |
| A3   | Rewrite remaining root pages using the validated shell.                                       | `faq.html`, `support.html`, `license.html`, `tutorial.html`, `changes.html`, `links.html`, optionally `linkmap.html` |
| A4   | Rewrite remaining sub-pages.                                                                  | `reference/drivers.html`, remaining `howto/` page                                                                    |
| A5   | Keep generated documentation largely as-is.                                                   | `docs/api/`, `docs/dtd/`                                                                                             |
| A6   | Preserve useful assets and remove obsolete Forrest artifacts.                                 | keep `images/`, `favicon.ico`, `CNAME`, `dtd/etl.dtd`; remove `skin/`, PDFs, `broken-links.xml`                      |
| A7   | Validate internal links, deep links, image paths, nested paths, and download URLs.            | full site                                                                                                            |
| A8   | Deploy to GitHub Pages.                                                                       | `scriptella.github.io` repository                                                                                    |

## Workstream B: Project Modernization (`scriptella-etl/`)

| Step | What                                                                                              | Key dirs/files                                    |
| ---- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| B1   | Establish a reproducible Maven and Ant baseline before making changes.                            | `pom.xml`, `build.xml`, module builds             |
| B2   | Define the supported Java, Maven, and Ant release environment.                                    | README, release documentation                     |
| B3   | Apply minimal build compatibility fixes.                                                          | parent and module POMs, Ant files                 |
| B4   | Review dependencies and update only approved ones.                                                | `pom.xml`, `lib/`                                 |
| B5   | Reconcile Maven dependencies with bundled distribution libraries.                                 | `pom.xml`, `lib/`, license files                  |
| B6   | Triage and fix a small set of critical issues.                                                    | `core/`, `drivers/`, `tools/`                     |
| B7   | Remove Forrest site generation while retaining required Javadoc and DTD documentation generation. | `build-docs.xml`                                  |
| B8   | Update project status, README, and changelog.                                                     | `README.md`, `CHANGELOG.md`, `forrest/status.xml` |
| B9   | Verify Maven publication and Ant distribution packaging.                                          | Maven release config, `ant dist`                  |
| B10  | Build, test, tag, publish, and verify Release 1.3.                                                | all release files                                 |

---

# Compatibility and Preservation Policy

## Java

* Java 8 is the required baseline runtime and bytecode target.
* Modern-JDK compatibility work is postponed until after the Java 8 release baseline is stable.
* Java 21 (Temurin) will be the later modern-LTS compatibility target; it is not a blocker for the initial Release 1.3 build-preservation chunks.
* Avoid raising the minimum Java version unless doing so is necessary for a 1.3 blocker.
* Document the tested runtime and build environments.

## Maven

Maven remains:

* the canonical module build
* the primary test path
* the Maven artifact publication mechanism

Release 1.3 should not redesign the multi-module structure.

## Ant

Ant remains supported for the existing project workflow where needed for Release 1.3.

In particular:

* preserve the current Ant build if it can be kept working with small changes
* preserve `ant dist`
* preserve the all-in-one JAR
* preserve binary, source, and examples ZIP generation
* document one known working Ant environment
* do not promise compatibility with every modern Ant release
* do not migrate packaging to Maven as part of Release 1.3
* preserve Scriptella’s user-facing Ant task

If a specific Ant target requires substantial rewriting, determine whether that target is actually required before expanding scope.

## Public Website URLs

The website should preserve the large majority of useful public HTML URLs.

The goal is approximately 90% preservation, not absolute compatibility.

### High-priority URLs to preserve

* `/`
* `/index.html`
* `/download.html`
* `/tutorial.html`
* `/faq.html`
* `/support.html`
* `/license.html`
* `/changes.html`
* `/links.html`
* `/reference/`
* `/reference/index.html`
* `/reference/drivers.html`
* `/howto/initialize-database.html`
* `/howto/migrate-from-ant.html`
* `/docs/api/`
* `/docs/dtd/`
* `/dtd/etl.dtd`

### Lower-priority compatibility

The following do not need to be retained when they provide little value:

* PDF variants
* Forrest `skin/` resources
* `broken-links.xml`
* Forrest-generated support files
* obsolete navigation resources
* `linkmap.html`, if it is no longer useful

### Deep links

Important existing anchors should be preserved where practical, especially links to:

* Maven integration
* in-process Java integration
* JMX
* JDBC batching
* driver documentation
* installation
* command-line execution
* Ant integration

Legacy anchors may be retained alongside cleaner new IDs when necessary.

---

# Phase 1: Establish the Baseline

## Chunk 1 — Repository and Build Baseline

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — requires diagnosing and classifying failures across two legacy build systems.

### Work

* Record the available Java, Maven, and Ant versions.
* Run the parent Maven build.
* Run the complete Maven test suite.
* Run the current Ant build.
* Run Ant tests.
* Run `ant dist`.
* Record every failure before making unrelated changes.
* Identify which commands currently produce:

  * module JARs
  * the all-in-one JAR
  * binary ZIP
  * source ZIP
  * examples ZIP
  * Javadocs
  * DTD documentation
* Inspect the generated outputs at a basic level.
* Record required external tools and environment variables.

### Output

* baseline report
* working command list
* reproducible failure list
* initial classification of each problem

### Stop condition

If Maven or Ant requires major environment reconstruction, document the blocker and reconsider the affected requirement before proceeding.

## Chunk 2 — Supported Release Environment

**Status:** ✅ Complete

**Target effort:** approximately 2–4 hours

**Reasoning level:** Higher — establishes compatibility promises and separates release blockers from deferred modern-JDK work.

### Work

* Confirm whether Java 8 remains a practical runtime and bytecode target.
* Identify a practical JDK for building Release 1.3.
* Identify one known working Ant version.
* Determine whether one modern LTS JDK should be included in CI or manual testing.
* Record Maven requirements.
* Record any platform-specific release assumptions.

### Findings

* Java 8 (Temurin 1.8.0_492) confirmed as practical runtime and bytecode target — all 288 tests pass.
* Build JDK: Java 8 Temurin (build JDK for release artifacts).
* Known-working Ant: 1.10.17 — produces all-in-one JAR, module JARs.
* Modern LTS: Java 21 (Temurin) available for optional testing; Java 24 also available but Rhino/JS tests fail.
* Maven: 3.9.9 (IntelliJ bundled) works; Maven 3.6+ should suffice.
* Platform assumptions: macOS 15 (x86_64), but no platform-specific code identified.

### Output

**Compatibility policy (for README and release docs):**

> **Java:** Release 1.3 targets Java 8 runtime compatibility as the required baseline. Modern-JDK compatibility work is postponed until the Java 8 release baseline is stable and will target Java 21 (Eclipse Temurin). Java 21 failures must be recorded as follow-up work but do not block the initial build-preservation chunks.
>
> **Maven:** Maven 3.6+ is required to build from source. IntelliJ IDEA's bundled Maven is also supported.
>
> **Ant:** Apache Ant 1.10.17 is the documented release-packaging environment. Other modern Ant versions may work but are not tested. The Ant build is preserved for release packaging only; Maven is the primary development build.
>
> **Platform:** The release was prepared on macOS 15 (x86_64). The project is pure Java and should work on any platform with a compatible JDK.

---

# Phase 2: Define Release 1.3 Scope

## Chunk 3 — Dependency and Bundled-Library Inventory

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — requires dependency, packaging, licensing, and compatibility judgment across Maven and bundled libraries.

### Work

Inventory dependencies from:

* Maven dependency management
* module POMs
* bundled JARs under `lib/`
* libraries copied into samples
* libraries embedded in the all-in-one JAR
* related license and NOTICE files

Classify each dependency as:

* no change
* required compatibility update
* required security or artifact-availability update
* easy low-risk update
* high-risk update to defer

Pay particular attention to:

* Janino
* H2
* HSQLDB
* Velocity
* Spring
* JEXL
* JavaMail
* Ant
* Commons Logging
* Rhino
* Maven build plugins
* Maven release and signing plugins

### Constraint

This is not a “latest versions” exercise.

Runtime and integration dependencies should not be upgraded without a concrete reason and a manageable compatibility test.

### Output

A dependency decision table for Release 1.3.

### Stop condition

If an upgrade requires meaningful API migration or breaks existing ETL scripts, defer it unless it blocks the release.

## Chunk 4 — Issue Triage and Scope Freeze

**Status:** ✅ Complete

**Target effort:** approximately 2–4 hours

**Reasoning level:** Higher — requires uncertain defect triage, release-risk assessment, and disciplined scope decisions.

### Work

Review open issues and select a deliberately small Release 1.3 set.

For each candidate:

* confirm reproducibility
* identify affected modules
* estimate risk
* determine whether a regression test is practical
* identify compatibility impact
* decide whether it blocks useful modern operation
* classify it as required, easy, difficult, or future work

### Recommended scope

Release 1.3 should focus on:

* build compatibility
* release compatibility
* dependency blockers
* a few clearly valuable bugs
* factual documentation fixes
* website modernization

It should not become a broad redesign of the ETL engine, drivers, or integration model.

### Output

* frozen 1.3 issue list
* separate deferred issue list
* clear non-goals

### Release 1.3 Scope Rule

Fix only defects that affect correctness, compatibility with the selected Java targets, or the ability to build, test, package, and publish reliably. Everything else waits.

### Frozen 1.3 Issues

| Issue | Description | Scope |
|-------|-------------|-------|
| [#29](https://github.com/scriptella/scriptella-etl/issues/29) | DTD error: `<include>` in `<script>` not declared | Fix — concrete, small |
| [#20](https://github.com/scriptella/scriptella-etl/issues/20) | ID columns/variables overridden | Investigate briefly; fix only if reproducible, clearly serious, and bounded |
| — | Ant/Maven test divergence | Resolve or document — directly affects release confidence |
| — | Ant test reports success despite failures | Fix or add a reliable release check |

All other open issues deferred (see execution log for full list).

---

# Phase 3: Minimal Build Modernization

## Chunk 5 — Maven Build Fixes

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — failures require diagnosis, but changes are constrained to minimal Maven compatibility fixes.

### Work

* Fix only reproducible Maven build failures.
* Preserve the existing multi-module structure.
* Preserve source layout unless it blocks modern builds.
* Update build plugins only where required.
* Verify module tests.
* Verify source JAR generation.
* Verify Javadoc JAR generation.
* Remove or disable obsolete reporting plugins only if they break normal builds or releases.
* Avoid unrelated POM cleanup.

### Output

A reliable Maven build in the documented release environment.

## Chunk 6 — Ant Build and Distribution Preservation

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — legacy test reporting, external documentation tools, bundled dependencies, and release packaging interact.

### Work

* Keep the current Ant project structure.
* Fix only problems that block the documented Ant version.
* Preserve:

  * module builds
  * all-in-one JAR generation
  * binary ZIP
  * source ZIP
  * examples ZIP
  * tests where practical
* Keep `ant dist`.
* Avoid migrating packaging to Maven.
* Avoid general Ant modernization.
* Keep Scriptella’s Ant integration.

### Output

A documented Ant command that produces the expected Release 1.3 distribution artifacts.

### Stop condition

If an Ant target requires substantial rewriting, reconsider whether that target is required for 1.3.

## Chunk 7 — Remove Forrest from Build Tooling

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — removal is bounded now that DTDDoc is verified, but distribution documentation must remain intact.

### Work

Refactor `build-docs.xml` conservatively.

Retain or preserve:

* `javadoc`
* DTD generation when reasonably practical
* `codereports` when required by `ant dist`
* generated documentation packaging

Remove:

* Forrest site generation
* Forrest execution
* Forrest validation
* Forrest post-processing
* StatCounter injection
* copying generated docs into a Forrest source tree
* Forrest-specific site cleanup
* obsolete site targets

### Important distinction

Remove Forrest website generation.

Do not unnecessarily remove Javadocs or DTD documentation required by the distribution.

### DTDDoc decision

**Resolved:** DTDDoc 1.1.0 was downloaded from SourceForge and verified under Java 8. Its archive includes all required dependencies, and the historical Ant task successfully generates current HTML from `etl.dtd`.

* Keep DTDDoc as an external release tool; do not add it to Scriptella's runtime `lib/` directory.
* Preserve the `dtd` target and its fail-fast `dtddoc.dir` validation.
* Use `ant -Ddtddoc.dir=/path/to/DTDDoc clean dist` for release distribution builds.
* Keep generated DTD HTML and `etl.dtd` in the binary distribution.

---

# Phase 4: Approved Dependencies and Critical Fixes

## Chunk 8 — Apply Low-Risk Dependency Changes

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — Maven, bundled JARs, samples, licenses, and compatibility must remain synchronized.

### Work

* Apply only the dependency updates approved during triage.
* Update Maven declarations.
* Update bundled JARs where applicable.
* Update sample libraries where applicable.
* Update license and NOTICE material.
* Verify embedded versus optional dependencies.
* Run focused tests after each meaningful change.
* Run Maven and Ant builds after completing the approved set.

### Output

A consistent dependency set across Maven artifacts and the standalone distribution.

## Chunk 9 and Later — Selected Bug Fixes

**Status:** ⬜ Not started

**Target effort:** approximately one four-hour chunk per small issue

**Reasoning level:** Higher — each issue begins with uncertain reproduction, root-cause analysis, and compatibility risk.

### Work per issue

* reproduce the problem
* add a regression test where practical
* implement the smallest safe fix
* run focused tests
* run affected-module tests
* record compatibility implications
* update the changelog entry

### Stop condition

If an issue expands beyond one or two chunks, pause and reconsider whether it belongs in Release 1.3.

---

# Phase 5: Website Foundation

## Chunk 10 — Modern HTML Shell and Stylesheet

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — foundational navigation, responsive layout, and reusable content patterns affect every migrated page.

### Work

Create:

* HTML5 page structure
* one primary `style.css`
* responsive header
* simple primary navigation
* content container
* documentation navigation where useful
* footer
* reusable styles for:

  * headings
  * code blocks
  * inline code
  * tables
  * notes
  * warnings
  * lists
  * long reference pages

Preserve:

* Scriptella logo
* favicon
* CNAME
* useful diagrams and images
* similar general color palette

Remove from the new shell:

* Forrest scripts
* Forrest menus
* font-size controls
* generated publication strips
* dynamic last-modified scripts
* PDF controls
* StatCounter
* Forrest branding

### Recommended primary navigation

* Home
* Download
* Tutorial
* Documentation
* Support

### Secondary or footer links

* Change History
* License
* GitHub
* Drivers
* API Documentation
* DTD Documentation

### Output

A reusable HTML shell and stylesheet.

## Chunk 11 — Representative Page Migration

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — validates the website architecture across root, reference, and nested-page use cases before bulk migration.

### Pages

* `index.html`
* `download.html`
* `reference/index.html`, or the first substantial section of it
* `howto/initialize-database.html`

### Work

* convert Forrest-generated HTML to clean HTML5
* use the shared stylesheet
* preserve useful content
* remove PDF links
* remove obsolete UI
* preserve important URLs
* preserve important anchors
* validate nested relative paths
* test desktop and narrow layouts

### Purpose

These pages test:

* homepage presentation
* release downloads
* long-form technical documentation
* nested-directory navigation

### Output

A validated site structure before full migration.

---

# Phase 6: Full Website Migration

## Chunk 12 — Remaining Root Pages

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Lower — primarily repeatable migration into the already validated shell, with limited editorial decisions.

### Pages

* `faq.html`
* `support.html`
* `license.html`
* `tutorial.html`
* `changes.html`
* `links.html`
* `linkmap.html`, only if it remains useful

### Work

* migrate content to the shared HTML structure
* preserve page URLs
* remove Forrest markup and scripts
* remove PDF links
* update clearly stale factual statements
* keep editorial rewriting limited
* decide whether `linkmap.html` should remain, redirect, or be removed

### Output

All high-value root pages migrated.

## Chunk 13 — Reference Manual Migration, Part 1

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — mostly structured migration, with careful anchor and semantic-markup preservation.

### Suggested scope

* document shell
* navigation
* introduction
* system requirements
* installation
* initial script syntax sections

### Work

* preserve existing content
* simplify generated markup
* preserve important anchors
* improve semantic HTML
* avoid rewriting technical prose unless clearly incorrect

## Chunk 14 — Reference Manual Migration, Part 2

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — integration documentation needs accurate anchors, examples, and nested navigation.

### Suggested scope

* remaining script syntax
* expressions and variables
* command-line execution
* Ant integration
* Maven integration
* in-process Java integration

## Chunk 15 — Reference Manual Migration, Part 3

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Moderate — broad technical content and deep links require judgment even though the shell is established.

### Suggested scope

* JMX
* JDBC adapters
* batching
* non-relational data sources
* examples
* best practices
* remaining sections
* final anchor validation

The exact split may be changed based on page size and complexity.

## Chunk 16 — Remaining Documentation Pages

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Lower — bounded page conversion using established layout and URL-preservation rules.

### Pages

* `reference/drivers.html`
* `howto/migrate-from-ant.html`
* any remaining high-value nested pages

### Work

* preserve URLs
* preserve important anchors
* use the shared layout
* keep generated API and DTD docs separate
* correct only obvious outdated references

## Chunk 17 — Website Cleanup and Validation

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — requires distinguishing obsolete artifacts from compatibility-critical URLs, anchors, images, and generated docs.

### Work

* crawl internal links
* verify fragment links
* verify major legacy anchors
* verify image paths
* verify nested navigation
* verify download URLs
* verify links from README
* verify links from packaged documentation
* check desktop and mobile layouts
* remove:

  * `skin/`
  * PDF page variants
  * Forrest JavaScript
  * Forrest-only images
  * `broken-links.xml`
  * obsolete generated navigation files
* keep:

  * useful images
  * diagrams
  * logo
  * favicon
  * CNAME
  * `dtd/etl.dtd`
  * generated Javadocs
  * generated DTD docs

### Output

A deployable website with no Forrest dependency.

---

# Phase 7: Project Documentation

## Chunk 18 — README and Status Wording

**Status:** ⬜ Not started

**Target effort:** approximately 2–4 hours

**Reasoning level:** Moderate — public compatibility and maintenance claims must be accurate without overstating commitments.

### Work

* replace “no longer actively developed”
* avoid overstating future maintenance commitments
* document the actual Java, Maven, and Ant environment
* document Maven installation
* document standalone distribution usage
* update HTTP links where appropriate
* point to GitHub issues
* review support and contact wording
* align website and README status statements

### Recommended wording direction

Prefer language such as:

* “Maintenance development has resumed.”
* “Scriptella is maintained through focused compatibility and bug-fix releases.”
* “Release 1.3 establishes a modernized maintenance baseline.”

Avoid claiming broad active feature development unless that commitment exists.

## Chunk 19 — Changelog

**Status:** ⬜ Not started

**Target effort:** approximately 2–4 hours

**Reasoning level:** Moderate — requires reconciling historical sources and presenting release-impact and upgrade information accurately.

### Work

* add `CHANGELOG.md`
* preserve `forrest/status.xml` as historical source material
* stop treating Forrest status files as the primary changelog
* summarize Release 1.3 by:

  * compatibility
  * build
  * dependencies
  * bug fixes
  * website
  * documentation
* add upgrade notes
* note deferred issues when relevant

### Output

A maintainable changelog independent of Forrest.

---

# Phase 8: Release Publication

## Chunk 20 — Maven Publication Validation

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — external repository requirements, credentials, signing, metadata, and fallback decisions are consequential and change over time.

### Work

* verify current Maven Central publication requirements
* test repository configuration
* update publication settings only where necessary
* verify POM metadata
* verify source artifacts
* verify Javadoc artifacts
* verify signatures
* verify checksums
* attempt a snapshot or safe staging validation before final release
* avoid replacing the release process wholesale unless the current process is unusable

### Stop condition

If Maven Central publication requires a substantial migration, document the work and reconsider whether it should remain in the initial 1.3 scope.

A temporary GitHub-only release should be considered only as an explicit fallback decision.

## Chunk 21 — Release Candidate Build

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — this is the main release gate and requires cross-checking artifacts, dependencies, licenses, and representative consumers.

### Work

* freeze dependencies
* freeze issue scope
* run a clean Maven build
* run all tests
* run the documented Ant distribution build
* generate:

  * module artifacts
  * all-in-one JAR
  * binary ZIP
  * source ZIP
  * examples ZIP
* inspect archive contents
* verify version numbers
* verify license and NOTICE files
* verify bundled dependencies
* verify no stale libraries remain
* smoke-test the command-line launcher
* run one representative ETL
* test one Maven consumer
* test one standalone-distribution example

### Output

A release-candidate artifact set and checklist.

## Chunk 22 — Release Website Preparation

**Status:** ⬜ Not started

**Target effort:** approximately 4 hours

**Reasoning level:** Lower — mostly bounded content and link updates once candidate artifact names and requirements are fixed.

### Work

* add the 1.3 entry to `download.html`
* add a homepage release announcement
* update `changes.html`
* update Java requirements
* update Maven examples
* update standalone installation instructions
* verify candidate asset names and URLs
* deploy or inspect a website preview

### Output

A website ready for the final artifacts.

## Chunk 23 — Final Release

**Status:** ⬜ Not started

**Target effort:** approximately 2–4 hours

**Reasoning level:** Higher — tagging and publication are externally visible, difficult to reverse, and require final verification across all release surfaces.

### Work

* set final versions
* run the final clean build
* tag the release
* publish Maven artifacts
* publish GitHub release assets
* verify uploaded artifacts
* verify checksums and signatures
* update website URLs
* deploy the plain-HTML website
* verify the live site
* record deferred problems and follow-up recommendations

### Output

* Scriptella 1.3
* Maven artifacts
* GitHub release assets
* modern plain-HTML website
* deferred-work list

---

# Explicitly Out of Scope for Release 1.3

Unless they become necessary blockers:

* replacing Ant packaging with Maven
* removing all Ant build files
* supporting every current Ant release
* redesigning the multi-module structure
* changing source layout
* converting JUnit 3 tests solely for modernization
* upgrading every dependency
* migrating documentation to Markdown
* adding a static-site generator
* retaining PDF website versions
* recreating Forrest behavior
* redesigning Scriptella APIs
* broadly rewriting technical documentation
* replacing Javadoc tooling
* replacing DTD documentation tooling
* resolving every open GitHub issue
* creating a broad new feature roadmap
* promising long-term active feature development

## Deferred migration follow-up

* Replace the temporarily bundled Rhino JARs with a deterministic dependency-staging step after Release 1.3. The future step should use Maven dependency tooling to populate a generated build directory; it must not read hard-coded paths from a developer's local Maven repository.
* Re-evaluate and test Scriptella on Java 21 after the Java 8 release baseline is stable, including the Rhino JavaScript engine aliases used by existing ETL scripts and tests.

---

# Recommended Execution Order

1. ✅ Repository and build baseline
2. ✅ Supported release environment
3. ✅ Dependency and bundled-library inventory
4. ✅ Issue triage and scope freeze
5. ✅ Maven build fixes
6. ✅ Ant build and distribution preservation
7. Forrest removal from build tooling
8. Website shell
9. Representative website pages
10. Remaining website pages
11. Approved dependency changes
12. Selected critical bugs
13. README and changelog
14. Maven publication validation
15. Release candidate
16. Final website updates
17. Final release

Website migration may proceed in parallel with dependency and bug work after the representative shell has been validated.

---

# Success Criteria

Release 1.3 is successful when:

* the project builds in a documented environment
* Maven tests pass, or known exceptions are explicitly documented
* the supported Ant workflow produces the expected distribution
* Maven artifacts can be published
* a small, deliberate issue set is fixed
* Maven and bundled-library versions are consistent
* the website is maintained as plain HTML and CSS
* Forrest is no longer required
* PDF website variants are removed
* the majority of useful public URLs remain available
* important deep links remain functional where practical
* generated Javadocs and DTD documentation remain available
* project status and compatibility claims are accurate
* unexpected legacy complexity is documented without unnecessarily delaying the release
* the work provides enough information to decide whether further Scriptella investment is worthwhile

---

# Initial Information Still Useful

The plan can be started without further information, but these inputs would improve estimates and reduce uncertainty:

* `build.properties`
* module POMs
* module Ant build files
* current GitHub Actions workflows, if any
* complete listing of `lib/`
* complete website file tree
* current open issues being considered for 1.3
* known last-working Java, Maven, and Ant versions
* previous Maven Central release notes or credentials setup documentation
