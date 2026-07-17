#!/usr/bin/env python3
"""Publish generated Javadoc / DTDDoc into a sibling scriptella.github.io tree.

See docs/site/README.md for layout, options, and the StatCounter allowlist.
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

STATCOUNTER_MARKER = "StatCounter: existing Scriptella project configuration"
STATCOUNTER_SNIPPET = """  <!-- StatCounter: existing Scriptella project configuration -->
  <script>
    var sc_project = 10775960;
    var sc_invisible = 1;
    var sc_security = "53eaed1c";
  </script>
  <script defer src="https://secure.statcounter.com/counter/counter.js"></script>
  <!-- End StatCounter -->
"""

# Refuse to rsync --delete if the generated tree looks empty or thin.
# Healthy site currently has ~570 API HTML files and ~40 package-summary pages;
# floors are intentionally well below that so modest package churn still passes.
MIN_API_FILES = 100
MIN_API_HTML = 100
MIN_API_PACKAGE_SUMMARIES = 20
MIN_DTD_FILES = 8
MIN_DTD_HTML = 4
# When the destination already looks healthy, source must not shrink drastically.
MIN_SRC_VS_DEST_RATIO = 0.5

API_REQUIRED_FILES = (
    "overview-summary.html",
    "index.html",
    "stylesheet.css",
    "package-list",
)
DTD_REQUIRED_FILES = (
    "intro.html",
    "index.html",
    "elementsIndex.html",
)


def product_root() -> Path:
    # docs/site/sync_generated_docs.py -> scriptella-etl/
    return Path(__file__).resolve().parent.parent.parent


def default_site_dir(root: Path) -> Path:
    return root.parent / "scriptella.github.io"


def default_dtddoc_dir(root: Path) -> Path | None:
    candidate = root.parent / "DTDDoc"
    return candidate if candidate.is_dir() else None


def resolve_ant(root: Path, explicit: str | None) -> Path | None:
    if explicit:
        return Path(explicit)
    which = shutil.which("ant")
    if which:
        return Path(which)
    sibling = root.parent / "apache-ant-1.10.17" / "bin" / "ant"
    if sibling.is_file() and os.access(sibling, os.X_OK):
        return sibling
    return None


def run(cmd: list[str], *, dry_run: bool, cwd: Path | None = None) -> None:
    printable = " ".join(shlex_quote(c) for c in cmd)
    if dry_run:
        print(f"[dry-run] {printable}")
        return
    subprocess.run(cmd, check=True, cwd=str(cwd) if cwd else None)


def shlex_quote(value: str) -> str:
    if not value or any(c in value for c in ' \t\n"\'\\$`'):
        return "'" + value.replace("'", "'\"'\"'") + "'"
    return value


def rsync_tree(src: Path, dest: Path, *, dry_run: bool) -> None:
    if not shutil.which("rsync"):
        raise SystemExit("error: rsync is required")
    dest.mkdir(parents=True, exist_ok=True)
    run(
        ["rsync", "-a", "--delete", f"{src}/", f"{dest}/"],
        dry_run=dry_run,
    )


def count_files(root: Path) -> tuple[int, int, int]:
    """Return (all_files, html_files, package_summary_files)."""
    all_files = 0
    html_files = 0
    package_summaries = 0
    if not root.is_dir():
        return 0, 0, 0
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        all_files += 1
        name = path.name
        if name.endswith(".html"):
            html_files += 1
        if name == "package-summary.html":
            package_summaries += 1
    return all_files, html_files, package_summaries


def missing_required(root: Path, required: tuple[str, ...]) -> list[str]:
    return [name for name in required if not (root / name).is_file()]


def sanity_check_tree(
    label: str,
    src: Path,
    dest: Path,
    *,
    required: tuple[str, ...],
    min_files: int,
    min_html: int,
    min_package_summaries: int = 0,
) -> None:
    """Abort before rsync if the source tree is empty, incomplete, or far thinner than dest."""
    if not src.is_dir():
        raise SystemExit(
            f"error: missing generated {label} docs: {src}\n"
            "       refuse to sync so the website tree is not wiped."
        )

    missing = missing_required(src, required)
    src_files, src_html, src_pkgs = count_files(src)
    dest_files, dest_html, dest_pkgs = count_files(dest)

    problems: list[str] = []
    if missing:
        problems.append("missing required files: " + ", ".join(missing))
    if src_files < min_files:
        problems.append(f"only {src_files} files (minimum {min_files})")
    if src_html < min_html:
        problems.append(f"only {src_html} HTML files (minimum {min_html})")
    if min_package_summaries and src_pkgs < min_package_summaries:
        problems.append(
            f"only {src_pkgs} package-summary.html files "
            f"(minimum {min_package_summaries})"
        )

    # If the live site already looks populated, reject a source that is ~empty by comparison.
    if dest_html >= min_html:
        if src_html < int(dest_html * MIN_SRC_VS_DEST_RATIO):
            problems.append(
                f"source HTML count {src_html} is under "
                f"{int(MIN_SRC_VS_DEST_RATIO * 100)}% of website {dest_html}"
            )
        if min_package_summaries and dest_pkgs >= min_package_summaries:
            if src_pkgs < int(dest_pkgs * MIN_SRC_VS_DEST_RATIO):
                problems.append(
                    f"source package-summary count {src_pkgs} is under "
                    f"{int(MIN_SRC_VS_DEST_RATIO * 100)}% of website {dest_pkgs}"
                )
        if dest_files >= min_files and src_files < int(dest_files * MIN_SRC_VS_DEST_RATIO):
            problems.append(
                f"source file count {src_files} is under "
                f"{int(MIN_SRC_VS_DEST_RATIO * 100)}% of website {dest_files}"
            )

    print(
        f"sanity {label}: src files={src_files} html={src_html}"
        + (f" package-summary={src_pkgs}" if min_package_summaries else "")
        + (
            f"; site files={dest_files} html={dest_html}"
            + (f" package-summary={dest_pkgs}" if min_package_summaries else "")
            if dest.is_dir()
            else "; site=(absent)"
        )
    )

    if problems:
        detail = "\n".join(f"         - {p}" for p in problems)
        raise SystemExit(
            f"error: generated {label} docs look empty or incomplete: {src}\n"
            f"{detail}\n"
            "       refuse to rsync --delete so the website tree is not wiped.\n"
            "       Rebuild docs successfully, then re-run."
        )


def track_targets(api_dest: Path, dtd_dest: Path) -> list[Path]:
    """Hub + package-summary + DTD entry pages only (not every class page)."""
    targets: list[Path] = []
    overview = api_dest / "overview-summary.html"
    if overview.is_file():
        targets.append(overview)
    targets.extend(sorted(api_dest.rglob("package-summary.html")))
    for name in ("intro.html", "elementsIndex.html"):
        path = dtd_dest / name
        if path.is_file():
            targets.append(path)
    targets.extend(sorted(dtd_dest.rglob("etl.dtd.html")))
    # Stable unique order
    seen: set[Path] = set()
    unique: list[Path] = []
    for path in targets:
        resolved = path.resolve()
        if resolved not in seen:
            seen.add(resolved)
            unique.append(path)
    return unique


def relative_display(path: Path, root: Path) -> str:
    try:
        return str(path.resolve().relative_to(root.resolve()))
    except ValueError:
        return str(path)


def inject_statcounter(path: Path, *, dry_run: bool, display: str) -> str:
    text = path.read_text(encoding="utf-8", errors="surrogateescape")
    if STATCOUNTER_MARKER in text:
        return f"already present: {display}"
    lower = text.lower()
    idx = lower.rfind("</body>")
    if idx < 0:
        return f"skip (no </body>): {display}"
    if dry_run:
        return f"would inject: {display}"
    path.write_text(
        text[:idx] + STATCOUNTER_SNIPPET + "\n" + text[idx:],
        encoding="utf-8",
        errors="surrogateescape",
    )
    return f"injected: {display}"


def build_docs(root: Path, ant: Path, dtddoc: Path | None, *, dry_run: bool) -> None:
    build_file = root / "build-docs.xml"
    if dtddoc is not None:
        print(f"regenerating Javadoc + DTD docs (DTDDoc: {dtddoc})")
        run(
            [
                str(ant),
                "-f",
                str(build_file),
                f"-Ddtddoc.dir={dtddoc}",
                "codereports",
            ],
            dry_run=dry_run,
            cwd=root,
        )
    else:
        print(
            "warning: DTDDoc not found; regenerating Javadoc only.\n"
            "         Pass --dtddoc-dir or set DTDDOC_DIR to include DTD docs.",
            file=sys.stderr,
        )
        run(
            [str(ant), "-f", str(build_file), "javadoc"],
            dry_run=dry_run,
            cwd=root,
        )


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Copy generated API/DTD docs into a sibling scriptella.github.io "
            "checkout and inject StatCounter into a small allowlist of pages."
        )
    )
    parser.add_argument(
        "--build",
        action="store_true",
        help="Regenerate docs with Ant before copying",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print actions without modifying the website tree",
    )
    parser.add_argument(
        "--site-dir",
        type=Path,
        default=None,
        help="Website checkout (default: sibling scriptella.github.io)",
    )
    parser.add_argument(
        "--dtddoc-dir",
        type=Path,
        default=None,
        help="DTDDoc home for --build (default: sibling DTDDoc if present)",
    )
    parser.add_argument(
        "--ant",
        default=None,
        help="Ant executable (default: PATH or sibling apache-ant-1.10.17)",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    root = product_root()
    site = (args.site_dir or Path(os.environ.get("SITE_DIR", default_site_dir(root)))).resolve()
    dtddoc_env = os.environ.get("DTDDOC_DIR")
    dtddoc = args.dtddoc_dir
    if dtddoc is None and dtddoc_env:
        dtddoc = Path(dtddoc_env)
    if dtddoc is None:
        dtddoc = default_dtddoc_dir(root)
    elif not dtddoc.is_dir():
        raise SystemExit(f"error: DTDDoc directory not found: {dtddoc}")

    if not site.is_dir():
        raise SystemExit(
            f"error: website checkout not found: {site}\n"
            "       clone scriptella.github.io as a sibling of scriptella-etl, "
            "or pass --site-dir / set SITE_DIR."
        )

    print(f"product:  {root}")
    print(f"site:     {site}")
    print(f"build:    {'yes' if args.build else 'no'}")
    print(f"dry-run:  {'yes' if args.dry_run else 'no'}")

    if args.build:
        ant = resolve_ant(root, args.ant or os.environ.get("ANT"))
        if ant is None:
            raise SystemExit(
                "error: Ant not found. Install Ant, put it on PATH, or pass --ant."
            )
        build_docs(root, ant, dtddoc, dry_run=args.dry_run)

    api_src = root / "build" / "docs" / "api"
    dtd_src = root / "build" / "docs" / "dtd"
    etl_dtd_src = root / "core" / "src" / "conf" / "scriptella" / "dtd" / "etl.dtd"
    api_dest = site / "docs" / "api"
    dtd_dest = site / "docs" / "dtd"
    etl_dtd_dest = site / "dtd" / "etl.dtd"

    if not etl_dtd_src.is_file():
        raise SystemExit(f"error: missing source DTD: {etl_dtd_src}")

    # Sanity-check both trees before any rsync --delete.
    sanity_check_tree(
        "API",
        api_src,
        api_dest,
        required=API_REQUIRED_FILES,
        min_files=MIN_API_FILES,
        min_html=MIN_API_HTML,
        min_package_summaries=MIN_API_PACKAGE_SUMMARIES,
    )
    sanity_check_tree(
        "DTD",
        dtd_src,
        dtd_dest,
        required=DTD_REQUIRED_FILES,
        min_files=MIN_DTD_FILES,
        min_html=MIN_DTD_HTML,
    )

    print(f"syncing API docs -> {api_dest}")
    rsync_tree(api_src, api_dest, dry_run=args.dry_run)

    print(f"syncing DTD docs -> {dtd_dest}")
    rsync_tree(dtd_src, dtd_dest, dry_run=args.dry_run)

    print(f"copying etl.dtd -> {etl_dtd_dest}")
    if args.dry_run:
        print(f"[dry-run] cp {etl_dtd_src} {etl_dtd_dest}")
    else:
        etl_dtd_dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(etl_dtd_src, etl_dtd_dest)

    # Dry-run scans the source tree (dest not updated); live run scans the site.
    scan_api = api_src if args.dry_run else api_dest
    scan_dtd = dtd_src if args.dry_run else dtd_dest
    targets = track_targets(scan_api, scan_dtd)

    print("injecting StatCounter into allowlisted generated pages")
    if not targets:
        print("warning: no allowlisted HTML files found for StatCounter", file=sys.stderr)
    else:
        for target in targets:
            if args.dry_run:
                try:
                    display = str(Path("docs/api") / target.relative_to(scan_api))
                except ValueError:
                    display = str(Path("docs/dtd") / target.relative_to(scan_dtd))
            else:
                display = relative_display(target, site)
            print(f"  {inject_statcounter(target, dry_run=args.dry_run, display=display)}")
        print(f"tracked page targets: {len(targets)}")

    print()
    print("done. Review and commit from the website repo when ready:")
    print(f"  git -C {shlex_quote(str(site))} status")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
