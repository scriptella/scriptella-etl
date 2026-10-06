# Scriptella Release Runbook

Release from a recorded source commit, keep completed work, and resume at the
failed step. Small issues should normally be fixed on `master`, corrected on
the website or release notes, or recorded for a follow-up patch release.

This is the current procedure for future releases. Historical version plans
record what happened at the time; they do not add gates to this procedure.
[RELEASE-PUBLISHING.md](../../RELEASE-PUBLISHING.md) contains Central commands
and troubleshooting. Private settings, keys, machine paths, and credential-bearing
logs stay outside Git. Read workspace-local operator notes before executing.

## Decide whether an issue blocks this release

The question is: **must the artifacts for this version change before users can
reasonably use them?** Classify the problem before restarting anything.

| Problem | Action |
| --- | --- |
| Typo, release-note wording, website layout/link, tracking issue, or incomplete evidence | Fix forward or record a follow-up; continue with the same tag and artifacts. |
| Small known product limitation with an acceptable workaround | Record it in release notes and a follow-up issue; the maintainer may ship and fix it in a patch release. |
| Locked signing agent, expired token, network failure, incorrect invocation, or missing upload signatures | Correct the environment/invocation and retry the affected step from the same tag. |
| Unexpected test failure | Investigate; retry an identified environment/flaky-test failure. A real regression needs a severity decision, not an automatic whole-process restart. |
| Broken primary launch/ETL path, wrong version or contents, corrupt archive, required license omission, unacceptable security defect, or incompatible unannounced change | Block the affected publication. Correct the candidate before publishing, or issue a patch if already public. |

The classification table above is authoritative. Documentation issues,
identified flaky tests, signing problems, upload problems, and other operational
failures do not justify abandoning a version or recreating its tag.

Severity depends on user impact, not whether a checklist box failed. Do not
silently waive a product regression: record why it is acceptable and the
follow-up. Signature and checksum failures must be corrected before publication,
but do not by themselves imply a source defect or a new tag.

Once any artifacts are public, preserve the tag and published binaries. Use a
new patch version for artifact fixes. Before publication, recreating a pushed
tag is an exceptional maintainer decision for a genuine release-blocking source
or artifact-content defect classified in the table above; only in that case,
prefer abandoning that version and preparing the next patch. Never silently
force-push or delete a remote tag. Fixes committed on `master` after the tag
are follow-up work; they are not part of that tagged release.

## Authorization and checkpoints

Preparation is local and needs no separate release approval. For an agent-led
release, obtain one explicit authorization covering the named version, tag,
source commit, signing fingerprint, and intended external actions (tag push,
Central upload/publication, GitHub Release; website edits and the Discussions
announcement when included).
Existing explicit authorization for that scope remains valid across retries and pauses. There is
no mandatory second GO ceremony; inspect Central validation and draft assets
before publishing under that authorization. A request only to prepare a
release does not authorize publication.

A source change to the candidate requires review of the changed scope and
updated authorization if it falls outside the original approval. Operational
retries and minor follow-up edits do not invalidate the approved release.

Keep a short release record in the tracking issue or a local non-secret file:

- version, next snapshot, source commit, tag commit, signing fingerprint;
- validation summary and links to applicable CI runs;
- staged asset names and SHA-256 hashes, Central bundle path/hash;
- deployment ID/state, GitHub draft/release URL, announcement URL, website status;
- publication authorization and deferred issues.

Update this record after each completed stage. Resume from it; do not recreate
successful uploads, builds, approvals, or website checks just because the
session ended. A website or tracking-issue delay does not invalidate published
artifacts or prevent development from continuing.

## 1. Preflight once

Use the current supported release toolchain: JDK 17, Maven 3.6+, Ant 1.10.17,
and DTDDoc for distribution documentation. Product class files target Java 17.
Check the selected checkout's configuration if these requirements change.

Confirm a clean source checkout, access to the artifact destinations, an unused
version/tag, private Maven settings with server ID `central`, and the approved
signing key with its public key available from a Central-supported keyserver.
Website readiness and access are not artifact-publication prerequisites.
Website work normally follows artifact publication.

Use non-secret shell variables for paths and release parameters:

```bash
export SOURCE=/path/to/source-checkout
export SETTINGS=/path/to/private-settings.xml
export DTDDOC_HOME=/path/to/dtddoc
export JAVA_HOME=/path/to/jdk17
export VERSION='<release-version>'
export NEXT_VERSION='<next-development-version>'
export TAG="scriptella-parent-$VERSION"
export SIGNING_KEY='<full-signing-key-fingerprint>'
```

Always pass `-s "$SETTINGS"`, including no-upload builds. Never print settings
contents, put tokens or passphrases on command lines, or enable debug tracing
around credentials. Use `gpg-agent` and interactive pinentry for signing.

