---
title: "Docs authoring guide"
---

# Docs authoring guide

How to write, structure, and ship the documentation that lives under `docs/`.
KmpToolkit has a richer docs surface than a single-module library — this guide
captures the three content types (narrative, cookbook recipes, per-module
pages), the conventions each one follows, and the publishing pipeline that
serves them.

Serves two audiences: human contributors (read top to bottom) and AI agents
(grep for the structured sections — Agent quick reference, Invariants,
Scaling rubric, Validation commands).

## Agent quick reference

Common operations as copy-paste recipes. Each row is the *complete* action.

| Want to… | Recipe |
|----------|--------|
| Add a cookbook recipe | (1) `cp docs/_partials/cookbook-recipe-template.md docs/cookbook/<topic>/<slug>.md` (2) Fill in YAML frontmatter (`title`, `reviewed_by.date`, `reviewed_by.version`) (3) Author body — Quick start (≤15 lines kotlin) + Caveats (per-platform bullets) + Related links (4) Append entry to `docs/cookbook/<topic>/index.md` recipe list. **Don't** add to `mkdocs.yml` nav (recipes are reached via topic index only). |
| Add a cookbook topic | (1) `mkdir docs/cookbook/<topic>` (2) Author `docs/cookbook/<topic>/index.md` listing recipes + relevant modules (3) Add `- <Title>: cookbook/<topic>/index.md` under Cookbook in `mkdocs.yml` → `nav:`. |
| New module shipped — landing page (README path) | (1) Write `cmp-<name>/README.md` (the source of truth) (2) Create `docs/modules/cmp-<name>.md` that uses the `mkdocs-include-markdown-plugin` directive to embed `../../cmp-<name>/README.md` (see any existing module page for live syntax) (3) Add alphabetically into `mkdocs.yml` → `nav: Modules:` (4) Apply Dokka plugin in `cmp-<name>/build.gradle.kts` (`id("io.github.mobilebytelabs.kmptoolkit.dokka")`) + wire `JavadocJar.Dokka("dokkaGeneratePublicationHtml")` in `vanniktech.mavenPublish` config. |
| New module shipped — placeholder page (no README yet) | (1) Create `docs/modules/cmp-<name>.md` with the standard placeholder block ("Full docs coming soon, see GitHub source") + Maven Central + API ref note (2) Add to `mkdocs.yml` → `nav: Modules:` alphabetically (3) Apply Dokka plugin as above. |
| Migrate placeholder → README-embedded | (1) Write `cmp-<name>/README.md` (2) Replace the page body in `docs/modules/cmp-<name>.md` with the include-markdown directive (3) Same PR. |
| Add a narrative page | (1) Author `docs/<slug>.md` (2) Add `- <Title>: <slug>.md` to top-level nav in `mkdocs.yml`. |
| Test build locally | `pip install -r docs/requirements.txt && mkdocs build --strict` |
| Live preview | `mkdocs serve` → open `http://127.0.0.1:8000` |
| Trigger deploy manually | `gh workflow run docs-publish.yml --repo MobileByteLabs/KmpToolkit --ref development` |
| Upgrade docs pipeline | Bump `@vX.Y.Z` pin in `.github/workflows/docs-publish.yml` |
| Investigate `/` 404 | Verify `docs/index.md` exists; mkdocs needs it for the root. |
| Migrate a legacy `docs/<module>/` subdir | (1) Move usable content into `cmp-<module>/README.md` or into cookbook recipes (2) Delete the legacy subdir (3) Remove its line from `mkdocs.yml` → `exclude_docs:`. |

## Invariants

