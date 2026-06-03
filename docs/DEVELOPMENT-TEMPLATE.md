# Docs authoring guide (template)

The canonical, reusable blueprint for writing the documentation that lives
under `docs/`. This file is **owned by `mbl-library-template-kmp`** and
sync'd into every consumer repo via `sync-dirs.sh` (SYNC_FILES). Do not
edit this file in a consumer repo — your edits will be overwritten on the
next sync. Instead, override or extend per-project in `docs/DEVELOPMENT.md`
(see §"Project-specific extensions" at the bottom).

Serves two audiences: human contributors (read top to bottom) and AI agents
(grep for the structured sections — Agent quick reference, Invariants,
Scaling rubric, Validation commands).

## Agent quick reference

Common operations as copy-paste recipes. Each row is the *complete* action;
follow every step.

| Want to… | Recipe |
|----------|--------|
| Add a narrative page | (1) Write `docs/<slug>.md` (2) Add `- <Title>: <slug>.md` under the right section in `mkdocs.yml` → `nav:` (3) `git add` both → commit |
| Add a section | (1) `mkdir docs/<section>` (2) Author `docs/<section>/<first-page>.md` (3) Add `- <Section>:\n  - <Page>: <section>/<first-page>.md` block to `mkdocs.yml` → `nav:` |
| Add an image / asset | (1) Drop into `docs/images/` (create if needed) (2) Reference as `![Alt](images/<file>)` from any page |
| Override brand color | Edit `docs/stylesheets/mbs-brand.css` → `--md-primary-fg-color` (header/links) + `--md-accent-fg-color` (highlights, copy button) |
| Test build locally | `pip install -r docs/requirements.txt && mkdocs build --strict` |
| Live preview | `mkdocs serve` → open `http://127.0.0.1:8000` |
| Upgrade docs pipeline | Bump `@vX.Y.Z` pin in `.github/workflows/docs-publish.yml` → next push redeploys via new version |
| Trigger deploy manually | `gh workflow run docs-publish.yml --ref development` |
| Investigate `/` 404 | Verify `docs/index.md` exists (mkdocs requires it for the root URL) |
| Investigate site shows old content | Cmd-Shift-R (CDN); wait 1-2 min; if persistent, check Pages config: `gh api repos/<org>/<repo>/pages` |
| Edit Home/index content | **Edit both** `docs/Home.md` and `docs/index.md` — they MUST stay in sync (see Invariants below) |
| Disable Jekyll on Pages site | (Already done — `actions/configure-pages@v5` with `enablement: true` in reusable workflow at `v1.9.1+` handles this) |

## Invariants

When you touch the file in the first column, you MUST also update the
files in the second column in the same commit (or the next push will break
something).

| If you change… | Also update… | Why |
|----------------|--------------|-----|
| `docs/Home.md` content | `docs/index.md` (mirror) | Wiki uses Home.md; mkdocs uses index.md. Divergence = surfaces show different content. |
| `docs/index.md` content | `docs/Home.md` (mirror) | Same reason, reversed. |
| Added a new `docs/<page>.md` (user-facing) | `mkdocs.yml` → `nav:` | Page exists on disk but no nav entry = invisible on site. |
| New section directory | `mkdocs.yml` → `nav:` (new section block) | Same — directory contents don't appear without nav registration. |
| Renamed a page | (1) Update all inbound `[link](old.md)` references (2) Optionally add a redirect via `mkdocs-redirects` plugin | Otherwise old URLs 404 and inbound links break. |
| Deleted a page | (1) Remove from `mkdocs.yml` nav (2) Grep for inbound links + fix or delete them | Strict-mode build fails on dangling nav entries. |
| Bumped `mkdocs-material` in `docs/requirements.txt` | Run `mkdocs build --strict` locally — Material can introduce theme breaks across minors | CI catches this, but you waste a deploy cycle if you skip local verification. |
| Bumped caller pin (`@v1.9.1` → `@v1.10.x`) in `.github/workflows/docs-publish.yml` | Verify the new tag actually exists at `MobileByteLabs/mbl-actionhub` | Pinning a non-existent tag fails at workflow-resolution time. |

