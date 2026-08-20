# Issue #54 — One-Command Curl Installer Plan

## Goal

Provide a small POSIX shell installer that makes the published Scriptella ZIP
distribution usable through either of these commands:

```sh
curl -fsSL https://scriptella.org/install.sh | sh
scriptella path/to/file.etl.xml
```

The installer must preserve and invoke the distribution's existing
`bin/scriptella.sh` launcher. The shorter `scriptella` command is a symlink to
that launcher, not a second launcher implementation.

## Agreed installation contract

- [x] Keep the canonical installer at repository root as `install.sh` so each
  source release tag contains the exact installer for that release.
- [x] Keep the release version, GitHub tag, archive URL, archive filename, and
  SHA-256 value explicit and reviewable in `install.sh`.
- [x] On development `master`, install the latest published stable release
  (initially 1.4), even though the reactor version is `1.5-SNAPSHOT`.
- [ ] During release preparation, update the installer to the version being
  released before creating its tag; after the release, leave it pinned to that
  now-current stable version while the POMs advance to the next snapshot.
- [x] Install into the unversioned user-local directory
  `${HOME}/.local/scriptella` without requiring root privileges.
- [x] Preserve the ZIP's contents and relative layout, including
  `scriptella.jar`, `lib/`, and `bin/scriptella.sh`.
- [x] Expose both `scriptella.sh` and `scriptella` as symlinks to the packaged
  `bin/scriptella.sh`; do not add another launcher script.
- [x] Treat Java as a runtime prerequisite rather than an installation
  prerequisite: installation succeeds without Java, but prints a concise
  warning when Java 17 or newer is not available.
- [ ] Align issue #54's Java-not-found test and hard-failure acceptance wording
  with the non-blocking Java policy before closing the issue.

## Phase 1 — Installer and isolated behavior tests

This phase is owned entirely by `scriptella-etl` and can be reviewed and merged
before the public website changes.

### Installer implementation

- [x] Add a small POSIX `sh` implementation at `install.sh` using `set -eu` and
  no non-system runtime, package manager, Maven, or Gradle.
- [x] Check for the tools actually required by the selected path, including a
  downloader, `unzip`, temporary-directory support, and a supported SHA-256
  command (`sha256sum`, `shasum -a 256`, or an explicitly documented fallback).
- [x] Download the exact binary distribution ZIP into a temporary staging
  directory and fail on HTTP, transport, or incomplete-download errors.
- [x] Verify the archive against the checksum embedded in the tagged installer
  before extraction.
- [x] Inspect ZIP entry names before extraction and reject any absolute path,
  parent-directory (`..`) traversal component, or other entry that could be
  written outside the temporary extraction root.
- [x] Run an archive-integrity check and reject unexpected archive structure,
  including a missing single distribution root, `lib/`, or executable
  `bin/scriptella.sh`.
- [x] Extract only into temporary staging and validate the staged launcher
  before touching an existing installation.
- [x] Replace `${HOME}/.local/scriptella` predictably, retaining enough prior
  state to restore it if final placement fails and cleaning temporary files on
  exit or interruption.
- [x] Preserve the executable mode of the packaged `bin/scriptella.sh`.
- [x] Avoid leaving a partially installed destination after download,
  checksum, extraction, validation, or placement failures.

### PATH and command exposure

- [x] Inspect existing `PATH` entries in order and identify safe, absolute,
  existing, writable directories located inside the user's home directory.
- [x] Prefer conventional general-purpose entries such as `${HOME}/.local/bin`
  and `${HOME}/bin`; use another safe home-contained `PATH` directory only as
  a fallback.
- [x] In the selected directory, create both `scriptella.sh` and `scriptella`
  symlinks targeting `${HOME}/.local/scriptella/bin/scriptella.sh`.
- [x] Replace only links previously managed by this installer; never overwrite
  an unrelated file, directory, or symlink with either command name.
- [x] If no suitable home-directory entry is already on `PATH`, create
  `${HOME}/.local/bin`, place both links there, and append one guarded,
  idempotent PATH stanza to an appropriate user startup file.
- [x] Make repeated runs preserve a single PATH stanza and refresh only the
  installer-managed links.
- [x] Explain when a new shell or an explicit startup-file reload is needed.

### Runtime notice and output

- [x] Detect a usable Java command through the same broad conventions as the
  packaged launcher (`JAVACMD`, a valid `JAVA_HOME`, or `java` on `PATH`).
- [x] If practical, identify the Java major version and warn when it is absent
  or older than 17; do not download Java and do not fail installation.
- [x] Print concise success output containing the install directory, exposed
  command names, selected link directory, and one example invocation.
- [x] Keep normal output quiet enough for the one-command installation flow and
  send actionable failures to standard error.

### Automated tests

- [x] Add an installer integration test under
  `tools/src/test/installer/` using a generated miniature ZIP fixture with the
  real Scriptella distribution layout and launcher name.
- [x] Provide narrowly scoped test overrides for archive URL, expected
  checksum, home directory, PATH, and command fixtures so tests need no network
  access and never modify the developer's real home directory.
- [x] Cover clean installation and preservation of the expected ZIP layout.
- [x] Cover successful download, download failure, truncated/invalid ZIP,
  checksum failure, and malicious ZIPs containing `../` traversal, absolute
  path entries, or symbolic links; confirm malicious entries are rejected
  before extraction and cannot create files outside the temporary extraction
  root.
- [x] Cover launcher validation, executable mode, and execution of a simple ETL
  through the installed distribution or a real locally built distribution.