Perform a detached sign-and-verify probe with the approved key. If the agent
is already unlocked and the probe succeeds, proceed. Ask the operator to
unlock it interactively only when needed; no special `GPG_UNLOCKED` response
or forced agent restart is required. Repeat the probe after an actual signing
failure or agent-cache expiry, rather than stopping before every Maven phase.

## 2. Select the candidate and validate proportionately

Fetch source, review changes since the previous release, and record the
candidate commit. Keep unrelated changes out of this candidate; `master` may
continue independently after tagging. Prepare version/changelog/release notes.
Before tagging, sweep documentation shipped in `scriptella-etl`: Markdown and
README translations, examples, CLI help, and Javadoc where relevant. Describe
shipping features accurately and remove development/unreleased-only wording;
preserve experimental labels where they still apply. Record remaining public
website/documentation work in the release tracking issue. Do not hold a release
for cosmetic wording or every outstanding issue.

The `scriptella.github.io` website does not need to be updated or staged before
artifact publication. An advance branch is optional; normal direct follow-up
after publication is equally valid.

For an ordinary patch release:

- run the Maven reactor tests on JDK 17 and a signed no-upload release build;
- run Ant tests and build distributions if distributing the standalone ZIPs;
- inspect version, artifact inventory, signatures, licenses, archive integrity;
- unpack the actual distribution and run the launcher, a representative ETL,
  and examples; check an isolated Maven consumer;
- test the changed behavior and any affected driver/integration.

Reuse successful CI coverage for unchanged areas at the selected source
revision. Do not manually repeat the full database matrix, dependency audit,
all website pages, or both JDK builds for every patch. Runtime, dependency,
packaging, launcher, or compatibility changes require the relevant additional
checks (including JDK 25 where affected). New compatibility promises require
evidence. Investigate unexpected test-count reductions rather than requiring
historical hard-coded totals.

Run the signed no-upload lifecycle from a disposable candidate checkout with
release versions in the four reactor POMs:

```bash
mvn-lite -s "$SETTINGS" clean deploy \
  -DperformRelease=true -Dcentral.skipPublishing=true \
  -Dgpg.keyname="$SIGNING_KEY"
mvn-lite -s "$SETTINGS" -N -Pant-test-dependencies dependency:copy
ant clean test
ant -Ddtddoc.dir="$DTDDOC_HOME" clean dist
```

The no-upload Central bundle is useful preflight evidence, not a public
release. If source changes, repeat checks affected by that change plus the
final signed build; retain unrelated successful evidence. Environment-only
failures need only the affected command rerun. Never publish a bundle from a
different source revision as though it came from the final tag.

Installer updates may follow publication: pin the checksum to the exact
published ZIP, then test installation in a disposable home. Avoid a pre-tag
checksum/rebuild loop. Leave the existing installer pointing to the previous
working release until the new archive is public. If the tagged installer must
itself install this new version, freeze its candidate ZIP before tagging and
publish that exact verified ZIP; never substitute a rebuild with a new hash.

## 3. Prepare the tag locally

From clean source `master`, prepare without remote pushes:

```bash
cd "$SOURCE"
mvn-lite -s "$SETTINGS" release:prepare \
  -DpushChanges=false \
  -DreleaseVersion="$VERSION" \
  -DdevelopmentVersion="$NEXT_VERSION" \
  -Dtag="$TAG"
```

Inspect the release commit, tag target, and following snapshot commit. Keep
`release.properties` and release-plugin backup POMs locally until publication
is complete; do not commit them. On preparation failure inspect the plugin's
recorded phase and Git state before choosing resume or rollback. Do not
blindly roll back successful preparation for a later upload failure.

## 4. Build once from the tag and stage

Use a fresh detached worktree at the local tag. Run the signed no-upload Maven
build above and the applicable Ant distribution build there. This is the final
artifact set; the earlier candidate gate need not be repeated in its entirety
if only expected release-version/SCM metadata changed. Check those differences.

Preserve the complete generated Central bundle (normally
`target/central-publishing/central-bundle.zip`) outside disposable build output.
Verify that it contains the complete reactor coordinate set and a signature
for every POM/JAR; retain its hash. Build GitHub distribution assets from this
same tag, record SHA-256 hashes, create `.sha256` and detached `.asc` sidecars,
and verify archives, signatures, and functional smoke tests on the staged files.

Keep the staged originals until release completion. Reuse them across network,
Portal, draft, and website failures. If a rebuild is necessary before publication,
replace only the unpublished candidate set, update hashes, and recheck its
contents and smoke behavior. Published binaries are immutable.

Present the candidate and validation summary for publication authorization if
not already granted. Then push the tag and prepared release/snapshot history:

```bash
git -C "$SOURCE" push --atomic origin master "refs/tags/$TAG"
```

Verify the remote tag target and branch. Pushing the next snapshot now lets
normal development resume; it does not change the release tag. If remote
`master` advanced meanwhile, reconcile normally without rewriting history.

## 5. Upload the preserved bundle and inspect the GitHub draft

