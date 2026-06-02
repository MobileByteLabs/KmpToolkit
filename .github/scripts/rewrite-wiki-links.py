#!/usr/bin/env python3
"""
Pre-process docs/ for GitHub Wiki sync.

Copy docs/ → .wiki-build/, rewriting internal `.md` links to absolute
`/wiki/{basename}` URLs that GitHub Wiki actually resolves.

Why this exists
---------------
GitHub Wiki indexes every `.md` file by its **basename** — the source
subdirectory context is dropped when the wiki action mirrors `docs/`.
That means relative links in the source docs break on the wiki:

  ](sub-dir/file.md)  →  404, redirects to raw.githubusercontent.com
  ](file.md)          →  200 but SILENT redirect to wiki Home

This script rewrites those targets to absolute `/wiki/file` URLs that
resolve correctly on the wiki. The source `docs/` tree stays untouched,
so in-repo browsing of `docs/` continues to use relative links naturally.

Scope rules
-----------
* Only rewrite if the resolved link target lives inside the source tree
  (i.e., is a doc that will actually be published to the wiki). Out-of-tree
  references like `../../build-logic/.../Plugin.kt` (.kt is ignored anyway)
  or `../../../../plan-layer/.../GOAL.md` (lives outside docs/) stay
  untouched.
* Skip code fences (```…```) and inline code (`…`) so we don't rewrite
  link-like substrings inside code samples.
* Skip absolute URLs (http://, https://) — never rewrite an existing
  external link.
* Skip pure-anchor links like `(#section)` — anchor-only links stay
  intact (they navigate within the current page).

Wiki URL resolution (portable across repos)
-------------------------------------------
1. `$WIKI_BASE` env var if set (explicit override, useful for tests)
2. `$GITHUB_REPOSITORY` env var (GitHub Actions sets this automatically)
3. `git remote get-url origin` parsed (local dev fallback)
4. Bail with a clear error message if none of the above resolves

Usage: python3 rewrite-wiki-links.py SRC DST
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


def resolve_wiki_base() -> str:
    """Derive the wiki URL prefix for this repo."""
    explicit = os.environ.get("WIKI_BASE")
    if explicit:
        return explicit.rstrip("/")

    gh_repo = os.environ.get("GITHUB_REPOSITORY")  # set by GitHub Actions: "Owner/Repo"
    if gh_repo:
        return f"https://github.com/{gh_repo}/wiki"

    try:
        url = subprocess.check_output(
            ["git", "remote", "get-url", "origin"], text=True
        ).strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        url = ""

    # git@github.com:Owner/Repo.git → https://github.com/Owner/Repo/wiki
    m = re.match(r"git@github\.com:([^/]+)/([^/]+?)(?:\.git)?$", url)
    if m:
        return f"https://github.com/{m.group(1)}/{m.group(2)}/wiki"
    # https://github.com/Owner/Repo[.git] → https://github.com/Owner/Repo/wiki
    m = re.match(r"https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?/?$", url)
    if m:
        return f"https://github.com/{m.group(1)}/{m.group(2)}/wiki"

    print(
        "❌ Cannot resolve wiki URL — set $WIKI_BASE or $GITHUB_REPOSITORY, "
        "or run from a git repo with an origin remote on github.com",
        file=sys.stderr,
    )
    sys.exit(1)


WIKI = resolve_wiki_base()


def build_slug_map(tree_root: Path) -> dict[Path, str]:
    """
    Map every .md file in tree_root to its wiki slug.

    Strategy: GitHub Wiki indexes by file basename. If two `.md` files share
    a basename (`docs/bubble/README.md` + `docs/clipboard/README.md`), the
    second overwrites the first on the wiki. To avoid that we detect
    collisions and use subdir-prefixed slugs for the colliding files:

      docs/Home.md                          → Home
      docs/_Sidebar.md                      → _Sidebar
      docs/installation.md                  → installation       (no collision)
      docs/getting-started/installation.md  → installation       (no collision)
      docs/bubble/README.md                 → bubble-README      (collision!)
      docs/clipboard/README.md              → clipboard-README   (collision!)

    Special wiki files (`Home`, `_Sidebar`, `_Footer`) at the docs root keep
    their bare names regardless.
    """
    SPECIAL = {"Home", "_Sidebar", "_Footer"}
    md_files = list(tree_root.rglob("*.md"))

    # Count basename occurrences
    counts: dict[str, int] = {}
    for f in md_files:
        counts[f.stem] = counts.get(f.stem, 0) + 1

    slug_map: dict[Path, str] = {}
    for f in md_files:
        rel = f.relative_to(tree_root)
        if rel.parent == Path(".") and f.stem in SPECIAL:
            slug_map[f.resolve(strict=False)] = f.stem
        elif counts[f.stem] == 1:
            slug_map[f.resolve(strict=False)] = f.stem
        else:
            # Collision → use subdir-prefixed slug
            parts = list(rel.parts[:-1]) + [f.stem]
            slug_map[f.resolve(strict=False)] = "-".join(parts)
    return slug_map


LINK_RE = re.compile(
    r"""
    \]\(                              # opening of the link target
      (?!https?://)                   # bail on absolute URLs
      (?P<path>(?:[^)/\s]+/)*)        # optional relative path segments
      (?P<basename>[A-Za-z0-9._-]+)   # filename without extension
      \.md                            # require .md
      (?P<anchor>\#[^)\s]+)?          # optional #anchor
    \)
    """,
    re.VERBOSE,
)

# Spans we MUST NOT rewrite: code fences + inline code.
PROTECT_RE = re.compile(r"```[\s\S]*?```|`[^`\n]+`")


def rewrite(content: str, source_file: Path, tree_root: Path, slug_map: dict[Path, str]) -> str:
    """Return `content` with in-tree .md links rewritten to wiki URLs."""
    stash: list[str] = []

    def _stash(m: re.Match) -> str:
        stash.append(m.group(0))
        return f"\0PROTECT{len(stash) - 1}\0"

    stashed = PROTECT_RE.sub(_stash, content)

    tree_root_abs = tree_root.resolve(strict=False)

    def _rewrite(m: re.Match) -> str:
        path = m.group("path") or ""
        basename = m.group("basename")
        anchor = m.group("anchor") or ""

        target_abs = (source_file.parent / f"{path}{basename}.md").resolve(strict=False)
        try:
            target_abs.relative_to(tree_root_abs)
        except ValueError:
            # Outside the docs tree — leave the link as-is so repo browsing still works.
            return m.group(0)

        slug = slug_map.get(target_abs, basename)
        return f"]({WIKI}/{slug}{anchor})"

    rewritten = LINK_RE.sub(_rewrite, stashed)

    for i, original in enumerate(stash):
        rewritten = rewritten.replace(f"\0PROTECT{i}\0", original, 1)

    return rewritten


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: rewrite-wiki-links.py SRC DST", file=sys.stderr)
        return 1

    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])

    if not src.is_dir():
        print(f"Source dir not found: {src}", file=sys.stderr)
        return 1

    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)

    # Phase 1 — Build the slug map from the ORIGINAL (still-nested) tree.
    # This map keys absolute paths in dst → wiki slug. Used by the rewriter
    # to translate `[text](sub/file.md)` link targets to `/wiki/{slug}` URLs.
    slug_map = build_slug_map(dst)

    # Phase 2 — Rewrite links inside each file. Resolves link targets against
    # the original tree layout (subdirs still present), so [text](sub/file.md)
    # in docs/Home.md correctly resolves to dst/sub/file.md → looks up
    # slug "sub-file" → emits /wiki/sub-file.
    changed = 0
    for md_file in list(dst.rglob("*.md")):
        before = md_file.read_text()
        after = rewrite(before, md_file, dst, slug_map)
        if after != before:
            md_file.write_text(after)
            changed += 1
            print(f"rewrote: {md_file.relative_to(dst)}")

    # Phase 3 — Flatten dst to top-level (wiki indexes by basename).
    # Move dst/sub/file.md → dst/sub-file.md (slug-named).
    for original_path, slug in slug_map.items():
        rel = original_path.relative_to(dst.resolve(strict=False))
        if str(rel) != f"{slug}.md":
            target = dst / f"{slug}.md"
            original_path.rename(target)

    # Phase 4 — Drop empty subdirs left behind by the flatten.
    for subdir in sorted(dst.rglob("*"), reverse=True):
        if subdir.is_dir() and not any(subdir.iterdir()):
            subdir.rmdir()

    # Phase 5 — Auto-generate _Sidebar.md if the consumer didn't ship one.
    # Consumer-authored docs/_Sidebar.md (now at dst/_Sidebar.md after flatten)
    # always wins. Generator groups by source subdirectory for sections.
    sidebar = dst / "_Sidebar.md"
    if not sidebar.exists():
        sidebar.write_text(_generate_sidebar(slug_map, dst))
        print(f"auto-generated: _Sidebar.md ({len(slug_map)} pages)")

    print(f"\nDone. {changed} file(s) had links rewritten. WIKI={WIKI}")
    return 0


def _generate_sidebar(slug_map: dict[Path, str], tree_root: Path) -> str:
    """
    Build a wiki Sidebar grouped by source subdirectory.

    Consumers can ship their own `docs/_Sidebar.md` to override — this generator
    only fires when none exists. Keep it minimal + alphabetical; consumer-authored
    sidebars get richer copy when worth the maintenance.
    """
    root = tree_root.resolve(strict=False)
    sections: dict[str, list[tuple[str, str]]] = {}
    home_slug: str | None = None

    for abs_path, slug in slug_map.items():
        try:
            rel = abs_path.relative_to(root)
        except ValueError:
            continue
        stem = abs_path.stem
        if stem in {"Home", "_Sidebar", "_Footer"}:
            if stem == "Home":
                home_slug = slug
            continue
        # Section header = first path part if nested, else "General".
        section = rel.parts[0] if len(rel.parts) > 1 else "General"
        # Display name = stem with - and _ → spaces, title-cased.
        display = stem.replace("-", " ").replace("_", " ").title()
        sections.setdefault(section, []).append((display, slug))

    lines: list[str] = []
    if home_slug:
        lines.append(f"**[Home]({WIKI}/{home_slug})**")
        lines.append("")

    for section_name in sorted(sections.keys(), key=lambda s: (s == "General", s.lower())):
        header = section_name.replace("-", " ").replace("_", " ").title()
        lines.append(f"**{header}**")
        for display, slug in sorted(sections[section_name]):
            lines.append(f"- [{display}]({WIKI}/{slug})")
        lines.append("")

    return "\n".join(lines).rstrip() + "\n"


if __name__ == "__main__":
    sys.exit(main())
