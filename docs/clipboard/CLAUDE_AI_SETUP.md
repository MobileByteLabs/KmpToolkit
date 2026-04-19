# cmp-clipboard — Claude AI Setup Guide

Use `/sync-clipboard` (or `/lib-sync cmp-clipboard`) to automatically verify that
`cmp-clipboard` is correctly integrated into your KMP project.

---

## Quick Commands

```bash
/sync-clipboard           # Full sync (Gate 1 Gradle only)
/sync-clipboard --check   # Dry run — show status, no writes
/lib-sync cmp-clipboard   # Same as /sync-clipboard (framework alias)
```

---

## What the Skill Does

`cmp-clipboard` is zero-configuration — sync is a single gate:

```
/sync-clipboard
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: kmp-clipboard:2.1.0 in libs.versions.toml       │
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
║  /sync-clipboard — COMPLETE                              ║
╠══════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  kmp-clipboard:2.1.0               ║
║  GATE 2  Supabase N/A  no backend                       ║
║  GATE 3  Wiring   N/A  zero-config module               ║
╠══════════════════════════════════════════════════════════╣
║  Docs: docs/clipboard/SETUP.md                          ║
╚══════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-clipboard
# → Inserts kmp-clipboard:2.1.0 in libs.versions.toml
# → Adds implementation(libs.kmp.clipboard) to shared/build.gradle.kts
```

### Scenario B — Version bump

```bash
# Library team updates version in sync-clipboard.md
/sync-clipboard
# → Detects stale version, updates to new version
```

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-clipboard.md
```

Framework `/lib-sync` reads this file on every run. No framework file changes needed when
the library version changes.
