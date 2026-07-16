# Release 1.3 — Low-Effort Modernization Plan

> **Note:** If any chunk requires installing or upgrading tools (JDK, Maven, Ant, etc.), I'll flag it here — I can't install system software myself. Let me know if you need guidance on what to install.

## Goal

Modernize Scriptella with the minimum practical effort, publish a credible 1.3 release, replace the Forrest-based website with directly maintained HTML and CSS, and learn what continued maintenance of the project realistically requires.

Release 1.3 has three possible public milestones:

1. **RC1** — existing Java 8 modernization baseline, integrated and deployed on the default branches.
2. **RC2** — optional bounded JDK 17 compatibility, built on the RC1 baseline.
3. **Final 1.3** — immutable Maven and GitHub publication from the selected RC.

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
* Do not let external publishing-account access block default-branch integration or website modernization.
* Use release candidates to expose validated progress without claiming final artifact availability.
* Treat JDK 17 as a bounded feasibility decision, not an open-ended requirement.
* Preserve Java 8 compatibility throughout Release 1.3.
* Defer JDK 17 to 1.4 if it exceeds one diagnosis chunk plus one implementation chunk.

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
* Develop the website on `exp-v1.3`, matching the `scriptella-etl` release branch.
* Keep `scriptella.github.io/master` aligned with the currently published `origin/master` until the modernized site has completed validation and is intentionally deployed.
* Do not push the website modernization work yet; publishing remains a separate Release 1.3 decision under A8.
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
| A8   | Adjust public wording and deploy the modernized site for RC1.                                 | `scriptella.github.io` repository, RC1 wording                                                                         |
| A9   | Update the site for RC2 only if bounded JDK 17 compatibility succeeds.                       | `scriptella.github.io` repository, RC2 wording                                                                         |
| A10  | Switch the site to final 1.3 wording and download links after final artifacts exist.          | `scriptella.github.io` repository, final wording                                                                       |

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
| B10  | Integrate and publish the validated Java 8 baseline as RC1.                                        | all release files, RC1 wording                     |
| B11  | Perform bounded JDK 17 compatibility feasibility.                                                 | JDK 17 environment, test results                   |
| B12  | Implement and validate RC2 only if feasibility succeeds.                                          | all release files, RC2 wording                     |
| B13  | Complete final signed Maven and GitHub publication.                                                | all release files, final wording                   |

---

# Compatibility and Preservation Policy

## Java

