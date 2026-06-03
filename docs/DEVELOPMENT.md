---
title: Docs guide (KmpToolkit)
description: KmpToolkit-specific authoring conventions — three content types, cookbook recipe enforcement, per-module page duality, legacy directory migration, Dokka API ref integration.
---

# Docs guide (KmpToolkit)

!!! abstract "What this file is"
    KmpToolkit-specific extensions on top of the canonical blueprint in
    [`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md) — **read that first.**

    This file is **owned by this repo**. Unlike `DEVELOPMENT-TEMPLATE.md`
    (which is sync'd from `mbl-library-template-kmp`), this one is
    hand-authored and captures KmpToolkit's distinctive shape:

    - A **third published surface** (Dokka API reference bundled inside `-javadoc.jar`)
    - **Three content types** (narrative, per-module pages, cookbook recipes)
    - **Cookbook recipe enforcement** (CI-checked format constraints)
    - **Legacy directory migration** (14 excluded `docs/<module>/` subdirs)

---

## The three surfaces

KmpToolkit publishes documentation across three surfaces, each with its
own pipeline.

=== "mkdocs site (canonical)"

    **Source:** `docs/**`, `mkdocs.yml`

    **Lands at:** [`https://mobilebytelabs.github.io/KmpToolkit/`](https://mobilebytelabs.github.io/KmpToolkit/)

    The primary product. Everything in this guide is about authoring for
    this surface.

=== "Dokka API reference (KmpToolkit-specific)"

    **Source:** Kotlin `///` KDoc in source files

    **Lands at:** Bundled inside each module's `-javadoc.jar` on Maven Central

    Built per-module via the convention plugin at
    `build-logic/convention/src/main/kotlin/DokkaConventionPlugin.kt`.
    Each `cmp-*/build.gradle.kts` applies it
    (`id("io.github.mobilebytelabs.kmptoolkit.dokka")`) and wires the output
    into `vanniktech.mavenPublish` via
    `JavadocJar.Dokka("dokkaGeneratePublicationHtml")`. Users open the HTML
    from the Maven Central UI.

=== "GitHub Wiki"

    **Source:** `docs/**` mirrored

    **Lands at:** `https://github.com/MobileByteLabs/KmpToolkit/wiki/<basename>`

    Passive mirror for users who prefer GitHub's UI. Generic conventions
    in `DEVELOPMENT-TEMPLATE.md` cover this.

---

## The three content types

### 1. Narrative docs

`docs/index.md`, `docs/getting-started.md` — top-of-funnel pages. Authored
prose, full freedom on structure. Same conventions as the generic guide
describes for any library.

### 2. Per-module pages

One page per published module under `docs/modules/cmp-*.md`. **Two flavors,
both registered in `mkdocs.yml` → `nav: Modules:`:**

=== "README-embedded (recommended)"

    **Used by:** 10 modules today (those with `cmp-*/README.md`)

    **Pattern:** the per-module page uses the `mkdocs-include-markdown-plugin`
    Liquid-style directive to embed the module's source-tree README. See
    [`docs/modules/cmp-network-monitor.md`](modules/cmp-network-monitor.md)
    for live syntax.

    !!! warning "Don't duplicate the README content"
        Let the include do its job. The per-module page should be a thin
        wrapper that pulls in the source-tree README at build time.

=== "Placeholder (interim)"

    **Used by:** 11 modules today (those without `cmp-*/README.md` yet)

    **Pattern:** a minimal page noting "shipped; full docs coming" + GitHub
    source link + a one-line API reference note pointing at the
    `-javadoc.jar` on Maven Central.

    !!! tip "Promote to README-embedded when the README lands"
        When a module gains a `cmp-*/README.md`, convert its
        `docs/modules/cmp-*.md` placeholder to the embedded form **in the
        same PR**. Reduces the gap between source-tree docs and site docs.

!!! info "Always include the Maven Central API reference note"
    Both flavors must mention that the API reference is inside the
    `-javadoc.jar` on Maven Central. This is the only place users discover
    that pattern.

### 3. Cookbook recipes

`docs/cookbook/{topic}/{recipe}.md` — the bulk of user-facing how-to docs.
**Strict format, CI-enforced.** Use the template at
[`_partials/cookbook-recipe-template.md`](_partials/cookbook-recipe-template.md)
as the starting point.

!!! danger "Hard constraints (CI-checked)"
    - ≤ 80 lines total (`wc -l`)
    - ≥ 1 ` ```kotlin ` code block (`grep`)
    - Frontmatter has `reviewed_by.date` (YYYY-MM) + `reviewed_by.version`
    - "How do I {task}?" title — phrased as a user question

!!! tip "Soft conventions"
    - Quick start is ≤ 15 lines of copy-paste-runnable code
    - Caveats prefer per-platform bullets over prose
    - Related links: module page + sample + (optional) ADR

A new cookbook **topic** gets its own subdir + an `index.md` topic index
listing recipes + relevant modules. See
[`docs/cookbook/network-monitor/index.md`](cookbook/network-monitor/index.md)
for the shape.

---

## Excluded legacy `docs/<module>/` subdirs

These per-module docs subdirs predate the mkdocs site and use relative
links that resolve only in the GitHub UI. They live in `docs/` for backward
compat but are excluded from the mkdocs build via `mkdocs.yml` → `exclude_docs:`.

??? note "Full list (14 directories + 5 root files)"
    ```
    docs/app-intents/
    docs/bubble/
    docs/clipboard/
    docs/firebase-analytics/
    docs/in-app-update/
    docs/intent-launcher/
    docs/inter-app-comms/
    docs/network-monitor/
    docs/open-url/
    docs/pdf-generator/
    docs/remote-config/
    docs/share/
    docs/toast/
    docs/user-tickets/

    docs/BUBBLE.md
    docs/CLIPBOARD_MONITOR.md
    docs/FEATURE_REQUEST.md
    docs/REMOTE_CONFIG.md
    docs/REMOTE_CONFIG_SAMPLES.md
    ```

!!! danger "Don't add new content to those directories"
    Lift content into one of the three current surfaces:

    - **Module-level reference** → write `cmp-*/README.md` (becomes source
      for `docs/modules/cmp-*.md` via include-markdown)
    - **How-to** → cookbook recipe under `docs/cookbook/{topic}/`
    - **Narrative** → top-level `docs/<slug>.md`

When a legacy directory's content gets migrated, drop its line from
`exclude_docs:` in the same PR.

---

## Agent quick reference

Generic recipes (add a page, add an asset, override brand colors, test,
deploy) live in [`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md#agent-quick-reference).
Below are operations specific to KmpToolkit's three-content-type structure.

| Want to… | Recipe |
|----------|--------|
| Add a cookbook recipe | (1) `cp docs/_partials/cookbook-recipe-template.md docs/cookbook/<topic>/<slug>.md` (2) Fill in YAML frontmatter (`title`, `reviewed_by.date`, `reviewed_by.version`) (3) Author body — Quick start (≤15 lines kotlin) + Caveats + Related links (4) Append entry to `docs/cookbook/<topic>/index.md` recipe list. **Don't** add to `mkdocs.yml` nav. |
| Add a cookbook topic | (1) `mkdir docs/cookbook/<topic>` (2) Author `docs/cookbook/<topic>/index.md` listing recipes + relevant modules (3) Add `- <Title>: cookbook/<topic>/index.md` under Cookbook in `mkdocs.yml` → `nav:` |
| New module shipped — README-embedded page | (1) Write `cmp-<name>/README.md` (2) Create `docs/modules/cmp-<name>.md` with the include-markdown directive (see existing modules for syntax) (3) Add alphabetically into `mkdocs.yml` → `nav: Modules:` (4) Apply Dokka plugin in `cmp-<name>/build.gradle.kts` (`id("io.github.mobilebytelabs.kmptoolkit.dokka")`) + wire `JavadocJar.Dokka("dokkaGeneratePublicationHtml")` in `vanniktech.mavenPublish` config |
| New module shipped — placeholder page | Same as above, but step (2) uses the standard placeholder block ("Full docs coming soon, see GitHub source") + Maven Central + API ref note instead of include-markdown |
| Migrate placeholder → README-embedded | (1) Write `cmp-<name>/README.md` (2) Replace the page body in `docs/modules/cmp-<name>.md` with the include-markdown directive (3) Same PR |
| Migrate a legacy `docs/<module>/` subdir | (1) Move usable content into `cmp-<module>/README.md` or into cookbook recipes (2) Delete the legacy subdir (3) Remove its line from `mkdocs.yml` → `exclude_docs:` |

---

## Invariants

Generic invariants (`Home.md` ↔ `index.md` sync, new page → nav, etc.)
live in [`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md#invariants).
Below are KmpToolkit-specific cross-file edit obligations.

| If you change… | Also update… | Why |
|----------------|--------------|-----|
| New `cmp-<name>/` Gradle module | (1) `docs/modules/cmp-<name>.md` (2) `mkdocs.yml` → `nav: Modules:` (3) `cmp-<name>/build.gradle.kts` Dokka plugin + JavadocJar config (4) `cmp-<name>/CHANGELOG.md` (5) root `CHANGELOG.md` entry | Module ships without docs, API ref, or release notes otherwise |
| New cookbook recipe | The matching `docs/cookbook/<topic>/index.md` (add to recipe list) | Recipe is invisible unless the topic index links to it |
| Recipe verified against new release | Bump `reviewed_by.date` + `reviewed_by.version` in frontmatter | Without bump, recipe-freshness audit treats it as stale |
| Module's `cmp-<name>/README.md` content | Nothing — `docs/modules/cmp-<name>.md` re-includes via plugin on every build | This is the point of the include-markdown pattern |
| Module renamed (`cmp-old` → `cmp-new`) | (1) Rename `docs/modules/cmp-old.md` → `cmp-new.md` (2) Update `mkdocs.yml` nav entry (3) Grep for inbound `[link](cmp-old.md)` references in cookbook recipes + fix | Build fails on dangling links |
| Migrated legacy `docs/<module>/` subdir | Remove its line from `mkdocs.yml` → `exclude_docs:` | Migration is incomplete otherwise; future authors see it excluded and may duplicate work |

---

## Scaling rubric

Generic scaling rubric (single-module → multi-module shape) lives in
[`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md#scaling-rubric).
KmpToolkit is sized for ≥ 15 modules with heavy how-to content. Below: what
to add as it scales further.

| Current state | What to add next |
|---------------|------------------|
| **21 modules, 12 recipes today** (now) | Migrate one legacy `docs/<module>/` subdir per release until `exclude_docs:` is empty. Each migration: lift content into `cmp-<module>/README.md` (replaces `docs/modules/` placeholder) and/or split into 1-3 cookbook recipes. |
| **25+ modules** | Group modules in nav by capability cluster (Inter-app comms, Network, Storage, etc.) instead of flat alphabetical. Edit `mkdocs.yml` → `nav: Modules:` to add subsections. |
| **30+ cookbook recipes** | Introduce sub-topics within a cookbook section (e.g. `cookbook/network-monitor/{detection,reaction,testing}/...`). Each sub-topic gets its own `index.md`. |
| **Recipe-freshness automation needed** | Add a CI gate that fails when ≥ N recipes have `reviewed_by.version` more than one minor behind current. (Not built today — manual audit per release.) |
| **Cookbook page count > 100** | Add a search-tag index page that groups recipes by tag (Android-only, requires-permission, async-flow, etc.). Tags live in recipe frontmatter; render via a custom mkdocs macro. |
| **Multi-version docs needed** (v3 + v4 coexisting) | Add `mike` plugin for versioned docs. Significant ceremony; adopt only when users genuinely need v3 docs after v4 ships. |

!!! warning "Anti-patterns"
    - **Don't** create a new cookbook topic for a single recipe — wait for ≥ 3.
    - **Don't** add per-module pages for modules without `cmp-*/README.md`
      AND without a clear "ships standalone" story — placeholder pages
      pile up.
    - **Don't** link cookbook recipes from `mkdocs.yml` nav directly. Keep
      nav at topic-index level. Currently 12 recipes; direct nav would mean
      12 entries instead of 4.

---

## Validation commands

Generic validation (strict build, link audit, site liveness probe) lives
in [`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md#validation-commands).
Below: audits specific to KmpToolkit's cookbook + multi-module structure.

??? example "Full KmpToolkit-specific audit suite"
    ```bash
    # Count cookbook recipes
    find docs/cookbook -name "*.md" -not -name "index.md" | wc -l

    # Audit: every cookbook recipe ≤ 80 lines (constraint AC12)
    for f in docs/cookbook/**/*.md; do
      [ "$(basename "$f")" = "index.md" ] && continue
      lines=$(wc -l < "$f")
      [ "$lines" -gt 80 ] && echo "OVERLONG ($lines lines): $f"
    done

    # Audit: every cookbook recipe has ≥ 1 kotlin block (constraint AC13)
    for f in docs/cookbook/**/*.md; do
      [ "$(basename "$f")" = "index.md" ] && continue
      grep -q '```kotlin' "$f" || echo "MISSING kotlin block: $f"
    done

    # Audit: cookbook recipes with stale reviewed_by.version (not current 3.5.x)
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

    # Verify the convention plugin sources the Dokka v2 task name
    grep -r "dokkaGeneratePublicationHtml" cmp-*/build.gradle.kts | wc -l
    # Expect: 21 (one per cmp-* module)
    ```

!!! tip "Run all audits before opening a release PR"
    Catches drift before users see it. The cookbook line/kotlin/version
    audits are particularly important — CI's recipe-shape constraints exist
    because the format is what makes the cookbook scannable.
