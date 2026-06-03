---
title: Docs guide (template)
description: Canonical authoring blueprint for KMP library documentation. Sync'd from mbl-library-template-kmp into every consumer repo; do not edit downstream.
---

# Docs guide (template)

!!! abstract "What this file is"
    The canonical, reusable blueprint for writing the documentation that lives
    under `docs/`. **Owned by [`mbl-library-template-kmp`](https://github.com/MobileByteLabs/mbl-library-template-kmp)**
    and sync'd into every consumer repo via `sync-dirs.sh` (see `SYNC_FILES`).

    Edits made to this file in a consumer repo **will be overwritten on the
    next sync**. For library-specific extensions, use the sibling
    [`DEVELOPMENT.md`](DEVELOPMENT.md).

!!! info "Two audiences"
    **Humans** read top to bottom for orientation.
    **AI agents** grep for the four canonical headings — Agent quick
    reference, Invariants, Scaling rubric, Validation commands — to act
    deterministically without parsing prose.

---

## Agent quick reference

Each row is the **complete** recipe. Follow every step in order.

| Want to… | Recipe |
|----------|--------|
| Add a narrative page | (1) Author `docs/<slug>.md` (2) Add `- <Title>: <slug>.md` under the right section in `mkdocs.yml` → `nav:` (3) `git add` both → commit |
| Add a section | (1) `mkdir docs/<section>` (2) Author the first page (3) Add a `- <Section>:\n  - <Page>: <section>/<page>.md` block to `mkdocs.yml` → `nav:` |
| Add an image/asset | (1) Drop into `docs/images/` (create if needed) (2) Reference as `![Alt](images/<file>)` from any page |
| Override brand colors | Edit `docs/stylesheets/mbs-brand.css` → `--md-primary-fg-color` (header/links) + `--md-accent-fg-color` (highlights, copy button) |
| Test build locally | `pip install -r docs/requirements.txt && mkdocs build --strict` |
| Live preview | `mkdocs serve` → open `http://127.0.0.1:8000` |
| Upgrade docs pipeline | Bump `@vX.Y.Z` pin in `.github/workflows/docs-publish.yml` |
| Trigger deploy manually | `gh workflow run docs-publish.yml --ref development` |
| Investigate `/` 404 | Verify `docs/index.md` exists (mkdocs needs it for the root URL) |
| Edit Home / index content | Edit **both** `docs/Home.md` and `docs/index.md` — they MUST stay in sync |
| Disable Jekyll on Pages | Already handled — reusable workflow `v1.9.1+` calls `actions/configure-pages@v5` with `enablement: true` |

---

## Invariants

When the file in the first column changes, the files in the second column
**must** also change in the same commit. Skipping either side breaks the
contract.

| If you change… | Also update… | Why |
|----------------|--------------|-----|
| `docs/Home.md` content | `docs/index.md` (mirror) | Wiki uses `Home.md`; mkdocs uses `index.md`. Divergence = surfaces show different content. |
| `docs/index.md` content | `docs/Home.md` (mirror) | Same reason, reversed. |
| New user-facing page under `docs/` | `mkdocs.yml` → `nav:` (registration) | Page on disk + no nav entry = invisible on site. |
| New section directory | `mkdocs.yml` → `nav:` (new section block) | Contents don't appear until the section is registered. |
| Renamed page | (1) Update all inbound `[link](old.md)` references (2) Optionally add a redirect via `mkdocs-redirects` plugin | Old URLs 404 + inbound links break. |
| Deleted page | (1) Remove from `mkdocs.yml` nav (2) Grep for inbound links + fix or delete | Strict-mode build fails on dangling nav entries. |
| Bumped `mkdocs-material` in `docs/requirements.txt` | Run `mkdocs build --strict` locally + visually preview | Material can introduce theme breaks across minor versions. |
| Bumped caller pin (`@v1.9.1` → `@v1.10.x`) in `.github/workflows/docs-publish.yml` | Verify the new tag exists at `MobileByteLabs/mbl-actionhub` | Pinning a non-existent tag fails workflow resolution. |

---

## The dual-surface contract

Files under `docs/` feed two published surfaces simultaneously. Each
surface has different conventions — but the **source is the same files**.

=== "mkdocs site (canonical)"

    **Pipeline:** `.github/workflows/docs-publish.yml` → reusable
    `mbl-actionhub/docs-publish-mkdocs.yml`

    **Lands at:** `https://<org>.github.io/<repo>/`

    **Triggered:** on push to `development` when `docs/**`, `mkdocs.yml`, or
    the caller workflow itself changes.

    **Build:** mkdocs-material renders every `.md` in `docs/` (except those
    in `exclude_docs:`) into static HTML. `actions/deploy-pages` hands the
    artifact to Pages.

=== "GitHub Wiki"

    **Pipeline:** `.github/workflows/sync-docs-to-wiki.yml` → composite
    action `mbl-actionhub-docshub`

    **Lands at:** `https://github.com/<org>/<repo>/wiki/<basename>`

    **Triggered:** same paths, same branch.

    **Build:** the composite walks `docs/` and pushes each `.md` to the
    wiki repo, basename-indexed. Wiki uses `Home.md` as its landing page.

!!! warning "Don't edit on the surfaces — edit the source"
    The site and wiki are read-only outputs. Always edit `docs/<file>.md`
    and push to `development`; both surfaces redeploy within ~30 seconds.

---

## Files with special meaning

| File | Used by | Purpose |
|------|---------|---------|
| `docs/index.md` | mkdocs | Root URL of the site (`/`). **Required** — without it the root returns 404. |
| `docs/Home.md` | wiki | Wiki's home page. GitHub Wiki indexes by basename; this filename is hard-coded. Excluded from the mkdocs build via `exclude_docs:` to avoid a duplicate `/Home/` page. |
| `docs/_Sidebar.md` | wiki | Wiki sidebar nav. Excluded from the mkdocs build. |
| `docs/requirements.txt` | docs-publish workflow | Pinned mkdocs deps. Change a version **here**, not in the workflow. |
| `docs/stylesheets/mbs-brand.css` | mkdocs | Brand polish. Override `--md-primary-fg-color` and `--md-accent-fg-color` here. |

!!! tip "Home.md and index.md are duplicates by design"
    mkdocs needs `index.md`, wiki needs `Home.md`, and they should always
    show the same landing content. **Edit one → edit the other in the same commit.**

---

## Adding a new page

```bash
# 1. Write the page (kebab-case filename)
echo "# My new page" > docs/<section>/<my-new-page>.md

# 2. Edit mkdocs.yml → nav: add the entry under the right section
#    - <Section>:
#        - <Title>: <section>/<my-new-page>.md

# 3. Verify locally
mkdocs serve  # browse to the new page

# 4. Commit + push
git add docs/<section>/<my-new-page>.md mkdocs.yml
git commit -m "docs: add <my-new-page>"
git push origin development
```

The docs-publish workflow rebuilds and redeploys within ~30 seconds.
The wiki sync picks up the new file automatically — no nav registration
needed there (wiki uses `_Sidebar.md` for nav).

---

## Style guide

### Code blocks

Always declare the language. mkdocs-material renders Kotlin, Swift, Bash,
YAML, JSON, and TOML out of the box.

```kotlin
val worker = MyWorker()
```

Inline `code` for symbols and flag names. Use `**bold**` sparingly for
first-mention emphasis of a concept.

### Admonitions

Use `!!! note`, `!!! warning`, `!!! tip`, `!!! example`, `!!! info`,
`!!! danger`, `!!! success`, `!!! abstract` for callouts that break the
prose flow but are important.

!!! example "Admonition syntax"
    ```markdown
    !!! warning "iOS background time is finite"
        BGTaskScheduler grants ~30s of execution. Long work needs
        `URLSession.uploadTask` instead.
    ```

For collapsible content, use `???` instead of `!!!`:

```markdown
??? note "Click to expand"
    Hidden until clicked. Useful for long examples or troubleshooting.
```

### Tabbed content

Use `pymdownx.tabbed` for multi-flavor content (per-platform examples,
multiple language equivalents):

````markdown
=== "Android"
    ```kotlin
    // Android-specific
    ```

=== "iOS"
    ```kotlin
    // iOS-specific
    ```
````

### Tables

Use tables for comparisons with ≥3 dimensions (platforms × features,
versions × behaviors). Inline bullet lists are fine for ≤2 dimensions.

### Links

- **Internal** (within `docs/`): relative paths
  (`[Quick start](getting-started/quick-start.md)`). mkdocs validates these
  on build; broken links surface as INFO-level warnings.
- **External**: full URLs.
- **Cross-repo source** (e.g. `workspaces/mbs/...`): downgraded to INFO via
  `validation.links.not_found: info` in `mkdocs.yml`. Expected.

### Per-platform caveats

When behavior varies across platforms, use this pattern:

```markdown
- **Android:** auto-init via ContentProvider; no manual `init()` needed.
- **iOS:** call from `applicationDidFinishLaunching`.
- **JVM Desktop:** prints to `System.out`; ANSI color enabled if TTY.
- **JS / wasmJs:** requires a user gesture on first invocation.
```

The bullet-with-bold-platform-name pattern is grep-friendly and reads
consistently across the site.

---

## Local preview

```bash
pip install -r docs/requirements.txt
mkdocs serve
# open http://127.0.0.1:8000
```

!!! tip "Strict mode mirrors CI"
    `mkdocs build --strict` is what the CI workflow runs. If strict
    fails locally, it'll fail in CI. Most common cause: a nav entry
    references a file that doesn't exist, or a relative link points
    outside `docs/`.

---

## Scaling rubric

The starter `mkdocs.yml` ships with a 5-section nav. That's intentionally
optimistic — most early-stage libraries shouldn't pre-create all sections.

**Start flat. Split a section only when it has ≥ 4 pages.**

| Library shape | docs/ structure |
|---------------|-----------------|
| **1 module, < 5 pages** | Flat: `index.md`, `Home.md`, `getting-started.md`, `api.md`. No subdirs. `mkdocs.yml` nav = 4 entries. |
| **1-2 modules, 5-15 pages** | Add `getting-started/` + `features/` subdirs. Keep operations + release inline as single pages until each grows to ≥ 4 pages. |
| **2-5 modules, 15-30 pages** | Adopt the full starter nav (6 sections). Each section has its own subdir. Add a `platform-support/` matrix page. |
| **5+ modules** | Introduce `docs/modules/<module>.md` per-module landing pages. Use `mkdocs-include-markdown-plugin` to mirror each module's source-tree README. Add a Modules section to nav. |
| **Heavy how-to content** (≥ 10 task-oriented pages) | Introduce a `docs/cookbook/<topic>/<recipe>.md` structure. One page per task, named as a user question ("How do I X?"). See [KmpToolkit](https://github.com/MobileByteLabs/KmpToolkit) for the live pattern. |
| **Heavy API reference** | Don't render API ref in mkdocs. Bundle Dokka HTML inside `-javadoc.jar` (per-module `dokkaGeneratePublicationHtml` + `vanniktech.mavenPublish.JavadocJar.Dokka` config) and link to Maven Central from the mkdocs site. |

!!! warning "Anti-pattern: pre-structuring for hypothetical growth"
    Empty sections (headings with one stub page) look unprofessional and add
    nav clutter. Three nav entries with content > seven nav entries
    half-filled.

---

## When the site breaks

| Symptom | Cause | Fix |
|---------|-------|-----|
| `/` returns 404 | `docs/index.md` missing | Add it (duplicate of `Home.md`). |
| Build fails: "unrecognized relative link" | A `*.md` link points outside `docs/` | Either fix the link or accept it (it'll surface as INFO, not error). |
| Build fails: "nav references file that doesn't exist" | `mkdocs.yml` nav has a stale entry | Remove the entry or create the file. |
| Pages deploy succeeds but site shows old content | CDN cache | Hard refresh (Cmd-Shift-R). Usually clears within 1-2 min. |
| `configure-pages` errors with "Get Pages site failed" | Repo's Pages source not set to "GitHub Actions" | Bump caller workflow pin to `v1.9.1+` (auto-enables) OR flip Settings → Pages → Source manually. |

---

## Pipeline architecture

??? info "How the docs-publish workflow actually works"
    The build + deploy logic lives **once** in
    [`mbl-actionhub/docs-publish-mkdocs.yml`](https://github.com/MobileByteLabs/mbl-actionhub/blob/main/.github/workflows/docs-publish-mkdocs.yml).

    This repo's `.github/workflows/docs-publish.yml` is a 5-line caller
    that pins a specific version (`@v1.9.1+`). Pipeline steps:

    1. `actions/checkout@v4`
    2. `actions/setup-python@v5` (with `cache: pip` + `cache-dependency-path: docs/requirements.txt`)
    3. `pip install -r docs/requirements.txt`
    4. `mkdocs build --strict` → produces `site/`
    5. `touch ./site/.nojekyll` (belt-and-suspenders Jekyll disable)
    6. `actions/configure-pages@v5` with `enablement: true` (auto-enables Pages on first run)
    7. `actions/upload-pages-artifact@v3`
    8. `actions/deploy-pages@v4` (separate job, environment-gated)

    Upgrades happen in one place; consumers bump the pin to opt in.

---

## Wiki notes

??? note "Wiki sync specifics"
    - Wiki sync requires the wiki to be initialized:
      Settings → Features → Wikis → enabled, then create any first
      page via the Wiki UI. Without this, the composite errors with
      "Repository not found".
    - `_Sidebar.md` in `docs/` becomes the wiki's left nav. Set
      `sidebar-mode: auto` in the workflow inputs (the default) to
      auto-generate it if absent.
    - Wiki indexes by basename — two files named the same in different
      subdirs collide. The docshub composite resolves by subdir-prefixing
      the slug, but it's cleaner to avoid the collision in the first
      place.

---

## Validation commands

??? example "Full CLI reference"
    Exact snippets agents can run without modification.

    ```bash
    # Strict build (what CI runs)
    pip install -r docs/requirements.txt
    mkdocs build --strict

    # Live preview
    mkdocs serve  # → http://127.0.0.1:8000

    # Show only WARNING/ERROR from strict build (filter INFO noise)
    mkdocs build --strict 2>&1 | grep -E "^(WARNING|ERROR)"

    # Count pages
    find docs -name "*.md" -not -path "*/stylesheets/*" | wc -l

    # Confirm Home.md and index.md are in sync (zero diff = OK)
    diff docs/Home.md docs/index.md && echo "OK: in sync"

    # Show current caller pin
    grep "docs-publish-mkdocs.yml@" .github/workflows/docs-publish.yml

    # Trigger deploy manually (workflow_dispatch)
    gh workflow run docs-publish.yml --ref development

    # Inspect Pages config (build_type should be "workflow")
    gh api repos/<org>/<repo>/pages --jq '"build_type: \(.build_type)\nstatus: \(.status)"'

    # Watch the most recent deploy to completion
    gh run watch $(gh run list --workflow=docs-publish.yml --limit 1 --json databaseId -q '.[0].databaseId') --exit-status

    # Verify the site is live (after CDN propagation)
    curl -sI https://<org>.github.io/<repo>/ | head -1   # expect HTTP/2 200
    ```

    When any of these fail, the corresponding fix is in the "When the
    site breaks" table above.

---

## What NOT to do

!!! danger "Common footguns"
    - **Don't hand-author files under `site/`** — that directory is the
      mkdocs build output, regenerated on every deploy.
    - **Don't commit `.cache/` or generated assets** — `.gitignore` excludes
      them. If you see one in `git status`, fix the ignore instead of
      staging it.
    - **Don't edit the workflow** to change build behavior. The logic lives
      in `mbl-actionhub/docs-publish-mkdocs.yml`. Bump the `@vX.Y.Z` pin in
      `.github/workflows/docs-publish.yml` to upgrade.
    - **Don't add a `docs/CNAME` file** to set a custom domain — configure
      it via the repo's Pages settings (UI or `gh api ... -f cname=...`).
    - **Don't author Liquid templating** in markdown. The mkdocs site
      doesn't process it, but `mkdocs-macros-plugin` (enabled in some
      consumer libraries) rejects stray Liquid-style brace syntax with
      "Macro Syntax Error" at build time, and legacy Jekyll Pages would
      also fail to render those files.

---

## Project-specific extensions

This file is generic. When your library has conventions that don't fit the
generic blueprint — a cookbook-recipe enforcement schema, a multi-module
modules/ index, custom validators — author them in the sibling
[`docs/DEVELOPMENT.md`](DEVELOPMENT.md).

| Belongs in `DEVELOPMENT-TEMPLATE.md` (this file) | Belongs in `DEVELOPMENT.md` (per-project) |
|---|---|
| How to add a page / section / image | Cookbook recipe enforcement (line caps, required langs, frontmatter fields) |
| Style guide (code blocks, admonitions, tables, links) | Per-module page conventions (README-embed flavor vs placeholder) |
| Local preview command | Library-specific validators ("every `cmp-*` module has a `docs/modules/` page") |
| Pipeline architecture | Legacy directory migration plan |
| Generic agent recipes / invariants / scaling | Library-specific recipes / invariants / scaling cues |
| `Home.md` ↔ `index.md` dual-surface rule | Custom mkdocs plugins or macros |