| If you change… | Also update… | Why |
|----------------|--------------|-----|
| New `cmp-<name>/` Gradle module | (1) `docs/modules/cmp-<name>.md` landing page (2) `mkdocs.yml` → `nav: Modules:` (3) `cmp-<name>/build.gradle.kts` Dokka plugin + JavadocJar config (4) `cmp-<name>/CHANGELOG.md` (5) root `CHANGELOG.md` entry | Otherwise module ships without docs, API ref, or release notes. |
| New cookbook recipe | The matching `docs/cookbook/<topic>/index.md` (add to recipe list) | Recipe is invisible unless the topic index links to it. |
| Recipe verified against new release | Bump `reviewed_by.date` + `reviewed_by.version` in the recipe's frontmatter | Without bump, recipe-freshness audit treats it as stale. |
| Module's `cmp-<name>/README.md` content | Nothing — `docs/modules/cmp-<name>.md` re-includes via plugin on every build | This is the point of the include-markdown pattern. |
| Module's `cmp-<name>/DEVELOPMENT.md` content | Nothing — DEVELOPMENT.md is per-module developer doc, not part of the site nav (but `cmp-*/DEVELOPMENT.md` IS in the workflow `paths:` trigger so deploy re-runs) | Deploy refreshes but no other file changes needed. |
| Module renamed (`cmp-old` → `cmp-new`) | (1) Rename `docs/modules/cmp-old.md` → `cmp-new.md` (2) Update `mkdocs.yml` nav entry (3) Grep for inbound `[link](cmp-old.md)` references in cookbook recipes + fix | Build fails on dangling links otherwise. |
| Migrated legacy `docs/<module>/` subdir | Remove its line from `mkdocs.yml` → `exclude_docs:` | Otherwise the migration is incomplete; future authors see it still excluded and may duplicate work. |
| Bumped caller pin (`@v1.9.1` → `@v1.10.x`) | Verify tag exists at `MobileByteLabs/mbl-actionhub` | Pinning a non-existent tag fails workflow resolution. |
| Bumped `mkdocs-material` in `docs/requirements.txt` | Run `mkdocs build --strict` locally + visually preview | Material can introduce theme breaks across minors. |

## The three surfaces

| Surface | Source | Lands at |
|---------|--------|----------|
| **mkdocs site** (canonical) | `docs/**`, `mkdocs.yml` | `https://mobilebytelabs.github.io/KmpToolkit/` |
| **Dokka API reference** | Kotlin `///` KDoc in source | Bundled inside each module's `-javadoc.jar` on Maven Central |
| **GitHub Wiki** | `docs/**` mirrored | `https://github.com/MobileByteLabs/KmpToolkit/wiki/<basename>` |

The mkdocs site is the primary product. The Dokka HTML is reference-only and
opens from Maven Central. The wiki is a passive mirror for users who prefer
GitHub's UI.

## The three content types

### 1. Narrative docs (`docs/index.md`, `docs/getting-started.md`)

Top-of-funnel pages. Authored prose, full freedom on structure. Keep these
short — the user is here to find a path into the library, not read a book.

### 2. Per-module pages (`docs/modules/cmp-*.md`)

One page per published module. 21 today, mirroring the 21 `cmp-*` modules
in the source tree. Two flavors:

- **README-embedded** (10 modules with `cmp-*/README.md`): the per-module
  page uses the `mkdocs-include-markdown-plugin` Liquid-style directive to
  embed the module's source-tree README. See any existing
  `docs/modules/cmp-network-monitor.md` for the live syntax. **Don't
  duplicate** the README content into the `docs/modules/` page — let the
  include do its job.
- **Placeholder** (11 modules without README.md yet): minimal "this module is
  shipped; full docs coming" page with a link to the GitHub source.

Whichever flavor: **always** include a one-line note about the API reference
being inside the `-javadoc.jar` on Maven Central.

When a module gains a `cmp-*/README.md`, convert its `docs/modules/cmp-*.md`
placeholder to the embedded form in the same PR.

### 3. Cookbook recipes (`docs/cookbook/{topic}/{recipe}.md`)

The bulk of the user-facing docs. Strict format, CI-enforced. Use the
template at [`_partials/cookbook-recipe-template.md`](_partials/cookbook-recipe-template.md):

```markdown
---
title: "How do I {task}?"
reviewed_by:
  date: 2026-06   # YYYY-MM — bumped on review
  version: 3.5.x  # last verified kmp-toolkit version
---

# How do I {task}?

## Quick start (minimal MWE)
```kotlin
// ≤ 15 lines runnable.
```

## Caveats / per-platform notes
- **Android:** …
- **iOS:** …

## Related
- Module: [cmp-{name}](../../modules/cmp-{name}.md)
- Sample: [`samples/sample-cmp-{name}/.../File.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-{name})
```

**Hard constraints (CI-checked):**

- ≤ 80 lines total (`wc -l`)
- ≥ 1 ` ```kotlin ` code block (`grep`)
- Frontmatter has `reviewed_by.date` (YYYY-MM) + `version`
- "How do I {task}?" title — phrased as a user question

**Soft conventions:**