* Java 8 is the required baseline runtime and bytecode target for Scriptella 1.3.
* After RC1, JDK 17 compatibility will receive one bounded diagnosis chunk and, only if justified, one implementation and validation chunk.
* If compatibility cannot be restored safely within that boundary, JDK 17 is deferred to Scriptella 1.4.
* Java 8 source, bytecode, and compatibility must be preserved throughout Release 1.3.
* Do not claim JDK 17 support until the RC2 validation chunk passes.
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
| [#20](https://github.com/scriptella/scriptella-etl/issues/20) | ID columns/variables overridden | Investigated — not reproducible; regression coverage added; no runtime fix for 1.3 |
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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

## Chunk 9 — Selected Bug Fix: Issue #29

**Status:** ✅ Complete

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

## Bounded Investigation — Issue #20

**Status:** ✅ Complete

The report contains no reproduction steps, database/driver details, or example ETL. The common JDBC case was investigated because it is the narrowest reasonable interpretation of the report.

* JDBC result-set columns are resolved case-insensitively before parent/global properties.
* The query execution context reserves only `rownum` and `etl`; `id` has no special treatment.
* Existing table-copy and nested-query tests already pass `ID` columns through nested scripts successfully.
* New regression coverage verifies that an `ID` result column takes precedence over a same-named global `id` property for `$id`, `${id}`, `?id`, and `?{id}` bindings.

The reported behavior is not reproducible under Java 8 with the release-baseline HSQLDB/JDBC path. No production change is justified without a concrete failing ETL and database/driver combination. The issue is deferred unless such a reproduction is provided; the Release 1.3 investigation is complete.

---

# Phase 5: Website Foundation

## Chunk 10 — Modern HTML Shell and Stylesheet

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — foundational navigation, responsive layout, and reusable content patterns affect every migrated page.

### Work

Treat the legacy site as a source of content and link destinations, not as a
visual or structural design reference. Establish a restrained dark
developer-tool identity with near-black surfaces, light text, and a limited
green accent. Keep the implementation framework-free and understandable.

Create:

* HTML5 page structure
* one primary `style.css`
* a compact SVG Scriptella wordmark for dark backgrounds
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

* favicon
* CNAME
* useful diagrams and images
* public URLs and important legacy anchors as pages are migrated

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

* Documentation
* Tutorial
* Drivers
* GitHub
* Download as the primary action

The logo links to the homepage; a separate Home item is unnecessary.

### Secondary or footer links

* Change History
* License
* Support

### Output

A reusable HTML shell and stylesheet.

## Chunk 11 — Representative Page Migration

**Status:** ✅ Complete

**Target effort:** approximately 4 hours

**Reasoning level:** Higher — validates the website architecture across root, reference, and nested-page use cases before bulk migration.

### Pages

* `index.html`
* `download.html`
* `reference/index.html`, or the first substantial section of it
* `howto/initialize-database.html`

### Work

* redesign the homepage from scratch rather than preserving its legacy composition
* explain what Scriptella is, show a real concise ETL example, summarize key capabilities, and provide obvious getting-started routes
* convert Forrest-generated HTML to clean HTML5
* use the shared stylesheet
* preserve useful content
* remove PDF links
* remove obsolete UI
* preserve important URLs
* preserve important anchors
* validate nested relative paths
* perform light markup, path, and narrow-layout checks; defer the detailed viewport matrix to Chunk 17

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

## Validation Cadence

Keep browser testing during Chunks 12–16 lightweight: check the files and links touched by the chunk, then perform one representative desktop and one narrow-layout smoke test for obvious rendering or overflow problems. Defer the exhaustive viewport matrix, site-wide crawl, legacy-anchor verification, keyboard-focus review, contrast review, and cross-page visual inspection to the dedicated cleanup and validation pass in Chunk 17.

## Chunk 12 — Remaining Root Pages

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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
* preserve known anchors; perform final anchor validation in Chunk 17

The exact split may be changed based on page size and complexity.

## Chunk 16 — Remaining Documentation Pages

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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
* manually review at approximately 1280px, 1024px, 768px, and 390px
* verify that navigation wraps or stacks cleanly without JavaScript
* verify that code blocks and tables scroll without page-wide overflow
* verify visible keyboard focus states and readable contrast
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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

**Status:** ✅ Complete

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

---

# Phase 9: RC1 Integration and Publication

## Chunk 23 — RC1 Plan and Public-Wording Adjustment

**Status:** ✅ Complete

**Target effort:** approximately 2–4 hours

**Reasoning level:** Moderate — public claims and release sequencing must be corrected, but no externally visible actions occur in this chunk.

### Purpose

Prepare the validated source and website branches for publication as Scriptella 1.3 RC1 without claiming that final 1.3 artifacts exist.

### Completed work

* Release plan restructured with three milestones (RC1, optional RC2, Final).
* Chunk 23–29 sequence defined; strategy revision documented in execution log.
* Website wording reviewed and updated on all maintained pages listed in the plan.
* `index.html`, `download.html`, `support.html`, `changes.html`, `tutorial.html`,
  `reference/index.html` updated to identify 1.3 RC1 as the current baseline
  and 1.2 as the latest generally available release.
* Final release asset links removed from the download page.
* Unsupported "Java 8 or newer" compatibility claims corrected to "Java 8
  baseline" pending bounded JDK 17 feasibility.
* README, CHANGELOG, and RELEASE-PUBLISHING.md aligned with RC1 policy.
* Historical execution-log entries superseded by the strategy revision.

### Work

#### Release-plan updates

* Record that the current candidate has reached RC1 status.
* Separate RC1 integration and website deployment from final Maven Central publication.
* State that Sonatype account recovery blocks only:

  * final Maven Central publication
  * final immutable `1.3` coordinates
  * final Scriptella 1.3 release

* State that Sonatype account recovery does not block:

  * default-branch integration
  * RC1 status
  * website modernization deployment
  * continued development
  * JDK 17 feasibility work

#### Website wording

Review all prepared website changes that currently imply Scriptella 1.3 final has been released.

At minimum inspect:

* `index.html`
* `download.html`
* `support.html`
* `changes.html`
* `tutorial.html`
* `reference/index.html`
* `reference/drivers.html`
* `howto/migrate-from-ant.html`
* shared templates
* generated release notes or publication notes

Use wording equivalent to:

> Scriptella 1.3 RC1
>
> The Scriptella 1.3 codebase has reached Release Candidate 1. This maintenance release modernizes the build, website, documentation, and release process while preserving Scriptella's established behavior and Java 8 compatibility.
>
> Official Scriptella 1.3 final artifacts have not yet been published. Scriptella 1.2 remains the latest generally available release.

Requirements:

* Use `Scriptella 1.3 RC1` consistently.
* Do not call it merely "in development."
* Do not claim that Scriptella 1.3 final has been released.
* Do not claim that 1.3 is available from Maven Central.
* Do not publish links to nonexistent final 1.3 assets.
* Keep Scriptella 1.2 identified as the latest generally available release.
* A link to the repository or exact RC1 source commit is acceptable.
* Do not promise RC binary downloads unless they are intentionally created later.
* Preserve the prepared final-release website wording where practical so it can be enabled during the eventual final release.

#### Default-branch preparation

Document the intended merges:

```text
scriptella-etl/exp-v1.3 -> master
scriptella.github.io/exp-v1.3 -> master
```

Record that after the merge:

* development continues on `master`
* POM versions remain `1.3-SNAPSHOT`
* no final `scriptella-parent-1.3` tag exists
* no immutable `1.3` Maven coordinates are published
* no final GitHub Release is created
* the `exp-v1.3` branches should not remain long-running development branches

#### Validation checklist

Prepare the exact checklist for Chunk 25 (merge and deploy):

* website does not claim final availability
* no broken final-release asset links
* 1.2 download links remain valid
* README status matches website status
* changelog distinguishes RC1 from final 1.3
* project versions remain `1.3-SNAPSHOT`
* branch heads are known and recorded
* working trees are clean before merge

### Output

* release plan ready for RC1
* website content ready for RC1 deployment
* exact merge and deployment checklist
* no external actions performed

### Stop condition

Do not proceed if any page still presents 1.3 as a final generally available release.

---

## Chunk 24 — Restore StatCounter Tracking

**Status:** Pending

**Target effort:** approximately 1 hour

**Reasoning level:** Lower — bounded mechanical restoration of an existing tracking snippet.

### Purpose

Restore the existing StatCounter integration from the currently published
`scriptella.github.io/master` site so traffic continues to be measured after
the modernized site is deployed.

### Work

* Inspect the StatCounter snippet and account identifiers currently present on `master`.
* Reuse the existing project/account configuration exactly; do not create a new StatCounter account or tracking project.
* Add the tracking snippet to the shared modern page structure so it appears on every maintained content page.
* Cover root pages and nested pages under `reference/` and `howto/`.
* Do not add tracking to generated Javadocs or generated DTD documentation unless they were previously tracked and doing so is trivial.
* Preserve any required `<noscript>` fallback.
* Keep the integration isolated and easy to remove or replace later.
* Do not restore unrelated Forrest scripts or legacy UI.

### Validation

* Search all maintained HTML pages and confirm each includes the StatCounter integration exactly once.
* Verify nested-page paths do not break the script.
* Confirm no obsolete or duplicate StatCounter project IDs remain.
* Preview representative root and nested pages.
* Confirm the tracking code does not block rendering or break HTML validation.
* Record which page groups are tracked and which generated pages are intentionally excluded.

### Placement

Run this chunk after RC1 wording adjustment (Chunk 23) and before the
website is merged and deployed (Chunk 25).

### Constraints

* Restore tracking only.
* Do not redesign analytics.
* Do not introduce a consent-management system in this chunk.
* Do not add other analytics providers.
* Do not copy back unrelated code from the old site.

---

## Chunk 24A — Website Branding and SEO

**Status:** Pending

**Target effort:** approximately 2–4 hours

**Reasoning level:** Lower — bounded content and metadata changes with clear validation criteria.

### Purpose

Finalize the Scriptella website by adding consistent PVRLabs branding across
all pages and completing the essential SEO setup so the modernized site ships
with discoverable and indexable content.

### Work

#### Branding

* Add a shared footer element with "Built by PVRLabs" attribution on every
  maintained content page (root, `reference/`, `howto/`).
* Keep the PVRLabs link pointing to `https://pvrlabs.xyz`.
* Preserve the existing footer content (license, navigation, theme toggle);
  the PVRLabs line is an addition, not a replacement.
* Do not add PVRLabs branding to generated Javadocs or DTD documentation.

#### SEO

* Create `robots.txt` allowing all crawlers with the sitemap directive.
* Create `sitemap.xml` listing all maintained content pages.
* Verify every page has a unique, descriptive `<title>`.
* Verify every page has a useful `<meta name="description">`.
* Add `<link rel="canonical" href="...">` to every maintained page.
* Add Open Graph `<meta property="og:...">` tags (title, description, type,
  url, image) to every maintained page.
* Add Twitter Card `<meta name="twitter:...">` tags (card, title, description)
  to every maintained page.
* Use `https://scriptella.org` as the canonical base URL.
* Use the site logo or a representative image for social previews.

#### Scope boundaries

* Do not add client-side SEO analysis tools, trackers, or third-party scripts
  beyond the tags listed above.
* Do not create an RSS feed or JSON-LD structured data unless trivial.
* Do not redesign page layouts or navigation.
* Do not modify generated API or DTD documentation pages.

### Placement

Run this chunk after StatCounter restoration (Chunk 24) and before the
website merge and deployment (Chunk 25).

---

## Chunk 25 — Merge RC1 and Deploy Website

**Status:** Pending

**Target effort:** approximately 2–4 hours

**Reasoning level:** Higher — default-branch updates and website deployment are externally visible.

### Purpose

Publish the completed modernization work as Scriptella 1.3 RC1.

### Preconditions

* Chunk 23 is complete.
* Both development branches are clean and pushed.
* RC1 wording has been validated.
* POM versions remain `1.3-SNAPSHOT`.
* No final-release asset links remain visible.
* No final 1.3 tag or GitHub Release exists.
* The maintainer has reviewed the merge and deployment checklist.

### Work

#### Source repository

* verify `scriptella-etl/exp-v1.3` matches its remote
* verify `master` has not received unexpected conflicting changes
* merge `exp-v1.3` into `master`
* preserve the complete modernization history
* push `master`
* verify remote `master`
* record the exact commit representing RC1

#### Website repository

* verify `scriptella.github.io/exp-v1.3` matches its remote
* verify `master` has not received unexpected conflicting changes
* merge `exp-v1.3` into `master`
* push `master`
* verify GitHub Pages deployment
* record the exact website deployment commit

#### Live-site verification

Verify at minimum:

* homepage
* download page
* tutorial
* FAQ
* support
* license
* changes
* reference manual
* drivers page
* both how-to pages
* generated API documentation
* generated DTD documentation
* `/dtd/etl.dtd`

Confirm:

* RC1 wording is visible
* Scriptella 1.2 remains the latest generally available release
* no final 1.3 asset links are broken or exposed
* no page claims Maven Central availability for 1.3
* important legacy URLs still resolve
* important deep links still resolve
* CSS, JavaScript, images, favicon, and nested paths load
* GitHub Pages uses the expected `CNAME`
* HTTPS works
* the deployed site contains no Forrest dependency

#### Optional RC1 tag

Do not create an RC1 tag by default.

A lightweight or annotated source tag may be considered only if the maintainer explicitly wants a fixed source marker. If used, choose and document an unambiguous name such as:

```text
scriptella-parent-1.3-rc1
```

Do not use the final tag:

```text
scriptella-parent-1.3
```

Do not create RC artifacts or a GitHub Release unless separately approved.

#### Branch cleanup

After both merges are verified:

* document that new work continues on `master`
* retain `exp-v1.3` temporarily only if useful for audit or rollback
* do not continue normal development on it
* delete it later only through a separate deliberate cleanup action

### Output

* modernized `scriptella-etl/master`
* modernized `scriptella.github.io/master`
* live RC1 website
* recorded RC1 source commit
* recorded website deployment commit
* no final 1.3 release

### Stop conditions

Stop before pushing if:

* the merge includes unexpected commits
* version numbers changed away from `1.3-SNAPSHOT`
* final 1.3 wording remains on the website
* final release links are exposed
* the website deployment source is unclear

---

# Phase 10: Bounded JDK 17 Compatibility Decision

## Chunk 26 — JDK 17 Compatibility Feasibility

**Status:** Pending after RC1

**Target effort:** one focused chunk, approximately 4 hours

**Reasoning level:** Higher — legacy scripting, service-provider discovery, Maven, Ant, and runtime compatibility interact.

### Purpose

Determine whether JDK 17 support is a small, safe RC2 improvement or work for Scriptella 1.4.

This chunk is diagnostic. Do not turn it into an unbounded compatibility migration.

### Compatibility objective

The preferred result is:

* Java 8 remains the source and bytecode baseline.
* Scriptella builds, tests, and runs on JDK 17.
* Existing ETL behavior remains unchanged.
* Existing Rhino and JavaScript aliases continue to work.
* Maven and the supported Ant workflow remain valid.

Do not raise the minimum required Java version.

### Environment

Use a clean checkout of `master`.

Record:

* exact JDK 17 distribution and version
* Maven version
* Ant version
* DTDDoc version
* platform
* `JAVA_HOME`
* relevant environment variables
* clean repository state

Use the already documented JDK 8 environment for regression comparison.

### Required JDK 17 baseline commands

Run:

```bash
mvn clean test
```

Run:

```bash
ant clean test
```

Run:

```bash
ant -Ddtddoc.dir=/path/to/DTDDoc clean dist
```

Also run focused tests for the scripting subsystem, including the existing tests covering:

* `ScriptConnectionTest`
* `ScriptConnectionPerfTest`
* `ScriptDriverITest`
* `ScriptingQueryITest`
* Scriptella sub-ETL execution using Rhino
* aliases such as `js`, `JavaScript`, and `rhino`

Use exact module and test commands appropriate to the repository.

### Smoke tests

Perform:

* command-line launcher startup
* one representative non-script ETL
* one representative JavaScript/Rhino ETL
* one ETL from the unpacked standalone binary distribution
* one examples-archive smoke test
* inspection of generated artifacts if `ant dist` succeeds

### Investigation

Determine:

1. Which Maven tests fail on JDK 17?
2. Which Ant tests fail on JDK 17?
3. Does `ant dist` fail independently of tests?
4. Are failures isolated to Rhino or `javax.script` discovery?
5. Which engine names are registered?
6. Does `ScriptEngineManager` discover the expected provider?
7. Does direct Rhino engine construction work?
8. Are required `META-INF/services` entries present in:

   * Maven-resolved dependencies
   * committed `lib/` JARs
   * generated module JARs
   * the all-in-one JAR
   * binary distribution contents

9. Is the failure caused by:

   * provider metadata
   * dependency version
   * classpath construction
   * module-path behavior
   * alias registration
   * Ant/Maven dependency divergence
   * removed JDK-provided JavaScript engines

10. Can a localized fix preserve Java 8 behavior?

### Allowed diagnosis work

This chunk may:

* inspect dependencies
* inspect JAR manifests and service-provider files
* run focused experiments
* create disposable test code outside production sources
* test a dependency substitution in a disposable checkout
* identify candidate production files
* estimate implementation scope
* document a proposed fix

This chunk must not commit the production fix unless the distinction between diagnosis and implementation would be artificial and the total work still clearly fits within the two-chunk limit. Prefer keeping implementation in Chunk 27.

### RC2 feasibility criteria

Recommend proceeding to Chunk 27 only if all of the following are true:

* root cause is understood
* the fix is localized
* Java 8 compatibility can be retained
* no scripting-subsystem redesign is needed
* no widespread dependency modernization is needed
* no Ant replacement or major rewrite is needed
* no established ETL semantics need to change
* regression tests can cover the behavior
* complete implementation and validation reasonably fit in one additional focused chunk

### Mandatory deferral conditions

Defer JDK 17 compatibility to Scriptella 1.4 if any of these apply:

* Java 8 support must be dropped
* source or bytecode target must be raised
* the scripting subsystem requires redesign
* many modules require module-system-specific changes
* multiple unrelated dependencies must be upgraded
* Ant packaging needs substantial restructuring
* Rhino must be replaced with a materially different scripting model
* behavior of existing ETL files becomes uncertain
* implementation scope is not confidently bounded
* the work exceeds one diagnosis chunk plus one implementation chunk

### Output

Append a feasibility decision containing:

* observed failures
* root cause
* smallest plausible fix
* affected files and dependencies
* Java 8 compatibility assessment
* required regression tests
* Maven impact
* Ant impact
* distribution impact
* estimated implementation scope
* explicit decision:

  * `Proceed with Scriptella 1.3 RC2`, or
  * `Defer JDK 17 compatibility to Scriptella 1.4`

### Stop condition

Do not continue investigating merely because additional modernization opportunities are discovered.

---

## Chunk 27 — JDK 17 Compatibility and RC2 Validation

**Status:** Conditional

**Target effort:** one focused chunk, approximately 4 hours

**Reasoning level:** Higher — runtime compatibility and scripting behavior require full cross-JDK validation.

### Entry condition

Create and execute this chunk only if Chunk 26 explicitly concludes that the work is bounded and suitable for RC2.

If Chunk 26 defers JDK 17 to 1.4:

* mark this chunk as skipped
* update the deferred-work section
* proceed to final 1.3 publication readiness using the RC1 Java 8 baseline

### Purpose

Implement the smallest safe JDK 17 compatibility fix while retaining Java 8 compatibility.

### Work

#### Implementation

* implement only the fix approved by Chunk 26
* avoid unrelated dependency upgrades
* avoid broad POM cleanup
* avoid scripting API redesign
* preserve existing engine aliases and ETL behavior
* keep Java 8 source and bytecode targets
* keep Maven and Ant dependency sets consistent
* update bundled JARs and license material only if required by the approved fix

#### Regression coverage

Add focused tests that verify:

* JavaScript/Rhino engine discovery
* required engine aliases
* representative script execution
* nested or sub-ETL script execution if affected
* behavior on Java 8
* behavior on JDK 17

Tests should fail for the diagnosed issue and pass after the fix.

#### Java 8 validation

Run from a clean checkout:

```bash
mvn clean test
```

Run:

```bash
ant clean test
```

Run:

```bash
ant -Ddtddoc.dir=/path/to/DTDDoc clean dist
```

Confirm:

* all tests pass
* Java 8 bytecode target remains unchanged
* Maven artifacts remain valid
* Ant distribution archives remain valid
* standalone launcher works
* representative ETL works
* Rhino/JavaScript ETL works

#### JDK 17 validation

Run from a clean checkout:

```bash
mvn clean test
```

Run:

```bash
ant clean test
```

Run the supported Ant distribution command on JDK 17 if the intended compatibility claim includes JDK 17 packaging:

```bash
ant -Ddtddoc.dir=/path/to/DTDDoc clean dist
```

If Ant packaging remains intentionally tied to JDK 8, document that precisely rather than claiming JDK 17 Ant-build support.

Perform:

* launcher smoke test
* representative ETL smoke test
* Rhino/JavaScript smoke test
* unpacked binary distribution smoke test
* examples archive smoke test
* Maven consumer test where relevant
* archive integrity checks
* JAR readability checks
* license and NOTICE checks if dependencies changed

#### Documentation

Update:

* compatibility policy
* README
* changelog
* release plan
* execution log
* release checklist
* website RC wording

State the exact tested matrix. For example:

> Scriptella 1.3 targets Java 8 bytecode and is tested on Java 8 and JDK 17. Release packaging uses the documented Java 8 and Ant environment.

Use only wording supported by completed validation.

#### RC2 publication

Update the website from RC1 to RC2 after successful validation.

The website must still state:

* 1.3 final has not yet been published
* Scriptella 1.2 remains the latest generally available release
* RC2 represents the current source baseline
* Maven Central does not yet contain final 1.3 coordinates

Merge and deploy the RC2 changes through a reviewed public-action checklist.

An optional source tag may use:

```text
scriptella-parent-1.3-rc2
```

Do not use the final tag.

### Output

* validated Scriptella 1.3 RC2
* Java 8 baseline preserved
* documented JDK 17 compatibility
* updated live website
* exact RC2 commit recorded

### Stop condition

If implementation reveals broader work than Chunk 26 predicted:

* revert or isolate incomplete changes
* document the findings
* defer JDK 17 compatibility to 1.4
* do not weaken Java 8 compatibility
* do not allow the work to delay final 1.3 indefinitely

---

# Phase 11: Final Release Readiness and Publication

## Chunk 28 — Final Publication Readiness

**Status:** Pending external access

**Target effort:** approximately 2–4 hours after credentials are available

**Reasoning level:** Higher — signing, immutable coordinates, release commits, and external publication are consequential.

### Purpose

Prepare the selected RC baseline for final immutable Scriptella 1.3 publication.

The final baseline is:

* RC2, if Chunk 27 completed successfully
* otherwise RC1, with JDK 17 explicitly deferred to 1.4

### External prerequisites

Confirm:

* Sonatype Central Portal account access
* control of the `org.scriptella` namespace
* Portal publishing token
* private Maven server configuration using ID `central`
* durable release-signing key
* public signing key available through an accepted key server
* working `gpg-agent`
* GitHub repository administration access
* GitHub Pages deployment access

Do not place credentials, private keys, passphrases, or tokens in the repository or execution log.

### Work

* select and record the exact source commit for final 1.3
* verify the working tree and repository state
* prepare a clean disposable release checkout
* review all changes since the last validated RC
* confirm issue and dependency scope remains frozen
* repeat the complete Java 8 release gate
* repeat JDK 17 tests if RC2 is the final baseline
* validate Maven publication with no unintended upload
* verify release POM metadata
* verify source and Javadoc JARs
* verify detached signatures
* verify checksums
* verify Ant binary, source, and examples ZIPs
* verify all-in-one JAR
* verify legal and documentation contents
* repeat launcher, ETL, archive, and Maven-consumer smoke tests
* prepare final release notes
* prepare final GitHub Release text
* prepare final website patch
* prepare the exact irreversible-action runbook
* obtain explicit maintainer go/no-go approval

### Output

* final release candidate selected
* complete evidence set
* final publication runbook
* final website patch ready but not deployed
* explicit go/no-go decision

### Stop conditions

Do not proceed to final publication if:

* namespace ownership is uncertain
* signing identity is temporary or unclear
* release artifacts differ from the validated candidate unexpectedly
* website links do not match final artifact names
* version changes are inconsistent
* any required test or smoke test fails
* the maintainer has not explicitly approved irreversible publication

---

## Chunk 29 — Final Scriptella 1.3 Release

**Status:** Pending

**Target effort:** approximately 2–4 hours

**Reasoning level:** Higher — publication creates immutable public artifacts.

### Purpose

Publish Scriptella 1.3 final from the approved RC baseline.

### Preconditions

* Chunk 28 is complete.
* The maintainer has explicitly approved the final runbook.
* Central credentials and signing identity are valid.
* Final artifacts have passed the complete release gate.
* Website final-release changes are ready.
* Exact release commit and tag names are confirmed.

### Required order

Follow the reviewed operator runbook.

The intended high-level order is:

1. Create the final release-version commit or perform the approved Maven release workflow.

2. Set all reactor versions to `1.3`.

3. Run the final clean build and validation.

4. Create the final tag:

   ```text
   scriptella-parent-1.3
   ```

5. Publish signed Maven artifacts to the Central Portal.

6. Review the Portal deployment.

7. Approve final Central publication.

8. Verify Maven Central coordinates after propagation.

9. Create the GitHub Release using the final tag.

10. Upload:

    * `scriptella-1.3.zip`
    * `scriptella-1.3-src.zip`
    * `scriptella-examples-1.3.zip`
    * any separately approved checksum or signature files

11. Verify every GitHub Release asset.

12. Change the website from RC wording to final-release wording.

13. Enable final release download links.

14. Deploy the website.

15. Verify the live site and release URLs.

16. Advance development to the selected next snapshot version.

17. Record final evidence and deferred work.

### Final website wording

The final website may state that Scriptella 1.3 is released only after:

* final GitHub Release assets exist
* final Maven Central artifacts are published or their status is accurately described
* final download URLs resolve

Remove wording that says:

* 1.3 final has not been published
* 1.2 is the latest generally available release
* the codebase is merely RC1 or RC2

Keep the RC history in the changelog where useful.

### Next snapshot

Choose the next development version explicitly.

Possible options include:

```text
1.3.1-SNAPSHOT
```

for maintenance work, or:

```text
1.4-SNAPSHOT
```

if JDK 17 or other deferred modernization work becomes the next planned release.

Do not choose automatically without recording the release direction.

### Output

* final Scriptella 1.3 tag
* published Maven artifacts
* final GitHub Release
* final release archives
* live final-release website
* next snapshot version
* recorded deferred-work list

### Stop and recovery rules

If Central publication fails before approval:

* do not publish the website as final
* do not claim general availability
* preserve logs and Portal deployment identifiers
* follow the documented recovery path

If GitHub asset publication fails:

* do not deploy final download links
* retain accurate RC or partial-publication wording

If website deployment fails after artifacts are published:

* keep the GitHub Release and Maven publication intact
* restore or retain the previous accurate website
* fix and redeploy the website separately

Never attempt to reuse already published immutable Maven coordinates with changed artifacts.

---

# Explicitly Out of Scope for Release 1.3

Unless they become necessary blockers:

* replacing Ant packaging with Maven
* removing all Ant build files
* supporting every current Ant version
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
* dropping Java 8 for Release 1.3
* broad Java module-system migration
* redesigning the scripting subsystem to obtain JDK 17 support
* replacing Rhino unless a very small compatibility-preserving change is proven
* allowing JDK 17 work to exceed the two-chunk boundary
* publishing final 1.3 wording before final artifacts exist
* coupling website modernization deployment to Central account recovery

## Deferred migration follow-up

* Replace the temporarily bundled Rhino JARs with a deterministic dependency-staging step after Release 1.3. The future step should use Maven dependency tooling to populate a generated build directory; it must not read hard-coded paths from a developer's local Maven repository.
* JDK 17 compatibility is evaluated immediately after RC1. If it fails the bounded feasibility test, move it explicitly to Scriptella 1.4.
* Java 21 compatibility remains future work after JDK 17 unless later planning changes that target.

---

# Recommended Execution Order

Chunks 23–29 execute in order:

* **Chunk 23** — Revise RC1 website wording and release policy.
* **Chunk 24** — Restore StatCounter tracking.
* **Chunk 24A** — Website branding and SEO.
* **Chunk 25** — Merge both `exp-v1.3` branches into `master` and deploy the RC1 website.
* **Chunk 26** — Perform bounded JDK 17 feasibility.
* **Chunk 27** — Implement and publish RC2 only if feasibility succeeds; otherwise defer JDK 17 to 1.4 and skip this chunk.
* **Chunk 28** — Recover Sonatype access; repeat final release gate from a clean checkout.
* **Chunk 29** — Publish immutable Scriptella 1.3 artifacts, GitHub Release, and final website; advance to the next snapshot.

The website migration (completed and pending) may proceed in parallel with dependency and bug work after the representative shell has been validated.

---

# Success Criteria

## RC1 success

RC1 is successful when:

* both development branches are integrated into their default branches
* the modernized site is live
* the site clearly identifies Scriptella 1.3 RC1
* the site does not claim final artifacts are published
* Scriptella 1.2 remains identified as the latest generally available release
* POM versions remain `1.3-SNAPSHOT`
* the existing Java 8 release gate remains valid
* development continues from `master`

## RC2 success

RC2 is successful only if:

* Java 8 compatibility remains intact
* JDK 17 compatibility is implemented within the bounded scope
* all required Java 8 tests pass
* all claimed JDK 17 tests pass
* Rhino and JavaScript behavior has focused regression coverage
* the actual build and runtime matrix is documented
* the website accurately identifies RC2 without claiming final release availability

If these conditions cannot be met within the scope boundary, successful deferral to 1.4 is an acceptable outcome.

## Final 1.3 success

Final 1.3 is successful when:

* final signed Maven artifacts are published
* final immutable coordinates are verified
* final GitHub Release assets are available
* checksums and signatures are verified
* the final tag exists
* final website links resolve
* the website identifies Scriptella 1.3 as generally available
* the next snapshot version is established
* deferred work is recorded

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
