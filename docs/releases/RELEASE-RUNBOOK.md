# Scriptella Release Runbook

This runbook defines the guarded, reusable procedure for publishing a
Scriptella release. It coordinates the source repository, Maven Central,
GitHub Releases, distribution archives, and the public website.

[RELEASE-PUBLISHING.md](../../RELEASE-PUBLISHING.md) documents the Maven and
Central configuration. This runbook covers the complete cross-system release
sequence.

The runbook intentionally contains no credentials, private-key locations,
machine-specific paths, release-specific commit IDs, or unpublished deployment
details. Record those values in a private release plan and substitute them only
while executing a release.

## Safety model

A request to prepare or continue release work is not permission to perform an
externally visible or irreversible action.

Two explicit maintainer approvals are required:

1. **Release GO** — after the complete no-upload release gate and before
   pushing the release commit or tag, creating a GitHub Release, or uploading
   to Central.
2. **Publication GO** — after Central reports `VALIDATED` and the draft GitHub
   Release has been completely inspected, immediately before publishing.

Stop when a prerequisite or validation step fails. Do not weaken a gate merely
to finish a release, and never replace artifacts at coordinates that have
already been published.

## Security rules

* Keep Central tokens in private Maven settings under server ID `central`.
* Keep private keys, passphrases, revocation material, authentication headers,
  and credential-bearing logs outside Git.
* Use a durable, passphrase-protected OpenPGP signing key through `gpg-agent`.
* Publish the corresponding public key through a Central-supported keyserver.
* Never put a passphrase or publishing token on a command line.
* Do not enable shell tracing or Maven/GPG debug logging around credentials.
* Review command output before copying it into a public issue or release note.
* Before every commit and push, inspect the staged file list and diff for
  credential files, private logs, temporary assets, and secret-looking values.

Public signing-key fingerprints, Central deployment IDs, artifact hashes, and
publication timestamps may be recorded as release evidence. They are not
credentials. Private keys, passphrases, and tokens must never be recorded.

## Release parameters

The private release plan must resolve and record these placeholders:

| Placeholder | Meaning |
| --- | --- |
| `<release-version>` | Version being published |
| `<next-development-version>` | Snapshot version after the release |
| `<release-tag>` | Final immutable source tag |
| `<release-date>` | Actual publication date |
| `<source-commit>` | Reviewed source baseline before release preparation |
| `<website-commit>` | Reviewed final website change |
| `<signing-key-fingerprint>` | Full approved OpenPGP fingerprint |
| `<central-deployment-id>` | Deployment returned by Central after upload |

Use shell variables only for non-secret values. Inspect their values before
running commands that push or publish:

```bash
export SOURCE=/path/to/source-checkout
export WEBSITE=/path/to/website-checkout
export DTDDOC_HOME=/path/to/dtddoc
export VERSION='<release-version>'
export NEXT_VERSION='<next-development-version>'
export TAG='<release-tag>'
export RELEASE_DATE='<release-date>'
export SIGNING_KEY='<signing-key-fingerprint>'
```

Do not store credential values in shell variables shown in this runbook.

## 1. Prerequisite gate

Confirm all of the following:

* both repositories are clean and synchronized with their remotes;
* the operator has push and release access to the source repository;
* the operator has push and Pages-deployment access to the website repository;
* GitHub CLI authentication is valid without printing its token;
* the Central account can publish the required namespace and view Deployments;
* a current Portal user token is configured privately as Maven server
  `central`;
* the approved secret signing key is available through `gpg-agent`;
* the full public signing key is retrievable from a Central-supported
  keyserver;
* the documented Java, Maven, Ant, and documentation-tool versions are active;
* the final tag and GitHub Release do not already exist; and
* enough uninterrupted time is available to complete and inspect each public
  stage.

Safe verification examples:

```bash
java -version
mvn -version
ant -version
gpg --version
gpg --list-secret-keys --keyid-format long
gh auth status
git -C "$SOURCE" status --short --branch
git -C "$WEBSITE" status --short --branch
git -C "$SOURCE" tag --list "$TAG"
gh release view "$TAG" --repo '<source-repository>'
```

Verify the signing fingerprint in full, perform a detached sign-and-verify
test, and retrieve the public key into a separate temporary keyring when
practical. Stop if the identity, passphrase, signing capability, expiry, or
public-key distribution is uncertain.

## 2. Refresh, review, and freeze

Fetch both repositories and compare every branch used by the release with its
remote. Require clean worktrees and review all commits since the last validated
candidate.

```bash
git -C "$SOURCE" fetch --prune origin
git -C "$WEBSITE" fetch --prune origin
git -C "$SOURCE" status --short --branch
git -C "$WEBSITE" status --short --branch
git -C "$SOURCE" log --oneline --decorate -10
git -C "$WEBSITE" log --oneline --decorate -10
```