- Quick start is ≤ 15 lines of copy-paste-runnable code
- Caveats prefer per-platform bullets over prose
- Related links: module page + sample + (optional) ADR

A new cookbook topic gets its own subdir + an `index.md` topic index that
lists the recipes + the underlying modules. See
[`docs/cookbook/network-monitor/index.md`](cookbook/network-monitor/index.md)
for the shape.

## Files with special meaning

| File | Used by | Purpose |
|------|---------|---------|
| `index.md` | mkdocs | Root URL of the site (`/`). |
| `_partials/cookbook-recipe-template.md` | authors (manual copy) | The canonical recipe shape. Don't edit casually — every recipe inherits. |
| `requirements.txt` | docs-publish workflow | Pinned mkdocs deps. Change a version here, not in the workflow. |
| `stylesheets/mbs-brand.css` | mkdocs | Brand polish. |

The `cmp-*/README.md` and `cmp-*/DEVELOPMENT.md` files at module roots are
**not under docs/** but feed the docs pipeline (paths trigger the
docs-publish workflow on push).

## Excluded legacy directories

These per-module docs subdirs predate the mkdocs site and use relative links
that resolve only in the GitHub UI. They live in `docs/` for backward compat
but are excluded from the mkdocs build via `mkdocs.yml` → `exclude_docs:`:

```
docs/app-intents/    docs/bubble/           docs/clipboard/
docs/firebase-analytics/  docs/in-app-update/    docs/intent-launcher/
docs/inter-app-comms/  docs/network-monitor/  docs/open-url/
docs/pdf-generator/  docs/remote-config/    docs/share/
docs/toast/          docs/user-tickets/     docs/BUBBLE.md
docs/CLIPBOARD_MONITOR.md  docs/FEATURE_REQUEST.md
docs/REMOTE_CONFIG.md  docs/REMOTE_CONFIG_SAMPLES.md
```

**Don't add new content to those directories.** Either:

- New module-level docs → write a `cmp-*/README.md` (it becomes the source
  for `docs/modules/cmp-*.md` via include-markdown)
- New how-to → write a cookbook recipe under `docs/cookbook/{topic}/`
- New narrative → write under `docs/` root + register in `mkdocs.yml` nav

When a legacy directory's content gets migrated to a current surface, drop
its line from `exclude_docs:` in the same PR.

## Adding new content

### A new cookbook recipe

1. Pick the topic subdir (or create one — see "A new cookbook topic" below)
2. Copy `_partials/cookbook-recipe-template.md` → `cookbook/{topic}/{slug}.md`
3. Fill in frontmatter + body (≤80 lines, ≥1 kotlin block)
4. Add the entry to `cookbook/{topic}/index.md`'s recipe list
5. **Don't** add individual recipes to `mkdocs.yml` nav — only the topic
   `index.md` is in nav; recipes are reached via the topic index

### A new cookbook topic

1. Create `cookbook/{topic}/index.md` listing the recipes + modules
2. Add the entry to `mkdocs.yml` → `nav: Cookbook:` (one line per topic)

### A new module landing page

When you ship a new `cmp-*` module:

1. Write `cmp-*/README.md` (the source of truth)
2. Create `docs/modules/cmp-{name}.md` with an include-markdown that points
   at the README
3. Add the entry to `mkdocs.yml` → `nav: Modules:` (alphabetical insertion)

### A new narrative page

Rare. Authored at `docs/{slug}.md` + registered in `mkdocs.yml` nav.

## Style guide

### Code blocks

Always declare language. mkdocs-material renders Kotlin, Swift, Bash, YAML,
JSON, TOML out of the box.

````markdown
```kotlin
val monitor = createNetworkMonitor()
```
````

Inline `code` for symbols, API names, flag names.

### Per-platform caveats

When behavior varies, structure as bullet list with bold platform name:

```markdown
- **Android:** auto-init via ContentProvider; no manual `init()` needed.
- **iOS:** call `Bundle.main.URLForResource(...)` from `applicationDidFinishLaunching`.
- **JVM Desktop:** prints to `System.out`; ANSI color enabled if TTY.
- **JS / wasmJs:** requires a user gesture on first invocation.
```

### Tables

Use for any comparison with ≥ 3 dimensions. The 21-module index in
[`index.md`](index.md) and the platform-support matrices are good examples.

### Links

- **Internal** (within `docs/`): relative paths. `mkdocs build --strict`
  validates these.
- **Cross-repo source** (`workspaces/mbs/...`): downgraded to INFO-level
  warning via `mkdocs.yml` → `validation.links.not_found: info`. Expected.
- **External**: full URLs.
- **Maven Central / API reference**: prefer
  `https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-{name}`
  over the Maven URL — better UX.

## Test locally

```bash
pip install -r docs/requirements.txt
mkdocs serve
# open http://127.0.0.1:8000
```

`mkdocs build --strict` is what CI runs. Most common cause of strict
failures: a new `cookbook/{topic}/{recipe}.md` added without an entry in the
topic `index.md`'s recipe list (the relative link from the index breaks).

## Recipe-freshness audit

Every recipe has `reviewed_by.date` + `version` in frontmatter. Once per
release cycle, scan for recipes whose `reviewed_by.version` is more than one
minor behind current and re-verify their code blocks against the current API.
Bump the date + version after each successful re-verification.

(No CI gate on this yet; expected manual cadence is per-release.)

## What NOT to do

- **Don't hand-author `site/`** — that directory is the mkdocs build output.
- **Don't add a new recipe outside the template format** — CI enforces the
  shape (line count + kotlin block presence). Use the template even for
  small recipes; consistency is the point.
- **Don't write content into a legacy `docs/{module}/` subdir** — those are
  excluded from the build. Use `cmp-*/README.md` or `docs/cookbook/` instead.
- **Don't edit the workflow** to change build behavior — the logic lives in
  `mbl-actionhub/docs-publish-mkdocs.yml`. Bump the `@vX.Y.Z` pin in
  `.github/workflows/docs-publish.yml` to upgrade.
- **Don't author Liquid templating** in markdown (other than the
  `include-markdown` plugin's own directive). Liquid-style braces break
  rendering if Pages is ever set back to legacy Jekyll.
- **Don't link recipes from `mkdocs.yml` nav directly** — keep nav to topic
  indexes only; recipes are reached via the index. Direct nav entries clutter
  the tab bar fast (12 recipes × 4 topics = 48 entries).

## When the site breaks

| Symptom | Cause | Fix |
|---------|-------|-----|
| `/` returns 404 | `docs/index.md` missing | Restore it. |
| Build fails: nav references file that doesn't exist | Stale `mkdocs.yml` nav entry | Remove the entry or create the file. |
| Cookbook recipe rejected by CI for length | Recipe > 80 lines | Split into two recipes OR move detail into a linked sample / ADR. |
| Cookbook recipe rejected for missing kotlin block | All code blocks are bash / yaml / etc. | Add at least one ` ```kotlin ` block, even if a 3-line snippet. |
| `mkdocs build --strict` fails on relative link | Cross-repo source link (`workspaces/mbs/...`) | Already downgraded to INFO via `validation.links.not_found: info`. If you're seeing ERROR, check that the link target literally cannot resolve in any way — even GitHub. |

