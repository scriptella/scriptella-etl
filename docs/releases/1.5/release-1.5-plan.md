# Release 1.5 Plan

**Status:** Release preparation

**Umbrella issue:** [#55 — Scriptella 1.5 Release](https://github.com/scriptella/scriptella-etl/issues/55)

**Primary workstreams:** [#48 — First-class support for modern databases](https://github.com/scriptella/scriptella-etl/issues/48), [#56 — Update and verify curl installer for Scriptella 1.5](https://github.com/scriptella/scriptella-etl/issues/56), and [#58 — dependency security remediation](https://github.com/scriptella/scriptella-etl/issues/58)

## Purpose

Release Scriptella 1.5 as a focused compatibility and usability update: make
Scriptella easier to install and immediately useful with current PostgreSQL,
MariaDB, and MySQL JDBC stacks while retaining the Java 17 runtime baseline.

This plan records scope and readiness. The guarded publication sequence is in
the [release runbook](../RELEASE-RUNBOOK.md). Neither this plan nor completion
of a preparation item authorizes a tag push, Central upload, GitHub Release,
or website deployment.

## Readiness assessment (August 28, 2026)

No known product-code fix remains from the database modernization or
dependency-security workstreams:

* modern database adapters, aliases, URL autodetection, diagnostics, focused
  tests, and public H2 contract tests are merged to `master`;
* PostgreSQL, MariaDB, H2, and the targeted MySQL lane passed the packaged
  `1.5` candidate checks recorded by the private validation harness;
* the Velocity, Spring, and related runtime dependency remediation is merged,
  with JDK 17 and JDK 25 release gates documented; and
* the Maven reactor test suite passes on the current `master` worktree.

The unchecked items in the private certification roadmap are deferred or
future formal-certification scope. They are not 1.5 release blockers. Issue
#48 should be reconciled with the completed implementation evidence and closed
as part of release preparation.

The recorded database run used an earlier candidate revision. Run the final
applicable packaged matrix once more against the exact frozen release source
commit if product content changes after that candidate, or if the maintainer
wants the final release evidence tied directly to the release commit.

## Release scope

### Included

* First-class PostgreSQL and MariaDB aliases and documented validation targets.
* A deliberately narrow MySQL 8.4.11 / Connector/J 26.7.0 validation lane.
* Current preferred Oracle and SQL Server driver classes, documented as
  provisional rather than real-server validated.
* Credential-safe JDBC driver-loading diagnostics.
* H2-only public JDBC commit, rollback, parameter-binding, and value-flow tests.
* Velocity 2.4.1, Commons Lang 3.20.0, Spring Framework 7.0.9, and the related
  standalone runtime dependency updates.
* Experimental `--check` command documentation.
* The existing ZIP-based curl installer updated for the final 1.5 artifact.
* Website and generated reference documentation updated for 1.5.

### Not release blockers

* Formal or regulatory certification.
* A broad MySQL version/driver matrix.
* Oracle or SQL Server real-server validation.
* Vendor-specific type coverage beyond the portable fixture.
* The Table Copy Wizard or other onboarding experiments.
* Commons JEXL 3 migration.

## Preparation checklist

### 1. Freeze scope and tracking

- [ ] Review commits since `scriptella-parent-1.4` and classify every change as
      included, deferred, or a release blocker.
- [ ] Reconcile issue #48 with the completed implementation and private
      packaged-validation evidence; close it when its public acceptance wording
      is accurate.
- [ ] Confirm issue #58 remains remediated with no open release-blocking alert.
- [ ] Keep unrelated features, dependency changes, and cleanup out of the
      frozen release baseline.

### 2. Final source wording

- [ ] Convert `CHANGELOG.md` Unreleased content into the final 1.5 section with
      the actual release date and comparison link.
- [ ] Update `README.md` latest-release wording, requirements table, Maven
      examples, and installer wording to 1.5.
- [ ] Remove candidate, rehearsal, and not-yet-published language from content
      shipped in the final artifacts while preserving honest validation tiers.
- [ ] Prepare reviewed GitHub release notes from the changelog, including the
      Java baseline, database support tiers, dependency upgrades, and upgrade
      notes.

### 3. Website preparation

- [ ] Create `release-1.5-site` from current website `master`; do not merge or
      deploy it before the release artifacts are public.
- [ ] Update the homepage, download page, changelog, tutorial, CLI reference,
      driver reference, and installer wording for 1.5.
- [ ] Regenerate and review API and DTD documentation from the frozen 1.5
      source where required.
- [ ] Use the final release date, tag, Maven coordinates, and exact GitHub asset
      names in the deployable website commit.
- [ ] Record the reviewed website commit in the private execution plan.

### 4. Final no-upload gate

- [ ] Select and record the exact source commit, website commit, release date,
      next development version, tag, signing fingerprint, and expected assets
      in the private execution plan.
- [ ] Run the complete signed, no-upload Maven release lifecycle on JDK 17 and
      JDK 25 as required by the runbook.
- [ ] Run Ant tests and the DTDDoc distribution build with the approved tool
      versions.
- [ ] Validate artifact inventory, licenses, dependency versions, signatures,
      checksums, archive integrity, Java 17 class-file baseline, launcher,
      representative ETL, examples, and isolated Maven consumer.
- [ ] Run the final packaged PostgreSQL/MariaDB/H2 matrix and targeted MySQL
      lane when required by the frozen-source rule above; record the exact
      source and artifact hashes.
- [ ] Freeze the installer candidate ZIP, record its SHA-256, and update the
      source `install.sh` before tagging as required by the runbook.

### 5. Approval and publication

- [ ] Present the complete no-upload evidence and obtain the first explicit
      Release GO before any external release action.
- [ ] Run and inspect Maven release preparation locally with pushing disabled.
- [ ] Push the approved release commit and tag atomically, then build and stage
      immutable signed GitHub assets from the tag.
- [ ] Create and inspect the draft GitHub Release.
- [ ] Upload to Central without automatic publication and require `VALIDATED`.
- [ ] Obtain the second explicit Publication GO.
- [ ] Publish Central, verify public resolution, publish and verify the GitHub
      Release, and only then merge/deploy the website branch.

### 6. Post-publication verification

- [ ] Update the website installer to the published 1.5 ZIP URL and pinned
      SHA-256, then run the clean disposable-home test required by #56.
- [ ] Verify homepage, downloads, changelog, tutorial, reference, API/DTD docs,
      Maven examples, installer instructions, links, assets, and responsive
      presentation on the deployed site.
- [ ] Push the next-development commit only after release surfaces are public
      and verified.
- [ ] Record sanitized evidence in issue #55; close #56 and #55 when their
      final checks pass.

## Release parameters

The workspace-local private execution plan must resolve these before the final
gate:

| Parameter | Planned value |
| --- | --- |
| Release version | `1.5` |
| Next development version | To decide before release preparation |
| Release tag | `scriptella-parent-1.5` |
| Release date | Set on the final release day |
| Source commit | Freeze after final review |
| Website branch | `release-1.5-site` |
| Website commit | Freeze after final website review |
| Binary ZIP SHA-256 | Record from the approved frozen 1.5 ZIP |
| Signing fingerprint | Record and verify privately |
| Central deployment ID | Record after upload |

## Definition of done

Scriptella 1.5 is complete only when the immutable artifacts resolve from
Maven Central and GitHub, their signatures and hashes verify, the published
installer passes against the exact 1.5 ZIP, the prepared website is deployed
and validated, the next development version is pushed, and the release tracker
contains the sanitized evidence required by the release runbook.
