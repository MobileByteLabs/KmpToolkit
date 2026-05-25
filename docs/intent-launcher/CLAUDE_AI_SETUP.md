# cmp-intent-launcher — Claude AI Setup Guide

Use `/sync-intent-launcher` (or `/lib-sync cmp-intent-launcher`) to automatically verify that
`cmp-intent-launcher` is correctly integrated into your KMP project.

---

## Quick Commands

```bash
/sync-intent-launcher           # Full sync (Gate 1 Gradle only)
/sync-intent-launcher --check   # Dry run — show status, no writes
/lib-sync cmp-intent-launcher   # Same as /sync-intent-launcher (framework alias)
```

---

## What the Skill Does

`cmp-intent-launcher` is zero-configuration at the Gradle level — sync is a single gate:

```
/sync-intent-launcher
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: cmp-intent-launcher:3.2.11 in libs.versions.toml│
│  Check: used in commonMain.dependencies                 │
│  Fix:   Auto-insert correct entries                     │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
              ✅ SYNC COMPLETE — state summary
```

**Gate 2 (Supabase)** and **Gate 3 (Wiring)** are skipped — no backend, no DI, no nav.
Android Activity wiring (`rememberIntentLauncher()` at composition root) is a developer
responsibility documented in [SETUP.md](SETUP.md) Step 4 — not auto-verified.

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════╗
║  /sync-intent-launcher — COMPLETE                            ║
╠══════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  cmp-intent-launcher:3.2.11            ║
║  GATE 2  Supabase N/A  no backend                           ║
║  GATE 3  Wiring   N/A  Compose-only wiring (see SETUP.md)   ║
╠══════════════════════════════════════════════════════════════╣
║  Docs: docs/intent-launcher/SETUP.md                        ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-intent-launcher
# → Inserts cmp-intent-launcher:3.2.11 in libs.versions.toml
# → Adds implementation(libs.cmp.intent.launcher) to shared/build.gradle.kts
```

### Scenario B — Version bump

```bash
# Library team updates version in sync-intent-launcher.md
/sync-intent-launcher
# → Detects stale version, updates to new version
```

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-intent-launcher.md
```

Framework `/lib-sync` reads this file on every run. No framework file changes needed when
the library version changes.
