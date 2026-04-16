# cmp-remote-config — Claude AI Setup Guide

Use `/sync-remote-config` (or `/lib-sync cmp-remote-config`) to automatically verify and wire
`cmp-remote-config` into your KMP project — including Supabase schema delta.

---

## Quick Commands

```bash
/sync-remote-config                 # Full sync (all 4 gates)
/sync-remote-config --check         # Dry run — show status, no writes
/sync-remote-config --migrate-only  # Gate 2 only (Supabase schema delta)
/sync-remote-config --wiring-only   # Gates 3a+3b only (Koin + RemoteConfigHost)
/lib-sync cmp-remote-config         # Same as /sync-remote-config
```

---

## What the Skill Does

```
/sync-remote-config
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 0: Prerequisite Check                                 │
│  Check: FeatureRequestConfig.init() present in project      │
│  Fail:  Block with "Configure cmp-user-tickets first"       │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                  │
│  Check: kmptoolkit-remote-config:2.1.0 in libs.versions     │
│  Fix:   Auto-insert                                         │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Delta                              │
│  Fetch: Live schema for product_remote_config               │
│  Delta: ADD missing columns (26 cols) — NEVER DROP          │
│  Also:  Check device_impressions table + 3 RPCs             │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 3a: Koin Module                                       │
│  Check: remoteConfigModule in startKoin { modules(...) }    │
│  Fix:   Auto-append + add import                            │
├─────────────────────────────────────────────────────────────┤
│  GATE 3b: RemoteConfigHost                                  │
│  Check: RemoteConfigHost() in root composable               │
│  Fix:   Auto-insert in App.kt / root Box                    │
└───────────────────────────┬─────────────────────────────────┘
                            │ ALL PASS
                            ▼
                ✅ SYNC COMPLETE
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-remote-config — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 0  Prereq    ✅  FeatureRequestConfig.init() found        ║
║  GATE 1  Gradle    ✅  kmptoolkit-remote-config:2.1.0           ║
║  GATE 2  Supabase  ⚡  1 column added (content_json)            ║
║            Tables  ✅  product_remote_config, device_impressions║
║            RPCs    ✅  3 RPCs present                           ║
║  GATE 3a Koin      ⚡  Added remoteConfigModule                 ║
║  GATE 3b UI Host   ✅  RemoteConfigHost() found in App.kt       ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: 26 columns — fully in sync                             ║
║  Docs: docs/remote-config/SETUP.md                             ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## Gate Reference

| Gate | Check | Auto-Fix |
|------|-------|----------|
| 0 | `FeatureRequestConfig.init()` in **/*.kt | Block — must fix manually |
| 1 | `kmptoolkit-remote-config = "2.1.0"` in versions | Insert / update |
| 1 | `implementation(libs.kmptoolkit.remote.config)` | Append |
| 2 | `product_remote_config` table live schema | `ALTER TABLE ADD COLUMN IF NOT EXISTS` |
| 2 | `device_impressions` table exists | `CREATE TABLE IF NOT EXISTS` |
| 2 | 3 RPCs exist | `CREATE OR REPLACE FUNCTION` |
| 3a | `remoteConfigModule` in `startKoin { modules }` | Append + import |
| 3b | `RemoteConfigHost(` in root composable | Insert in root Box + import |

---

## Team Scenarios

### First time (new team member)

```bash
/sync-remote-config
# Prerequisite check ✅
# Gradle inserted ⚡
# Schema created ⚡ (26 columns + device_impressions + 3 RPCs)
# Koin module added ⚡
# RemoteConfigHost inserted ⚡
```

### Colleague adds new column to product_remote_config

```bash
/sync-remote-config --migrate-only
# Fetches live schema — detects new column
# ALTER TABLE ADD COLUMN IF NOT EXISTS ...
# Preserves all team-added columns
```

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-remote-config.md
```

Updating the library? Edit that file. `/lib-sync cmp-remote-config` picks it up automatically.
