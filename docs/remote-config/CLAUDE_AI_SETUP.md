# cmp-remote-config — Claude AI Setup Guide

Use `/sync-remote-config` (or `/lib-sync cmp-remote-config`) to automatically verify and wire
`cmp-remote-config` into your KMP project — including Supabase schema delta.

---

## Quick Commands

```bash
/sync-remote-config                 # Full sync (all 3 gates)
/sync-remote-config --check         # Dry run — show status, no writes
/sync-remote-config --migrate-only  # Gate 2 only (Supabase schema delta)
/sync-remote-config --wiring-only   # Gates 3a+3b only (DSL + RemoteConfigHost)
/lib-sync cmp-remote-config         # Same as /sync-remote-config
```

---

## What the Skill Does

```
/sync-remote-config
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                  │
│  Check: kmptoolkit-remote-config:4.0.0+ in libs.versions    │
│  Fix:   Auto-insert                                         │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Delta                              │
│  Fetch: Live schema for product_remote_config               │
│  Delta: ADD missing columns (25 cols) — NEVER DROP          │
│  Also:  Check device_impressions table + 3 RPCs             │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 3a: remoteConfig { … } DSL                            │
│  Check: `remoteConfig {` block inside any module { } in **/*.kt │
│  Fix:   Auto-insert into networkModule (or first module {}) │
│         + add import: com.mobilebytelabs.remoteconfig.remoteConfig │
├─────────────────────────────────────────────────────────────┤
│  GATE 3b: RemoteConfigHost                                  │
│  Check: RemoteConfigHost() in root composable               │
│  Fix:   Auto-insert in App.kt / root Box                    │
└───────────────────────────┬─────────────────────────────────┘
                            │ ALL PASS
                            ▼
                ✅ SYNC COMPLETE
```

> **No Gate 0 prerequisite.** `cmp-remote-config` 4.0.0+ is standalone — there is no
> dependency on `cmp-product-tickets` or any other module.

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-remote-config — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle    ✅  kmptoolkit-remote-config:4.0.0           ║
║  GATE 2  Supabase  ⚡  1 column added (content_json)            ║
║            Tables  ✅  product_remote_config, device_impressions║
║            RPCs    ✅  3 RPCs present                           ║
║  GATE 3a DSL       ⚡  Added remoteConfig {…} to networkModule  ║
║  GATE 3b UI Host   ✅  RemoteConfigHost() found in App.kt       ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: 25 columns — fully in sync                             ║
║  Docs: docs/remote-config/SETUP.md                              ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## Gate Reference

| Gate | Check | Auto-Fix |
|------|-------|----------|
| 1 | `kmptoolkit-remote-config = "4.0.0"` in versions | Insert / update |
| 1 | `implementation(libs.kmptoolkit.remote.config)` | Append |
| 2 | `product_remote_config` table live schema | `ALTER TABLE ADD COLUMN IF NOT EXISTS` |
| 2 | `device_impressions` table exists | `CREATE TABLE IF NOT EXISTS` |
| 2 | 3 RPCs exist | `CREATE OR REPLACE FUNCTION` |
| 3a | `remoteConfig {` block in any `module { }` block | Insert into networkModule + import |
| 3b | `RemoteConfigHost(` in root composable | Insert in root Box + import |

---

## Team Scenarios

### First time (new team member)

```bash
/sync-remote-config
# Gradle inserted ⚡
# Schema created ⚡ (25 columns + device_impressions + 3 RPCs)
# remoteConfig {…} DSL added to networkModule ⚡
# RemoteConfigHost inserted ⚡
```

### Colleague adds new column to product_remote_config

```bash
/sync-remote-config --migrate-only
# Fetches live schema — detects new column
# ALTER TABLE ADD COLUMN IF NOT EXISTS ...
# Preserves all team-added columns
```

### Migrating from 3.x

If the project still uses `RemoteConfigConfig`, `remoteConfigModule`, or the old
`ProductTicketsConfig`-based init, the wiring gate will detect the legacy pattern
and offer to rewrite it as `remoteConfig { … }` inside `networkModule`. The
`productType` parameter is dropped (per-project Supabase model — see
[README.md#migrating-from-3x](README.md#migrating-from-3x)).

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-remote-config.md
```

Updating the library? Edit that file. `/lib-sync cmp-remote-config` picks it up automatically.