## Two surfaces, one source

Files in `docs/` feed **two** published surfaces:

| Surface | Pipeline | Lands at |
|---------|----------|----------|
| **mkdocs site** (canonical) | `.github/workflows/docs-publish.yml` → `mbl-actionhub/docs-publish-mkdocs.yml` | `https://<org>.github.io/<repo>/` |
| **GitHub Wiki** | `.github/workflows/sync-docs-to-wiki.yml` → `mbl-actionhub-docshub` composite | `https://github.com/<org>/<repo>/wiki/<basename>` |

Both pipelines trigger on push to `development` whenever `docs/**` or `mkdocs.yml`
changes. There is no "write once, sync later" step — the merge is the deploy.

## Files with special meaning

| File | Used by | Purpose |
|------|---------|---------|
| `index.md` | mkdocs | Root URL of the site (`/`). Required — without it the root returns 404. |
| `Home.md` | wiki | Wiki's home page. GitHub Wiki indexes by basename; this name is hard-coded. Excluded from the mkdocs build (`exclude_docs:`) to avoid a duplicate `/Home/` page. |
| `_Sidebar.md` | wiki | Wiki sidebar nav. Excluded from the mkdocs build. |
| `requirements.txt` | docs-publish workflow | Pinned mkdocs deps — change a version here, not in the workflow. |
| `stylesheets/mbs-brand.css` | mkdocs | Brand polish. Override colors for your library here. |

`index.md` and `Home.md` are duplicates by design: mkdocs needs `index.md`,
wiki needs `Home.md`, and they should always show the same landing content.
When you edit one, edit the other.

## Adding a new page (3-step)

1. **Write the markdown** under the appropriate section directory
   (`docs/<section>/<page-slug>.md`). Use kebab-case for filenames.
2. **Register it in `mkdocs.yml`** under `nav:`. The section heading you put it
   under is what appears in the site's tab bar.
3. **Push to `development`** — the docs-publish workflow rebuilds and
   redeploys the site within ~30 seconds.

Wiki picks up the new file automatically (no nav registration needed —
wiki uses `_Sidebar.md` for nav).

## Section conventions

The starter `mkdocs.yml` declares these sections. Use them or replace them,
but keep the structure shallow (≤2 levels of nesting):

| Section | What goes here |
|---------|----------------|
| **Home** | Library overview, badges, "why this exists" — same content as `index.md`/`Home.md`. |
| **Getting started** | Install, first usage, key concepts, migration guides. Each page should be runnable end-to-end. |
| **Features** | One page per top-level capability. Title each page as a noun phrase ("Foreground tasks"), not a verb ("How to run foreground tasks"). |
| **Platform support** | Per-platform pages + matrix tables. Capture quirks, capabilities, and not-yet-supported APIs. |
| **Operations** | Performance, security, audits, threat models — anything ops-oriented for production deployers. |
| **Release** | Release process, postmortem template, deprecation policy. |

## Style guide

### Code blocks

Always declare the language. mkdocs-material renders Kotlin, Swift, Bash, YAML,
JSON, and TOML out of the box. Never leave a code block unlabeled.

````markdown
```kotlin
val worker = MyWorker()
```
````

Inline `code` for symbols + flag names. Use `**bold**` for first-mention
emphasis of a concept (sparingly).

### Admonitions

Use admonitions (`!!! note`, `!!! warning`, `!!! tip`) for callouts that break
the flow but are important. Don't use them for body text.

```markdown
!!! warning "iOS background time is finite"
    BGTaskScheduler grants ~30s of execution. Long work needs
    `URLSession.uploadTask` instead.
```

### Tables

Use tables for any comparison with ≥3 dimensions (platforms × features,
versions × behaviors, options × tradeoffs). Inline lists are fine for
≤2 dimensions.

### Links

- **Internal** (within the docs/ tree): use relative paths
  (`[Quick start](getting-started/quick-start.md)`). mkdocs validates these on
  build; broken links surface as INFO-level warnings.
- **External**: full URLs.
- **Cross-repo source** (`workspaces/mbs/...`): the warning is INFO-level and
  expected — the link works on GitHub source view but not on the docs site.
