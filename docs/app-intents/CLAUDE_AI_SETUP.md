# cmp-app-intents — Claude AI Setup Guide

Use `/sync-app-intents` (or `/lib-sync cmp-app-intents`) to automatically verify that
`cmp-app-intents` is correctly integrated into your KMP project.

---

## Quick Commands

```bash
/sync-app-intents           # Full sync (Gate 1 Gradle only)
/sync-app-intents --check   # Dry run — show status, no writes
/lib-sync cmp-app-intents   # Same as /sync-app-intents (framework alias)
```

---

## What the Skill Does

`cmp-app-intents` is zero-configuration at the Gradle level — sync is a single gate:

```
/sync-app-intents
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: cmp-app-intents:3.2.11 in libs.versions.toml    │
│  Check: used in commonMain.dependencies                 │
│  Fix:   Auto-insert correct entries                     │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
              ✅ SYNC COMPLETE — state summary
```

**Gate 2 (Supabase)** and **Gate 3 (Wiring)** are skipped — no backend, no DI, no nav.
iOS Swift bridge installation and Android App Actions XML are developer responsibilities
documented in [SETUP.md](SETUP.md) Steps 3–4 — not auto-verified.

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════╗
║  /sync-app-intents — COMPLETE                                ║
╠══════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  cmp-app-intents:3.2.11                ║
║  GATE 2  Supabase N/A  no backend                           ║
║  GATE 3  Wiring   N/A  platform bridge (see SETUP.md)       ║
╠══════════════════════════════════════════════════════════════╣
║  Docs: docs/app-intents/SETUP.md                            ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-app-intents
# → Inserts cmp-app-intents:3.2.11 in libs.versions.toml
# → Adds implementation(libs.cmp.app.intents) to shared/build.gradle.kts
```

### Scenario B — Version bump

```bash
# Library team updates version in sync-app-intents.md
/sync-app-intents
# → Detects stale version, updates to new version
```

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-app-intents.md
```

Framework `/lib-sync` reads this file on every run. No framework file changes needed when
the library version changes.
