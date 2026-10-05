# Maven Central Publishing

The [release runbook](docs/releases/RELEASE-RUNBOOK.md) owns the release and
fix-forward policy. This document covers building, uploading, and recovering
the Maven deployment. Historical RC and release plans are not current gates.

Scriptella uses the Central Publisher Portal and
`central-publishing-maven-plugin` (currently 0.11.0), not legacy Nexus/OSSRH
staging. The POM keeps `autoPublish=false`: upload and publication are separate.

## Prepare once

- Confirm Portal access to namespace `org.scriptella`.
- Configure a Portal user token in private Maven settings under server ID
  `central`. Always pass that settings file explicitly, even without upload.
- Select JDK 17 and Maven 3.6+; compiler output remains Java 17-compatible.
- Confirm the approved signing key and its published public key; use
  `gpg-agent` with interactive pinentry. Probe signing once and unlock only
  when needed. Never put a token or passphrase on a command line.

Use the private operator notes for settings path and signing identity. Private
settings, keys, credential-bearing logs, and tokens must remain outside Git.
Do not use Maven debug output or shell tracing around credentials.

Example private settings structure:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>PORTAL_TOKEN_USERNAME</username>
      <password>PORTAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

## Preferred path: build a bundle, then upload it

From a fresh checkout of the exact release tag (all POMs at the release version),
with the non-secret variables from the runbook:

```bash
mvn-lite -s "$SETTINGS" clean deploy \
  -DperformRelease=true \
  -Dcentral.skipPublishing=true \
  -Dgpg.keyname="$SIGNING_KEY"
```

`skipPublishing` creates the bundle while suppressing upload and publication.
Preserve `target/central-publishing/central-bundle.zip` and its SHA-256 outside
build output before another `clean`. Inspect its complete reactor inventory:

- parent POM and Core, Drivers, Tools POMs;
- main, source, and Javadoc JARs for all three modules;
- attached Core test JAR;
- ASCII-armored `.asc` signatures for every POM and JAR;
- generated checksums (the plugin supplies these).

Check version, required POM metadata (name, description, URL, license,
developers, SCM), dependency versions, and absence of snapshots. Verify
signatures and run the release smoke checks. A sources or Javadoc attachment
failure is a build/configuration problem; uploading again cannot fix it.

After release publication is authorized, upload this exact ZIP at
[Portal Deployments](https://central.sonatype.com/publishing/deployments),
keeping manual publication selected. Record its deployment ID, bundle hash,
and state. Inspect `VALIDATED` before clicking Publish. A successful local
build is not proof that credentials or namespace authorization are valid;
the Portal upload/validation checks those separately.

This avoids rebuilding and signing just to retry an upload. The same preserved
bundle can be uploaded again when an operational failure genuinely requires
it. Check whether an earlier upload succeeded before submitting a duplicate.

## Alternative: Maven-managed upload

If using the Release Plugin after `release:prepare`, retain its local
`release.properties` and backups. Use the local tag to avoid requiring SSH
SCM authentication, and propagate both release signing flags:

```bash
mvn-lite -s "$SETTINGS" release:perform \
  -DlocalCheckout=true \
  -Darguments="-DperformRelease=true -Dgpg.keyname=$SIGNING_KEY"
```

This rebuilds the tag and uploads to Central. It requires publication
authorization just like a Portal upload. Keep `autoPublish=false` in the POM.
A failed `release:perform` does not require rerunning `release:prepare`,
changing the version, or deleting the tag. For subsequent operational retries,
prefer the preserved bundle over another rebuild.

## Recovery without restarting

| Symptom/state | Next action |
| --- | --- |
| GPG cannot sign / `Inappropriate ioctl for device` | Unlock the approved key interactively, probe it, rerun the signed build from the same tag. |
| SSH `Permission denied (publickey)` | Use `-DlocalCheckout=true`, or use the preferred bundle path. |
| Missing `.asc` files | Ensure `-DperformRelease=true` and key fingerprint reach the build; rebuild the unpublished bundle from the same tag. |
| Authentication/namespace error | Fix the private Portal token/account access; retry upload of the existing bundle. |
| Timeout or lost connection | Look up the recorded deployment or Portal list first; a successful upload may still be processing. |
| `PENDING` / `VALIDATING` | Wait and inspect that deployment. |
| `FAILED` | Retain sanitized errors; fix the cause. Retain deployment files for Support, otherwise drop the unpublished deployment before retrying. |
| `VALIDATED` | Inspect and publish the existing deployment under release authorization. |
| `PUBLISHING` | Wait; do not resubmit. |
| `PUBLISHED` but consumer cannot resolve | Check public availability/propagation with a fresh Maven repository; keep the same deployment and artifacts. |
| Wrong artifact contents or required POM metadata | Correct the unpublished candidate under the runbook's blocker policy; published coordinates require a new patch version. |

After publication, check every reactor coordinate and run an isolated consumer
against public Central. Published coordinates are immutable. Website,
installer, release-note, and follow-up issue fixes continue independently.

## Official references

- [Central Portal Maven plugin: bundle generation and manual publication](https://central.sonatype.org/publish/publish-portal-maven/)
- [Central Portal API: deployment states and recovery](https://central.sonatype.org/publish/publish-portal-api/)
- [Central publication requirements](https://central.sonatype.org/publish/requirements/)
- [Central GPG requirements](https://central.sonatype.org/publish/requirements/gpg/)
- [OSSRH retirement](https://central.sonatype.org/pages/ossrh-eol/)
