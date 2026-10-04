# Contributing to Scriptella

Thank you for contributing to Scriptella. Keep changes focused and make
functional diffs easy to review.

## Commit messages

Include the issue number in the commit message when an issue is available.
For example: `Add direct SQL file execution without ETL XML (#63)`.

## Code style

* Create new text files as UTF-8 without a byte-order mark (BOM), using LF
  line endings.
* Preserve an existing file's line-ending style during ordinary changes. Do
  not mix CRLF-to-LF conversion or other whole-file mechanical formatting with
  behavioral changes.
* Perform intentional line-ending normalization or broad reformatting in a
  dedicated formatting-only commit.
* Use four spaces, not tabs, for Java indentation.
* Use braces for all Java control-flow bodies, including single-statement
  `if`, `else`, loop, and similar bodies.
* Remove trailing whitespace and end text files with one newline.
* Apply local style fixes to code being modified, but do not reformat unrelated
  legacy code.

The repository contains legacy files with both CRLF and LF line endings.
Consequently, the repository does not currently force one line-ending style
through `.editorconfig` or `.gitattributes`: doing so would make an ordinary
edit appear to replace an entire legacy file. Check the diff before committing
to ensure that only intended lines changed.

If a legacy file is intentionally normalized, prefer two separate commits:
one mechanical normalization commit and one functional commit. This keeps both
changes reviewable and makes history easier to follow.

## Ant tests

The SQLite contract test requires the same external JDBC driver as the Maven
tests. Before running `ant test`, fetch it from the repository root:

```bash
mvn --batch-mode --no-transfer-progress -N -Pant-test-dependencies dependency:copy
```

The profile uses `sqlite.jdbc.version` from the root `pom.xml`, shared with the
Maven tests, and writes `build/test-dependencies/sqlite-jdbc.jar` for Ant.
When upgrading the driver, update that property and the compatibility test's
expected driver version together.

For an existing local copy, pass `-Dsqlite.jdbc.jar=/absolute/path/to/sqlite-jdbc.jar`
to Ant instead. Keep this test dependency outside `lib/` so it is not bundled
in the release distribution. CI performs the download before the Ant tests.