- [x] Cover selection of an existing home-directory PATH entry and creation of
  both `scriptella.sh` and `scriptella` links.
- [x] Cover the no-suitable-PATH-entry fallback and idempotent startup-file
  modification.
- [x] Cover collisions with unrelated files and symlinks without overwriting
  them.
- [x] Cover repeated installation and replacement of a prior managed install.
- [x] Cover missing and pre-Java-17 runtimes as successful installs with
  warnings.
- [x] Cover failures at each pre-install stage and confirm that an existing
  installation remains intact and no partial destination remains.
- [x] Add the installer suite to the existing Ant distribution-test workflow or
  another repository-standard CI entry point.
- [x] Run shell syntax checks, the focused installer suite, the existing
  distribution contract test, and `git diff --check`.

### Phase 1 completion gate

- [x] Review the installer for quoting, whitespace-containing home paths,
  cleanup traps, archive traversal exposure, and unsafe symlink replacement.
- [x] Confirm neither Maven nor Gradle is invoked and no JAR/classpath logic is
  duplicated.
- [x] Confirm a clean checkout can test the installer without network access or
  writes outside its disposable test home.
- [x] Record the exact stable release ZIP checksum used by the initial
  installer and how it was independently verified.

Phase-1 checksum evidence: Scriptella 1.4 uses
`e96900158e0b2823b48954c33901a7867f8b9f82eb9510089d24f91a674e66ee` for
`scriptella-1.4.zip`, independently checked against the GitHub Release API
asset digest for tag `scriptella-parent-1.4`.

## Phase 2 — Publication infrastructure and release lifecycle

This phase spans `scriptella-etl` and the sibling `scriptella.github.io`
repository. It is split into publication of the already-tested installer for
the published 1.4 release and the later 1.5 release cutover.

### Phase 2A — Publish the stable 1.4 installer now

#### Website integration

- [ ] Copy the reviewed canonical 1.4 `install.sh` to the website root so it
  is served as `https://scriptella.org/install.sh`.
- [ ] Verify the website copy is byte-for-byte identical to the approved source
  installer for 1.4.
- [ ] Add a prominent copyable curl command to `download.html`, labeled as a
  new or experimental alternative installation method.
- [ ] Document `scriptella` and `scriptella.sh`, the default install directory,
  the PATH/symlink behavior, the Java 17 runtime requirement, and how to reload
  the shell when necessary.
- [ ] Keep the existing ZIP download table and manual `java -jar` instructions
  available as alternatives.
- [ ] Update the source README installation section consistently without
  implying that Java is installed by the installer.

#### Checksum and release-process alignment

- [ ] Resolve the 1.4 publication mismatch: its release notes claim `.sha256`
  and signature sidecars, while the public GitHub Release exposes only the
  published ZIP files.
- [ ] Update the release runbook so every future binary ZIP has an approved
  SHA-256 value before the corresponding installer is tagged.
- [ ] Add release checklist items to update the installer version, tag, URL,
  filename, Java baseline when changed, embedded checksum, and smoke evidence.
- [ ] Test the canonical 1.4 installer against the exact published 1.4 ZIP
  before copying it to the website.

#### Public smoke validation

- [ ] Verify `https://scriptella.org/install.sh` is served as the expected
  shell source over HTTPS with no HTML fallback or redirect surprise.
- [ ] Run the documented curl command in a disposable home directory on at
  least Linux and macOS environments representative of supported Unix-like
  users.
- [ ] Verify both `scriptella --version` and `scriptella.sh --version` through
  the selected PATH directory.
- [ ] Execute a small ETL file through the installed launcher.
- [ ] Re-run the public installer and confirm idempotent installation, links,
  and startup-file behavior.
- [ ] Simulate or verify actionable behavior when Java is missing without
  treating extraction as failed.
- [ ] Confirm the manual ZIP installation path still works and remains visible
  on the website.

#### Phase 2A completion gate

- [ ] Record sanitized 1.4 smoke-test evidence.
- [ ] Confirm the live installer, website documentation, and published 1.4
  release asset identify the same stable version.

### Phase 2B — Cut over to 1.5 during release preparation

- [ ] Update `install.sh` from 1.4 to 1.5, including the release version, tag,
  archive URL, filename, Java baseline if changed, and embedded checksum.
- [ ] Test the 1.5 installer against the exact staged 1.5 ZIP before release
  publication.
- [ ] Create and tag the 1.5 release, then verify its public ZIP is reachable
  and matches the staged artifact and approved checksum.
- [ ] Keep the website on the working 1.4 installer until the 1.5 archive is
  public and independently downloadable.
- [ ] Copy the tested 1.5 installer to the website and verify byte-for-byte
  identity with the canonical source.
- [ ] Run the final public curl smoke test, including both launcher names and
  a small ETL execution.
- [ ] Verify that the live installer, website documentation, GitHub release
  asset, release tag, and embedded checksum all identify the same stable
  version.

#### Phase 2B completion gate

- [ ] Confirm all issue acceptance criteria match the final Java and command
  alias decisions.
- [ ] Record sanitized 1.5 smoke-test evidence and close issue #54 only after
  the live one-command flow and both launcher names succeed.

## Non-goals retained

- [ ] Do not introduce runtime version switching, self-update, or automatic
  upgrade management.
- [ ] Do not download Java, individual dependency JARs, or Maven/Gradle
  dependencies.
- [ ] Do not rearrange distribution libraries or reconstruct its classpath.
- [ ] Do not replace `bin/scriptella.sh` or implement Windows installation in
  this issue.
