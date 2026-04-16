# cmp-in-app-update — Claude AI Setup Guide

Use `/sync-in-app-update` (or `/lib-sync cmp-in-app-update`) to automatically verify and wire
`cmp-in-app-update` into your KMP project.

---

## Quick Commands

```bash
/sync-in-app-update                 # Full verify-gated sync
/sync-in-app-update --check         # Dry run — show status, no writes
/sync-in-app-update --wiring-only   # Gate 3 only (config check)
/lib-sync cmp-in-app-update         # Same as /sync-in-app-update
```

---

## What the Skill Does

```
/sync-in-app-update
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                  │
│  Check: kmp-in-app-update:2.1.0 in libs.versions.toml      │
│  Fix:   Auto-insert                                         │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema (OPTIONAL)                         │
│  Only runs if app uses .supabase() resolver                 │
│  Check: app_versions table exists                           │
│  Delta: ONLY ADD missing columns — NEVER DROP               │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS / SKIP
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GATE 3: AppUpdateConfig                                    │
│  Check: AppUpdateConfig.builder() present in project        │
│  Fix:   Auto-insert builder stub if missing                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ PASS
                            ▼
                ✅ SYNC COMPLETE
```

---

## State Summary Output

```
╔════════════════════════════════════════════════════════════════╗
║  /sync-in-app-update — COMPLETE                                ║
╠════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle    ✅  kmp-in-app-update:2.1.0                ║
║  GATE 2  Supabase  N/A  no Supabase resolver detected          ║
║  GATE 3  Config    ⚡  Added AppUpdateConfig.builder() stub    ║
╠════════════════════════════════════════════════════════════════╣
║  Docs: docs/in-app-update/SETUP.md                            ║
╚════════════════════════════════════════════════════════════════╝
```

---

## Gate Reference

| Gate | Check | Auto-Fix |
|------|-------|----------|
| 1 | `kmp-in-app-update = "2.1.0"` in versions | Insert / update |
| 1 | `implementation(libs.kmp.in.app.update)` in commonMain | Append |
| 2 | `app_versions` table exists (Supabase resolver only) | `CREATE TABLE IF NOT EXISTS` |
| 3 | `AppUpdateConfig.builder()` call present | Insert builder stub |

---

## Single Source of Truth

```
source/kmp-toolkit/.claude-runtime/commands/sync-in-app-update.md
```