## Pipeline architecture (one-paragraph version)

The mkdocs build + Pages deploy logic lives **once** in
[`mbl-actionhub/docs-publish-mkdocs.yml`](https://github.com/MobileByteLabs/mbl-actionhub/blob/main/.github/workflows/docs-publish-mkdocs.yml).
This repo's `.github/workflows/docs-publish.yml` is a 5-line caller pinned to
a specific version. The wiki sync is a separate workflow
(`sync-docs-to-wiki.yml`) that mirrors `docs/` to the GitHub Wiki via the
`mbl-actionhub-docshub` composite action. The Dokka API reference is built
inside the Maven publish pipeline (per-module `dokkaGeneratePublicationHtml`
task, bundled into `-javadoc.jar` via `vanniktech.mavenPublish`'s
`JavadocJar.Dokka("dokkaGeneratePublicationHtml")` config). All three
pipelines are independent; a failure in one doesn't block the others.

## Scaling rubric

KmpToolkit's docs structure today (21 modules, 12 cookbook recipes across
4 topics, 14 excluded legacy subdirs awaiting migration) is sized for
"≥ 15 modules with heavy how-to content." Use this rubric to understand
what to add/restructure as the library scales further:

| Current state | What to add next |
|---------------|------------------|
| **21 modules, 12 recipes today** (now) | Migrate one legacy `docs/<module>/` subdir per release until `exclude_docs:` is empty. Each migration: lift content into `cmp-<module>/README.md` (replaces `docs/modules/cmp-<module>.md` placeholder) and/or split into 1-3 cookbook recipes. |
| **25+ modules** | Group modules in nav by capability cluster (Inter-app comms, Network, Storage, etc.) instead of flat alphabetical. Edit `mkdocs.yml` → `nav: Modules:` to add subsections. |
| **30+ cookbook recipes** | Introduce sub-topics within a cookbook section (e.g. `cookbook/network-monitor/{detection,reaction,testing}/...`). Each sub-topic gets its own `index.md`. |
| **Recipe-freshness automation needed** | Add a CI gate that fails when ≥ N recipes have `reviewed_by.version` more than one minor behind current. (Not built today — manual audit per release.) |
| **Cookbook page count > 100** | Add a search-tag index page that groups recipes by tag (Android-only, requires-permission, async-flow, etc.). Tags live in recipe frontmatter; render via a custom mkdocs macro. |
| **Multi-version docs needed** (e.g. v3 + v4 coexisting) | Add `mike` plugin for versioned docs. Significant ceremony; only adopt when users genuinely need to read v3 docs after v4 ships. |

