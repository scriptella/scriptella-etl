# Maintaining Scriptella

This page is the entry point for maintainers. The basic source build remains in
the user-facing README; this page collects the documentation, release, and
publication procedures.

## Generated website documentation

Use the [generated-docs sync instructions](https://github.com/scriptella/scriptella-etl/blob/master/docs/site/README.md)
to rebuild API and DTD documentation from the exact released tag with its
required JDK. Publish them at the website step in
`../scriptella-private/releases/RUNBOOK.md`, relative to this repository.
That procedure is available only to maintainers with the private checkout.

## Release and publication

The release model is the [release runbook](releases/RELEASE-RUNBOOK.md):
tag `scriptella-parent-$VERSION`, JDK 17, one signed Central bundle from that
tag, manual publication, and fix-forward after artifacts are public.

Maintainer steps are in the sibling checkout
`../scriptella-private/releases/RUNBOOK.md`, relative to this repository.
That procedure is available only to maintainers with the private checkout.

Maven Central bundle contents and deployment recovery are in
[`RELEASE-PUBLISHING.md`](../RELEASE-PUBLISHING.md).

## Release history

* [Scriptella 1.4 release plan](https://github.com/scriptella/scriptella-etl/blob/master/docs/releases/1.4/release-1.4-plan.md)
* [Scriptella 1.5 release plan](https://github.com/scriptella/scriptella-etl/blob/master/docs/releases/1.5/release-1.5-plan.md)
* [Release history](../CHANGELOG.md)
* [Issue #58 security and JDK 25 disposition](https://github.com/scriptella/scriptella-etl/blob/master/docs/security/dependabot-2026-08.md)
