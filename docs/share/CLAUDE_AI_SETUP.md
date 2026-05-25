# cmp-share — Claude AI Setup Guide

Use `/sync-share` (or `/lib-sync cmp-share`) to automatically verify that
`cmp-share` is correctly integrated into your KMP project.

---

## Quick Commands

```bash
/sync-share           # Full sync (Gate 1 Gradle only)
/sync-share --check   # Dry run — show status, no writes
/lib-sync cmp-share   # Same as /sync-share (framework alias)
```

---

## What the Skill Does

`cmp-share` is zero-configuration — sync is a single gate:

```
/sync-share
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: cmp-share:3.2.11 in libs.versions.toml          │
│  Check: used in commonMain.dependencies                 │
│  Fix:   Auto-insert correct entries                     │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
              ✅ SYNC COMPLETE — state summary
```

**Gate 2 (Supabase)** and **Gate 3 (Wiring)** are skipped — zero-config, no DI, no nav.

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════╗
║  /sync-share — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  cmp-share:3.2.11                  ║
║  GATE 2  Supabase N/A  no backend                       ║
║  GATE 3  Wiring   N/A  zero-config module               ║
╠══════════════════════════════════════════════════════════╣
║  Docs: docs/share/SETUP.md                              ║
╚══════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-share
# → Inserts cmp-share:3.2.11 in libs.versions.toml
# → Adds implementation(libs.cmp.share) to shared/build.gradle.kts
```

### Scenario B — Version bump

```bash
# Library team updates version in sync-share.md
/sync-share
# → Detects stale version, updates to new version
```

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-share.md
```

Framework `/lib-sync` reads this file on every run. No framework file changes needed when
the library version changes.
