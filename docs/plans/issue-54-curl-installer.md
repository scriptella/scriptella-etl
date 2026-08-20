# Issue #54 — One-Command Curl Installer Plan

## Goal

Provide a small POSIX shell installer that makes the published Scriptella ZIP
distribution usable through either of these commands:

```sh
curl -fsSL https://scriptella.org/install.sh | sh
scriptella.sh path/to/file.etl.xml
```

The installer must preserve and invoke the distribution's existing
`bin/scriptella.sh` launcher. The 1.4 installer does not create an external
launcher alias.

## Agreed installation contract

- [x] Keep the canonical installer at repository root as `install.sh` so each
  current stable target has one explicit, reviewable installer.
- [x] Keep the release version, GitHub tag, archive URL, archive filename, and
  SHA-256 value explicit and reviewable in `install.sh`.
- [x] On development `master`, install the latest published stable release
  (initially 1.4), even though the reactor version is `1.5-SNAPSHOT`.
- [ ] Keep the 1.4 installer during 1.5 release preparation. After the 1.5
  archive is public, revisit the preserved `installer-1.5` implementation,
  test it against that actual archive, and only then replace the public
  installer and website copy.
- [x] Install into the unversioned user-local directory
  `${HOME}/.local/scriptella` without requiring root privileges.
- [x] Preserve the ZIP's contents and relative layout, including
  `scriptella.jar`, `lib/`, and `bin/scriptella.sh`.
- [x] Expose the packaged `bin/scriptella.sh` through the installation's own
  `bin` directory; do not create external launcher symlinks or another
  launcher script.
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

- [x] Use `${HOME}/.local/scriptella/bin` as the only command directory.
- [x] If that directory is not already on `PATH`, append one guarded,
  idempotent PATH stanza to an appropriate existing user startup file, or
  print the exact manual PATH command when choosing a file is unsafe.
- [x] Do not create shell startup files. For Bash this can change startup-file
  precedence; for zsh, creating `.zprofile` does not configure non-login
  interactive shells. Print manual guidance when no suitable file exists.
- [x] Make repeated runs preserve a single PATH stanza and never create
  external launcher symlinks.
- [x] Explain when a new shell or an explicit startup-file reload is needed.

### Runtime notice and output

- [x] Detect a usable Java command through the same broad conventions as the
  packaged launcher (`JAVACMD`, a valid `JAVA_HOME`, or `java` on `PATH`).
- [x] If practical, identify the Java major version and warn when it is absent
  or older than 17; do not download Java and do not fail installation.
- [x] Print concise success output containing the install directory, command
  path, PATH status, and one `scriptella.sh` example invocation.
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
- [x] Cover an existing `${HOME}/.local/scriptella/bin` PATH entry, addition to
  existing Bash/zsh startup files, and manual guidance when no suitable file
  exists.
- [x] Cover idempotent startup-file modification without external links.
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

## Phase 2 — Current 1.4 publication and future 1.5 work

This phase spans `scriptella-etl` and the sibling `scriptella.github.io`
repository. The current 1.4 stage is deliberately simple because the
published launcher predates external symlink resolution.

### Current stable stage — publish the 1.4 installer

- [x] Target the exact published `scriptella-1.4.zip` and its approved
  SHA-256.
- [x] Install into `${HOME}/.local/scriptella` while preserving the ZIP
  layout exactly.
- [x] Put only `${HOME}/.local/scriptella/bin` on `PATH`.
- [x] Guarantee `scriptella.sh`; do not create external `scriptella` or
  `scriptella.sh` symlinks.
- [x] Treat a legacy installer PATH marker as insufficient unless the actual
  `${HOME}/.local/scriptella/bin` stanza is present.
- [x] Verify the exact published 1.4 ZIP with `scriptella.sh --version` and a
  small ETL through the installed PATH layout.
- [ ] Copy the reviewed installer to the website root and verify byte identity.
- [ ] Add concise 1.4 documentation to the website and source README, keeping
  manual ZIP and `java -jar` instructions available.
- [ ] Verify the live installer over HTTPS and run the documented disposable
  public smoke test before publication.
- [ ] Resolve the 1.4 release-note mismatch: do not claim checksum/signature
  sidecars that are absent from the public release.

The earlier external-symlink failure is resolved by simplifying the installer,
not by modifying the immutable 1.4 ZIP or its packaged launcher.

### Future stage — revisit after 1.5

The richer installer implementation and its associated tests are preserved on
local branch `installer-1.5` at commit
`9ffc156cadb98a3cfbe234054e3d0181f0da56b8`. After 1.5 is released:

- [ ] Revisit that implementation against the actual 1.5 distribution.
- [ ] Confirm the packaged 1.5 launcher supports its richer command exposure.
- [ ] Test the exact staged and public 1.5 ZIP before replacing the public
  installer or website copy.

Do not over-specify the 1.5 design until the actual 1.5 archive is available.

The intended lifecycle is:

```text
1.4: simple PATH-based installer
  -> 1.5 released
  -> revisit installer-1.5
  -> test against the actual 1.5 ZIP
  -> replace the public installer
```

## Non-goals retained

- [ ] Do not introduce runtime version switching, self-update, or automatic
  upgrade management.
- [ ] Do not download Java, individual dependency JARs, or Maven/Gradle
  dependencies.
- [ ] Do not rearrange distribution libraries or reconstruct its classpath.
- [ ] Do not replace `bin/scriptella.sh` or implement Windows installation in
  this issue.