Anti-patterns:
- **Don't** create a new cookbook topic for a single recipe — wait for ≥ 3.
- **Don't** add per-module pages for modules without `cmp-*/README.md` AND
  without a clear "ships standalone" story — placeholder pages pile up.
- **Don't** link cookbook recipes from `mkdocs.yml` nav directly — keep nav
  at topic-index level. Currently 12 recipes; direct nav would mean 12
  entries instead of 4.

## Validation commands

Exact CLI snippets agents can run without modification.

```bash
# Strict build (what CI runs)
pip install -r docs/requirements.txt
mkdocs build --strict

# Live preview
mkdocs serve  # → http://127.0.0.1:8000

# Show only WARNING/ERROR from strict build (filter INFO noise)
mkdocs build --strict 2>&1 | grep -E "^(WARNING|ERROR)"

# Count cookbook recipes
find docs/cookbook -name "*.md" -not -name "index.md" | wc -l

# Audit: every cookbook recipe ≤ 80 lines (AC12)
for f in docs/cookbook/**/*.md; do
  [ "$(basename "$f")" = "index.md" ] && continue
  lines=$(wc -l < "$f")
  [ "$lines" -gt 80 ] && echo "OVERLONG ($lines lines): $f"
done

# Audit: every cookbook recipe has ≥ 1 kotlin block (AC13)
for f in docs/cookbook/**/*.md; do
  [ "$(basename "$f")" = "index.md" ] && continue
  grep -q '```kotlin' "$f" || echo "MISSING kotlin block: $f"
done

# Audit: cookbook recipes with stale reviewed_by.version (not 3.5.x)
grep -L "version: 3.5" docs/cookbook/**/*.md 2>/dev/null | grep -v "index.md"

# Cross-check: every cmp-* Gradle module has a docs/modules/cmp-*.md
diff <(ls -1d cmp-*/ 2>/dev/null | sed 's|/||') \
     <(ls -1 docs/modules/cmp-*.md | xargs -n1 basename | sed 's|\.md||') \
  | head -20

# Cross-check: every cmp-* module applies the Dokka convention plugin
for d in cmp-*/; do
  grep -q "io.github.mobilebytelabs.kmptoolkit.dokka" "$d/build.gradle.kts" \
    || echo "MISSING Dokka plugin: $d"
done

# Show current caller pin
grep "docs-publish-mkdocs.yml@" .github/workflows/docs-publish.yml

# Trigger deploy manually
gh workflow run docs-publish.yml --repo MobileByteLabs/KmpToolkit --ref development

# Inspect Pages config (build_type must be "workflow")
gh api repos/MobileByteLabs/KmpToolkit/pages --jq '"build_type: \(.build_type)\nstatus: \(.status)"'

# Watch latest docs-publish run
gh run watch $(gh run list --workflow=docs-publish.yml --repo MobileByteLabs/KmpToolkit --limit 1 --json databaseId -q '.[0].databaseId') --exit-status

# Verify site is live
curl -sI https://mobilebytelabs.github.io/KmpToolkit/ | head -1   # expect HTTP/2 200
```

Each failure in these checks maps to a fix in the "When the site breaks"
table above. Run all the audits before opening a release PR to catch
drift before users see it.
