# Site generated-docs sync

Operator tools for publishing **Javadoc** and **DTDDoc** output from this
repository into the sibling website checkout `scriptella.github.io`.

Maintained HTML (tutorial, reference, support, …) is edited directly in the
website repo. Only the generated trees under `docs/api/` and `docs/dtd/` are
refreshed by this tooling.

## Layout assumptions

```text
<workspace>/
  scriptella-etl/          # this repository
  scriptella.github.io/    # public site (sibling checkout)
  DTDDoc/                  # optional, for --build with DTD docs
  apache-ant-…/            # optional local Ant
```

The sync script resolves the product root as the parent of `docs/`, then expects
`scriptella.github.io` as a sibling of `scriptella-etl`. It **exits with an
error** if that directory is missing, is not a directory, or does not look like
the site repo (no `index.html` / `CNAME` / `docs/` / `.git`). It will not create
the website checkout.

## Usage

```bash
# Copy existing build/docs into the site (no Ant)
python3 docs/site/sync_generated_docs.py

# Regenerate with Ant, then copy and inject trackers
python3 docs/site/sync_generated_docs.py --build

# Preview actions only
python3 docs/site/sync_generated_docs.py --dry-run
python3 docs/site/sync_generated_docs.py --build --dry-run
```

Requires **Python 3.9+**, **rsync**, and (for `--build`) **Ant**. DTDDoc is
used when present for a full docs rebuild. Prefer **Java 8** for Javadoc so the
output matches the frameset layout currently published on scriptella.org
(`--build` tries to select a Java 8 `JAVA_HOME` automatically on macOS).

| Flag / env | Purpose |
|------------|---------|
| `--build` | Regenerate docs with Ant before copying |
| `--dry-run` | Print actions without modifying the website tree |
| `--site-dir` / `SITE_DIR` | Website checkout (default: sibling `scriptella.github.io`) |
| `--dtddoc-dir` / `DTDDOC_DIR` | DTDDoc home for `--build` (default: sibling `DTDDoc`) |
| `--ant` / `ANT` | Ant executable (default: `PATH` or sibling `apache-ant-1.10.17`) |
| `--java-home` / `JAVA_HOME_8` | JDK for `--build` (default: Temurin 8 if installed) |

`--build` runs `build-docs.xml` `codereports` when DTDDoc is available, or
`javadoc` only when it is not (with a warning).

## What is copied

| Source (`scriptella-etl`) | Destination (`scriptella.github.io`) |
|---------------------------|--------------------------------------|
| `build/docs/api/` | `docs/api/` |
| `build/docs/dtd/` | `docs/dtd/` |
| `core/src/conf/scriptella/dtd/etl.dtd` | `dtd/etl.dtd` |

Copy uses `rsync --delete` so removed packages do not leave stale HTML on the site.

### Sanity checks (before rsync)

The script **refuses to sync** if a generated tree looks empty or mostly empty,
so a failed Ant/Javadoc run cannot wipe the live website docs:

* Required entry files must exist (API: overview + index + stylesheet/`resource-files`
  + `package-list`/`element-list`; DTD: `intro.html`, `index.html`,
  `elementsIndex.html`).
* Absolute floors: enough files / HTML pages / `package-summary.html` pages
  (well below a healthy full tree, but above an empty or partial build).
* If the website destination already looks populated, the source must not be
  drastically smaller (under ~50% of the site’s file/HTML counts).

On failure the script exits non-zero and does not run `rsync`.

## StatCounter projects

Scriptella uses two separate StatCounter projects so generated documentation
traffic does not consume the maintained website's visit allowance:

| Project | ID | Covers |
| --- | --- | --- |
| Scriptella website | `10775960` | Maintained pages in `scriptella.github.io` |
| Scriptella API documentation | `13337472` | The generated API/DTD allowlist below |

## StatCounter injection (subset only)

After copy, the separate Scriptella API documentation StatCounter project is
injected into a **three-page allowlist** of generated HTML files (not every class
page). The maintained website continues to use its own project:

* `docs/api/overview-summary.html` — API hub / default content frame
* `docs/dtd/intro.html` — DTD hub
* `docs/dtd/elementsIndex.html` — element index

Skipped: API frameset chrome and content (`docs/api/**`), package summaries,
class/type pages, detailed DTD reference pages, `package-tree` / `package-use`,
and other secondary generated pages.

Injection is idempotent (marker comment) and applied only under the website
tree, never into distribution `build/docs` consumed by the product zip. Each
live sync first removes the marker-delimited block from all generated HTML, so
pages removed from the allowlist stop being tracked immediately.

## After a successful sync

Review the website git status, then commit and deploy from
`scriptella.github.io` when ready. This script does not commit or push.
