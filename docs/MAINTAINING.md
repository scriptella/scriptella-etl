# Maintaining Scriptella

This page is the entry point for maintainers. The basic source build remains in
the user-facing README; this page collects the documentation, release, and
publication procedures.

## Generated website documentation

Use the [generated-docs sync instructions](https://github.com/scriptella/scriptella-etl/blob/master/docs/site/README.md)
to rebuild API and DTD documentation from the exact released tag with its
required JDK. Publish them when the release runbook reaches the website step.

## Release and publication

Use the [release runbook](https://github.com/scriptella/scriptella-etl/blob/master/docs/releases/RELEASE-RUNBOOK.md) for the complete,
approval-gated release sequence across the source repository, Maven Central,
GitHub Releases, distribution archives, and website.

The Maven Central configuration and artifact publication details are in
[`RELEASE-PUBLISHING.md`](../RELEASE-PUBLISHING.md).

## Release history

* [Scriptella 1.4 release plan](https://github.com/scriptella/scriptella-etl/blob/master/docs/releases/1.4/release-1.4-plan.md)
* [Scriptella 1.5 release plan](https://github.com/scriptella/scriptella-etl/blob/master/docs/releases/1.5/release-1.5-plan.md)
* [Release history](../CHANGELOG.md)
* [Issue #58 security and JDK 25 disposition](https://github.com/scriptella/scriptella-etl/blob/master/docs/security/dependabot-2026-08.md)