- **API reference**: link to the published Dokka HTML (bundled inside each
  module's `-javadoc.jar`) or to Maven Central.

### Per-platform caveats

When a feature behaves differently across platforms, structure as:

```markdown
## Per-platform notes

- **Android:** ... 
- **iOS:** ...
- **JVM Desktop:** ...
- **JS / wasmJs:** ...
```

This pattern is grep-friendly and reads consistently across the site.

## Test locally before pushing

```bash
pip install -r docs/requirements.txt
mkdocs serve
# open http://127.0.0.1:8000
```

`mkdocs build --strict` is what CI runs. If strict fails locally, it'll fail
in CI. Most common cause: a nav entry references a file that doesn't exist,
or a relative link points outside `docs/`.

## What NOT to do

- **Don't hand-author files under `site/`** — that directory is the mkdocs
  build output, regenerated on every deploy.
- **Don't commit `.cache/` or generated assets** — `.gitignore` already
  excludes them; if you see one in `git status`, fix the ignore instead of
  staging it.
- **Don't edit the workflow** to change build behavior. The build logic
  lives in `mbl-actionhub/docs-publish-mkdocs.yml`. Bump the `@vX.Y.Z` pin
  in `.github/workflows/docs-publish.yml` to upgrade.
- **Don't add a `docs/CNAME` file** to set a custom domain — configure it
  via the repo's Pages settings (UI or `gh api ... -f cname=...`).
- **Don't author Liquid templating** in markdown. The mkdocs site doesn't
  process Liquid, but if Pages is ever misconfigured back to legacy Jekyll
  it will fail to render those files. (The `mkdocs-macros-plugin` —
  enabled in some consumer libraries — will also reject stray Liquid-style
  brace syntax with "Macro Syntax Error" at build time.)

## Adding a new section to the nav

Edit `mkdocs.yml` → `nav:` block. Each entry is `Section Title:` followed by
either a single `page.md` or a nested list of pages. Keep section titles
≤3 words; the navigation tab bar truncates long titles.

```yaml
nav:
  - Home: index.md
  - Getting started:
      - Installation: getting-started/installation.md
      - Quick start: getting-started/quick-start.md
  - New section:
      - First page: new-section/first-page.md
```

## Updating the brand

`docs/stylesheets/mbs-brand.css` declares primary + accent colors via
`--md-primary-fg-color` and `--md-accent-fg-color`. Change these to match
your library's brand. Tweak typography spacing in the same file — keep
changes small; mkdocs-material's defaults are well-considered.

For the palette toggle (light/dark mode), edit `mkdocs.yml` → `theme.palette`.

## When the site breaks

| Symptom | Cause | Fix |
|---------|-------|-----|
| `/` returns 404 | `docs/index.md` missing | Add it (duplicate of `Home.md`). |
| Build fails: "unrecognized relative link" | A `*.md` link points outside `docs/` | Either fix the link or accept it (it'll surface as INFO, not error). |
| Build fails: "nav references file that doesn't exist" | `mkdocs.yml` nav has a stale entry | Remove the entry or create the file. |
| Pages deploy succeeds but site shows old content | CDN cache | Hard refresh (Cmd-Shift-R). Usually clears within 1-2 minutes. |
| `configure-pages` errors with "Get Pages site failed" | Repo's Pages source not set to "GitHub Actions" | Bump caller workflow pin to `v1.9.1+` (auto-enables) OR flip Settings → Pages → Source manually. |

## Wiki-specific notes

- Wiki sync requires the wiki to be initialized (Settings → Features → Wikis
  → enabled, then create any first page via the Wiki UI). Without this, the
  composite errors with "Repository not found".
- `_Sidebar.md` in `docs/` becomes the wiki's left nav. If you want wiki
  auto-sidebar generation, set `sidebar-mode: auto` in the workflow inputs
  (the default).
- Wiki indexes by basename — two files named the same in different subdirs
  collide. The docshub composite resolves by subdir-prefixing the slug, but
  it's cleaner to avoid the collision in the first place.

## Pipeline architecture (one-paragraph version)

The build + deploy logic lives **once** in
[`mbl-actionhub/docs-publish-mkdocs.yml`](https://github.com/MobileByteLabs/mbl-actionhub/blob/main/.github/workflows/docs-publish-mkdocs.yml).
This repo's `.github/workflows/docs-publish.yml` is a 5-line caller that
pins to a specific version (`@v1.9.1` at time of writing). Upgrades happen
in one place; consumers bump the pin to opt in.

## Scaling rubric

The starter `mkdocs.yml` ships with a 5-section nav (Home / Getting started
/ Features / Platform support / Operations / Release). That's intentionally
optimistic — most early-stage libraries shouldn't pre-create all of those.

Start flat. Split a section only when it has **≥ 4 pages**. Use this rubric
to choose the right structure for your library's current size:

| Library shape | docs/ structure |
|---------------|-----------------|
| **1 module, < 5 pages** | Flat: `index.md`, `Home.md`, `getting-started.md`, `api.md`. No subdirs. `mkdocs.yml` nav = 4 entries. |
| **1-2 modules, 5-15 pages** | Add `getting-started/` + `features/` subdirs. Keep operations + release inline as single pages until each grows to ≥ 4 pages. |
| **2-5 modules, 15-30 pages** | Adopt the full starter nav (6 sections). Each section has its own subdir. Add a `platform-support/` matrix page. |
| **5+ modules** | Introduce `docs/modules/<module>.md` per-module landing pages. Use `mkdocs-include-markdown-plugin` to mirror each module's source-tree README. Add a Modules section to nav. |
| **Heavy how-to content** (≥ 10 task-oriented pages) | Introduce a `docs/cookbook/<topic>/<recipe>.md` structure. One page per task, named as a user question ("How do I X?"). See KmpToolkit for the live pattern. |
| **Heavy API reference** | Don't try to render API ref in mkdocs. Bundle Dokka HTML inside `-javadoc.jar` (per-module `dokkaGeneratePublicationHtml` + `vanniktech.mavenPublish.JavadocJar.Dokka` config) and link to Maven Central from the mkdocs site. |

Anti-pattern: pre-structuring for hypothetical growth. Empty sections
(headings with one stub page) look unprofessional and add nav clutter.
Three nav entries with content > seven nav entries half-filled.

## Validation commands

Exact CLI snippets agents can run without modification.

```bash
# Strict build (what CI runs)
pip install -r docs/requirements.txt
mkdocs build --strict

# Live preview
mkdocs serve  # → http://127.0.0.1:8000

# List every WARNING / ERROR from a strict build (filter out INFO noise)
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

# Watch the most recent deploy run to completion
gh run watch $(gh run list --workflow=docs-publish.yml --limit 1 --json databaseId -q '.[0].databaseId') --exit-status

# Verify the site is live (after CDN propagation)
curl -sI https://<org>.github.io/<repo>/ | head -1   # expect HTTP/2 200
```

When any of these fail, the corresponding fix is in the troubleshooting
table above ("When the site breaks").

## Project-specific extensions

This file (`docs/DEVELOPMENT-TEMPLATE.md`) is generic and sync'd from the
template — your library cannot edit it (next sync would clobber the change).

When your library has conventions that don't fit the generic blueprint —
a cookbook-recipe enforcement schema, a multi-module modules/ index, a
release-cadence rubric, custom validators — author them in a sibling file
named **`docs/DEVELOPMENT.md`**. That file:

- Lives in your library only (NOT sync'd from the template)
- Layers on top of this generic guide rather than replacing it
- Should be linked from your library's `docs/DEVELOPMENT.md` to this one
  as the "see also: generic guide"

Examples of content that belongs in the per-project `DEVELOPMENT.md`, not here:

- Cookbook recipe enforcement (line caps, mandatory code-block languages,
  required frontmatter fields like `reviewed_by`)
- Per-module page conventions (README-embed vs placeholder duality)
- Legacy directory migration tasks
- Library-specific validation commands (e.g. "every `cmp-*` module has a
  matching `docs/modules/` page")
- Custom mkdocs plugins or macros
