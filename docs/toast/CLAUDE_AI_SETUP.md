# cmp-toast — Claude AI Setup Guide

Use `/sync-toast` (or `/lib-sync cmp-toast`) to automatically verify and wire `cmp-toast`
into your KMP project.

---

## Quick Commands

```bash
/sync-toast           # Full sync
/sync-toast --check   # Dry run — show status, no writes
/lib-sync cmp-toast   # Same as /sync-toast
```

---

## What the Skill Does

```
/sync-toast
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: kmp-toast:2.1.0 in libs.versions.toml           │
│  Check: used in commonMain.dependencies                 │
│  Fix:   Auto-insert correct entries                     │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 3: ToastHost Placement                            │
│  Check: ToastHost( present in project **/*.kt           │
│  Fix:   Auto-insert in root App composable              │
│  Result: ✅ PASS / ⚡ FIXED                             │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
              ✅ SYNC COMPLETE
```

**Gate 2 (Supabase) is skipped — cmp-toast has no backend.**

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════╗
║  /sync-toast — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  kmp-toast:2.1.0                   ║
║  GATE 2  Supabase N/A  no backend                       ║
║  GATE 3  Wiring   ⚡  Added ToastHost() in App.kt       ║
╠══════════════════════════════════════════════════════════╣
║  Docs: docs/toast/SETUP.md                              ║
╚══════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### First time

```bash
/sync-toast
# → kmp-toast:2.1.0 inserted in libs.versions.toml
# → implementation(libs.kmp.toast) added to shared/build.gradle.kts
# → ToastHost(hostState = toastState) inserted in root App.kt
```

### Check only

```bash
/sync-toast --check
# Shows what WOULD change without writing any files
```

---

## Gate Reference

| Gate | Check | Auto-Fix |
|------|-------|----------|
| 1 | `kmp-toast = "2.1.0"` in versions | Insert / update |
| 1 | `implementation(libs.kmp.toast)` in commonMain | Append |
| 3 | `ToastHost(` in **/*.kt | Insert in root composable |

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-toast.md
```
