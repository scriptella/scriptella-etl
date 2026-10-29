# Scriptella Release Runbook

Scriptella releases come from a recorded source commit. The tag is
`scriptella-parent-$VERSION`. The build and runtime JDK is 17. One signed
Maven Central bundle is built from that tag with upload suppressed, then
uploaded and published manually. GitHub Release assets come from the same tag.

Small issues are fixed on `master`, on the website, or in a patch release.
After any artifact is public, that tag and those binaries stay. Changing
published artifacts requires a new patch version.

Historical version plans record what happened at the time. They do not add
gates to the current release.

Maintainer steps, the toolchain, signing, and Maven settings are in the
sibling checkout, relative to this repository:

`../scriptella-private/releases/RUNBOOK.md`

Central bundle contents, the upload, and deployment recovery stay in
[RELEASE-PUBLISHING.md](../../RELEASE-PUBLISHING.md).
