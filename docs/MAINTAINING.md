# Maintaining Scriptella

This page is the entry point for maintainers. The basic source build remains in
the user-facing README; this page collects the documentation, release, and
publication procedures.

## Generated website documentation

The published website currently contains the Scriptella 1.3 documentation.
Use the [generated-docs sync instructions](site/README.md) when refreshing the
published API or DTD documentation. Rebuild those docs from a 1.3 checkout or
worktree with Java 8; do not use current `master` output to refresh the 1.3
site.

## Release and publication

Use the [release runbook](releases/RELEASE-RUNBOOK.md) for the complete,
approval-gated release sequence across the source repository, Maven Central,
GitHub Releases, distribution archives, and website.

The Maven Central configuration and artifact publication details are in
[`RELEASE-PUBLISHING.md`](../RELEASE-PUBLISHING.md).

## Current planning

* [Scriptella 1.4 release plan](releases/1.4/release-1.4-plan.md)
* [Scriptella 1.5 release plan](releases/1.5/release-1.5-plan.md)
* [Release history](../CHANGELOG.md)
* [Issue #58 security and JDK 25 disposition](security/dependabot-2026-08.md)