Preferred Central path: upload the preserved signed bundle through the
[Portal Deployments page](https://central.sonatype.com/publishing/deployments)
using manual publication. This separates build/signing from network/publishing
and avoids another `release:perform` rebuild. See
[Central troubleshooting](../../RELEASE-PUBLISHING.md#recovery-without-restarting).
Record the deployment ID immediately. Wait for `VALIDATED` and inspect the
coordinate inventory and validation result. For timeout/connection loss, check
that deployment before retrying; the upload may have succeeded.

Create a GitHub draft against the existing tag, upload the preserved signed
assets, and inspect notes, tag, filenames, hashes, and draft state. A draft's
`untagged-...` URL is not proof of a wrong tag; inspect its actual tag field.
Correct draft notes/assets in place before publication.

If Central validation fails, identify the cause. Correct signing/invocation or
settings and retry from the same tag. Required POM/artifact-content changes
need a corrected candidate. Keep the failed deployment if asking Sonatype
Support to investigate; otherwise drop it after retaining sanitized errors.
No full release restart is required for an operational deployment failure.

## 6. Publish artifacts

Under the recorded publication authorization:

1. Publish the inspected `VALIDATED` Central deployment.
2. Wait for `PUBLISHED` and public resolution of the reactor coordinates; run
   an isolated consumer using a fresh local Maven repository and public Central.
3. Publish the GitHub draft and download/verify public assets against staged
   hashes and signatures.

Propagation delays mean wait and retry read-only checks. Do not rebuild or
upload identical coordinates to solve them. Pause downstream announcements,
including the Discussions post in section 7, while primary artifacts are
unavailable. Once Central or GitHub artifacts are
public, website failures or unfinished documentation do not invalidate them.
Central, GitHub Release, and the website do not need atomic publication;
temporary website lag is acceptable. Website fixes require ordinary commits,
never a release restart, rebuild, retag, or artifact rollback.

Record public URLs, final deployment state, validation summary, and asset
hashes. Remove disposable worktrees with `git worktree remove` after preserving
assets and non-secret records, and clean release-plugin temporary state.

## 7. Public website and documentation follow-up

Use the release tracking issue as the checklist of accumulated public follow-ups.
After artifact publication:

- publish/finalize release notes as appropriate;
- make a quick sweep of `scriptella.github.io` for shipping features, examples,
  downloads, version references, installation instructions, generated docs where
  needed, and stale development/unreleased-only wording;
- update the installer to the actual published archive and verified checksum;
- check basic consistency with the published release and test changed links
  and installer behavior;
- post a GitHub Discussions announcement in the Announcements category once
  the GitHub Release is public. Follow the shape of the
  [Scriptella 1.5 announcement](https://github.com/scriptella/scriptella-etl/discussions/60):
  version, highlights, and links to that release, the changelog, and the
  Maven Central coordinates. Mention the curl installer or website only when
  those already describe this release. A delayed or corrected post is an
  ordinary edit.

These are post-release follow-ups, not artifact-publication gates. No special
website branch, freeze, synchronization ceremony, or additional release GO
checkpoint is required.
Keep the release tracking issue open until these promised public follow-ups
are complete. Other deferred issues do not require reopening the release.

## Recovery at a glance

| Checkpoint | Resume action |
| --- | --- |
| Candidate build fails | Classify; fix and rerun affected checks. No tag/draft cleanup needed. |
| Local preparation partly completed | Inspect Git and `release.properties`; resume or roll back only preparation as needed. |
| Signing or bundle packaging fails | Unlock/fix invocation, rebuild from the same tag, verify the corrected unpublished bundle. |
| Upload fails or times out | Check Portal for an existing deployment; retry upload of the preserved bundle only if needed. |
| Central `FAILED` | Diagnose; retry operational fixes on the same tag, or correct a genuinely defective unpublished candidate. |
| Central `VALIDATED` | Resume inspection/publication of that deployment; no rebuild. |
| Central `PUBLISHING` or `PUBLISHED` | Check state/public resolution; never upload a replacement. |
| GitHub draft fails | Correct draft/upload missing staged assets; retain the tag and Central progress. |
| Public artifact has a defect | Assess severity, document workaround, prepare a patch release if needed. |
| Website/installer fails | Fix or revert through normal commits; keep artifacts and release tag. |
| Announcement missing or incorrect | Edit or publish the discussion; keep the tag and published artifacts. |

## References

Recheck these when publishing configuration changes or a service error suggests
API/requirements drift; routine patch releases do not need a fresh policy audit.

- [Central Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/)
- [Central Portal API and states](https://central.sonatype.org/publish/publish-portal-api/)
- [Central publication requirements](https://central.sonatype.org/publish/requirements/)
- [Central OpenPGP requirements](https://central.sonatype.org/publish/requirements/gpg/)
- [Maven release guide](https://maven.apache.org/guides/mini/guide-releasing.html)