Freeze feature, dependency, and cleanup work. Only reviewed release blockers
and final release wording may enter the selected baseline.

Prepare source wording before the final build:

* set the changelog heading and comparison link to the final version, date,
  and tag;
* identify the new version as the latest release in the README;
* update dependency examples to the release version;
* retain accurate compatibility and known-limitation statements; and
* ensure no release-candidate or “not yet published” wording remains in files
  that will be included in final artifacts.

Prepare final website changes on a branch from current website `master`, but
do not merge or deploy that branch yet. It must use the final release date and
the exact planned GitHub asset URLs and Maven coordinates. The live website
must retain its previous accurate status until the artifacts are public.

Record the exact reviewed source and website commits in the private plan.

## 3. Final no-upload release gate

Create a disposable detached worktree from `<source-commit>`. Change only the
reactor versions from the development snapshot to `<release-version>` and
inspect the complete diff.

Run the same release lifecycle used by publication while suppressing Central
upload:

```bash
mvn clean deploy \
  -DperformRelease=true \
  -Dcentral.skipPublishing=true \
  -Dgpg.keyname="$SIGNING_KEY"
ant clean test
ant -Ddtddoc.dir="$DTDDOC_HOME" clean dist
```

Validate all of the following against the expected baseline recorded in the
private plan:

* Maven reactor modules and test totals;
* Ant tests and failure propagation;
* parent and module POM metadata;
* main, source, Javadoc, test, and distribution JARs as applicable;
* detached signatures for every POM and Maven artifact required by Central;
* required checksums;
* binary, source, and examples archives;
* manifests and embedded version strings;
* README, changelog, license, notice, and dependency-license contents;
* dependency versions and byte-identical bundled copies where expected;
* unpacked launcher and representative ETL execution;
* unpacked examples; and
* an isolated Maven consumer resolving the candidate release version.

Verify every ZIP with an archive-integrity tool and every JAR as a readable ZIP
archive. Record hashes for evidence, but do not reuse artifacts from this
disposable validation as final release assets.

Stop on any failure, unexpected source difference, dependency change,
signature problem, archive discrepancy, or smoke-test regression. Fix through
an ordinary reviewed commit, refresh the frozen commits, and repeat the entire
gate.

## 4. First maintainer approval

Present:

* exact source and website commit IDs;
* release version, next version, tag, and actual date;
* full signing-key fingerprint;
* tool versions and complete test totals;
* Maven, Ant, distribution, archive, and smoke-test results;
* final GitHub asset names;
* confirmation of GitHub and Central access; and
* every deviation from this runbook.

Require an unambiguous **GO** naming the source commit, website commit, tag,
and signing fingerprint. Anything else is **NO-GO**.

## 5. Prepare and inspect release history

Run Maven Release Plugin with remote pushing disabled and every version/tag
choice supplied explicitly:

```bash
git -C "$SOURCE" switch master
test -z "$(git -C "$SOURCE" status --porcelain)"
cd "$SOURCE"
mvn --batch-mode release:prepare \
  -DpushChanges=false \
  -DreleaseVersion="$VERSION" \
  -DdevelopmentVersion="$NEXT_VERSION" \
  -Dtag="$TAG"
```

Inspect the resulting graph, release commit, tag, and next-development commit.
Require the tag to contain the release version and the following local commit
to contain the next snapshot version. Confirm that only expected version and
SCM metadata changed.

If preparation fails before push, retain the logs, use `release:rollback` and
`release:clean` when appropriate, and inspect the result. Do not use destructive
Git cleanup on an uninspected worktree.

Push the release commit and tag atomically. Keep the next-development commit
local until all release surfaces have been published and verified:

```bash
export RELEASE_COMMIT="$(git -C "$SOURCE" rev-parse "$TAG^{commit}")"
git -C "$SOURCE" push --atomic origin \
  "$RELEASE_COMMIT:refs/heads/master" \
  "refs/tags/$TAG"
```

Verify the remote branch and tag through GitHub. Record the release, tag, and
local next-development commit IDs.

## 6. Build and stage immutable assets

Create fresh detached worktrees and asset directories from the pushed tag.
Build the final Maven and Ant outputs there. Copy only the approved primary
assets into a dedicated staging directory.

For every primary GitHub asset:

* record its byte size and SHA-256 hash;
* create one detached ASCII-armored signature with the approved key;
* create one `.sha256` sidecar; and
* verify the signature, checksum, and archive integrity.

Repeat the functional smoke tests against these exact files. Do not rebuild or
replace them after recording their hashes and signatures.

Prepare reviewed release notes from the changelog, including upgrade notes and
known limitations. Create a **draft** GitHub Release from the existing tag and
upload the complete approved asset set. Verify the tag, draft state, notes,
asset count, filenames, sizes, hashes, and signatures. Download the draft
assets through an authenticated request when possible and compare them with
the staged originals.

