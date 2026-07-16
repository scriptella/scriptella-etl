# Maven Central publishing

Release 1.3 publishes the Maven reactor through the Sonatype Central Publisher
Portal. The legacy OSSRH service was retired on June 30, 2025, so its repository
URLs and credentials no longer work.

The POM uses Sonatype's `central-publishing-maven-plugin`. Uploads require manual
approval in the Portal: `autoPublish` is intentionally `false`.

## Maintainer prerequisites

Before a real release, a maintainer must:

1. Sign in to the [Central Publisher Portal](https://central.sonatype.com/).
2. Confirm that the migrated `org.scriptella` namespace is present and that the
   account may publish to it. Contact Central Support if the migrated namespace
   is missing.
3. Generate a Portal user token and add it to the maintainer's Maven
   `settings.xml` under server ID `central`.
4. Select a durable, passphrase-protected OpenPGP signing key and publish its
   public key to a Central-supported key server such as `keys.openpgp.org` or
   `keyserver.ubuntu.com`.
5. Build with the documented Java 8 and Maven 3.6+ environment.

Never commit a Portal token, private key, or passphrase.

Example private Maven settings (replace both token values):

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

The GPG plugin normally uses `gpg-agent` to request the key passphrase. Do not
place a clear-text passphrase on the Maven command line or in the project POM.

## Safe local validation

For the normal `1.3-SNAPSHOT` development version, run the complete publication
lifecycle without uploading:

```bash
JAVA_HOME=/path/to/jdk8 \
  mvn clean deploy -Dcentral.skipPublishing=true
```

Before releasing, perform the same check in a disposable checkout whose POM
version has been changed to the release version. Enable release signing while
keeping upload disabled:

```bash
JAVA_HOME=/path/to/jdk8 \
  mvn clean deploy \
  -DperformRelease=true \
  -Dcentral.skipPublishing=true
```

The second command must produce and sign the parent POM plus the main, source,
Javadoc, and attached test artifacts. Verify representative signatures with:

```bash
gpg --verify artifact.jar.asc artifact.jar
```

The Central plugin generates MD5, SHA-1, SHA-256, and SHA-512 checksums during
staging. Central requires MD5 and SHA-1; SHA-256 and SHA-512 are also retained.

## Release flow

Run these only from a clean, pushed release branch with the real Portal token
and signing key available:

```bash
JAVA_HOME=/path/to/jdk8 mvn release:prepare
JAVA_HOME=/path/to/jdk8 mvn release:perform
```

`release:prepare` verifies the reactor, changes the POMs to `1.3`, commits and
tags the release, then advances the branch to the next development version.
`release:perform` checks out the tag, signs the artifacts, and runs `deploy`.

Because automatic publishing is disabled, a successful deploy uploads a
validation deployment but does not make it public. Inspect the deployment in
the Central Portal, resolve any validation errors, and explicitly choose
**Publish** only after the release-candidate checklist is complete. Published
Maven Central coordinates are immutable.

## Required artifact and metadata checks

The release must include:

* the parent POM and all three module POMs;
* main JARs for Core, Drivers, and Tools;
* source and Javadoc JARs for every main JAR;
* the Core test JAR used by the reactor;
* an ASCII-armored `.asc` signature for every POM and JAR;
* generated checksums for every POM and JAR.

Each effective POM must retain the project name, description, HTTPS project
URL, Apache-2.0 license, developer information, and SCM coordinates. Release
dependencies must already be available from Maven Central and may not use
`-SNAPSHOT` versions.

Official references:

* [Central Portal Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/)
* [Central publication requirements](https://central.sonatype.org/publish/requirements/)
* [Central GPG requirements](https://central.sonatype.org/publish/requirements/gpg/)
* [OSSRH retirement and migration](https://central.sonatype.org/pages/ossrh-eol/)