## 7. Upload to Central without publishing

Run `release:perform` with the approved fingerprint. The project must keep
Central automatic publication disabled:

```bash
mvn --batch-mode release:perform \
  -Darguments="-Dgpg.keyname=$SIGNING_KEY"
```

Capture logs privately and redact them before sharing. Record the deployment
ID and URL without recording credentials.

Require Central state `VALIDATED`. `FAILED` is a stop condition. Inspect:

* the deployment name, namespace, version, and complete coordinate set;
* parent and module POM metadata;
* main, source, Javadoc, and attached test artifacts as applicable;
* `.asc` signatures and generated checksums; and
* absence of snapshots and unexpected files.

Download representative files from the validated deployment, verify their
signatures using the published key, and run the isolated consumer smoke test.
Do not publish yet.

If validation fails, preserve the errors and drop the unpublished deployment.
Any decision to remove or recreate a pushed tag requires explicit maintainer
approval and is permitted only while neither Central nor the GitHub Release is
public.

## 8. Second maintainer approval and publication

Present the validated Central deployment, complete draft GitHub Release, tag
commit, hashes, verified signatures, and final smoke-test results. Obtain a
second explicit **GO** immediately before publication.

Publish in this order:

1. Publish the reviewed Central deployment.
2. Wait for `PUBLISHED` and for every coordinate and representative file to
   resolve from public Maven Central.
3. Run a clean Maven consumer against public Central.
4. Publish the GitHub draft as a non-prerelease.
5. Download every public GitHub asset and recheck hashes and signatures.
6. Only then deploy the prepared website changes.

Stop later surfaces if Central does not become publicly resolvable. Published
Central components are immutable.

## 9. Website deployment

Refresh the website remote and require the prepared website commit to be based
on the expected current `master`. Confirm all final download URLs return the
published files, then fast-forward website `master` to the reviewed final
website commit and push it without rewriting history.

Inspect the Pages deployment and verify:

* homepage, downloads, changelog, tutorial, reference, and support pages;
* generated API and DTD documentation;
* downloadable DTDs and other stable public resources;
* every final GitHub asset link and Maven example;
* custom domain and TLS;
* CSS, JavaScript, images, favicon, nested paths, and important anchors;
* internal links and desktop/mobile presentation; and
* absence of stale RC, “in development,” or previous-latest-release wording.

If deployment fails after artifacts are public, revert the website deployment
through an ordinary commit. Do not alter the published release artifacts.

## 10. Advance development and close out

After Central, GitHub, and the website are all public and verified, push the
previously inspected next-development commit to source `master`. Confirm the
remote now contains `<next-development-version>`.

Record sanitized evidence in the release-tracking issue:

* both maintainer GO statements and times;
* release, tag, next-development, and website commit IDs;
* tool versions, test totals, and smoke-test results;
* public signing-key fingerprint;
* final asset names, sizes, and SHA-256 hashes;
* signature verification results;
* Central deployment ID, states, timestamps, and public coordinate checks;
* GitHub Release URL and integrity checks;
* Pages deployment and live-site checks; and
* deferred issues and deviations.

Remove disposable worktrees using `git worktree remove`, retain non-secret
evidence in an approved location, and close the tracking issue only after every
completion condition passes.

## Recovery matrix

| Failure point | Safe response |
| --- | --- |
| Before release/tag push | Roll back and clean local release preparation, fix normally, and repeat every gate. |
| Tag pushed; nothing public | Stop and obtain explicit approval before dropping drafts/deployments or recreating the tag. Never force-push silently. |
| Central `FAILED` or `VALIDATED` | Drop the unpublished deployment, fix, rebuild, and revalidate. |
| Central `PUBLISHED` | Treat the version as immutable; publish a new patch version for artifact defects. |
| GitHub draft problem | Correct or delete the draft before publication; reuse only the preserved signed assets. |
| GitHub Release published | Do not replace public binaries or rewrite the tag; use a patch release for defects. |
| Website deployment problem | Revert the website commit and redeploy without changing release artifacts. |
| Propagation delay | Pause later surfaces, record times, and repeat read-only checks; never rebuild identical coordinates. |

## Authoritative references

Before each release, recheck the current versions of:

* [Central Portal Maven publishing](https://central.sonatype.org/publish/publish-portal-maven/)
* [Central Portal API](https://central.sonatype.org/publish/publish-portal-api/)
* [Central publication requirements](https://central.sonatype.org/publish/requirements/)
* [Central OpenPGP requirements](https://central.sonatype.org/publish/requirements/gpg/)
* [Maven release guide](https://maven.apache.org/guides/mini/guide-releasing.html)
* [GitHub release management](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
* [GitHub release integrity](https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/verify-release-integrity)
* [GitHub Pages publishing](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)
