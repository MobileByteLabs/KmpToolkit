# cmp-bubble — Claude AI Setup Guide

Use `/sync-bubble` (or `/lib-sync cmp-bubble`) to automatically verify and wire `cmp-bubble`
into your KMP project in seconds.

---

## Quick Commands

```bash
/sync-bubble                  # Full verify-gated sync
/sync-bubble --check          # Dry run — show status, no writes
/sync-bubble --wiring-only    # Gate 3 only (BubbleConfig wiring)
/lib-sync cmp-bubble          # Same as /sync-bubble (framework alias)
```

---

## What the Skill Does

```
/sync-bubble
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                              │
│  Check: kmp-bubble:2.1.0 in libs.versions.toml          │
│  Check: used in commonMain.dependencies                 │
│  Fix:   Auto-insert correct entries                     │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
┌─────────────────────────────────────────────────────────┐
│  GATE 3: Wiring Check                                   │
│  Check: createBubble() call present in project          │
│  Fix:   Auto-insert usage example if missing            │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                │
└──────────────────────────┬──────────────────────────────┘
                           │ PASS
                           ▼
              ✅ SYNC COMPLETE — state summary
```

**Gate 2 (Supabase schema) is skipped — cmp-bubble has no backend.**

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════╗
║  /sync-bubble — COMPLETE                                 ║
╠══════════════════════════════════════════════════════════╣
║  GATE 1  Gradle   ✅  kmp-bubble:2.1.0                  ║
║  GATE 2  Supabase N/A  no backend                       ║
║  GATE 3  Wiring   ⚡  Added createBubble() example      ║
╠══════════════════════════════════════════════════════════╣
║  Docs: docs/bubble/SETUP.md                             ║
╚══════════════════════════════════════════════════════════╝
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-bubble
# → Inserts kmp-bubble:2.1.0 in libs.versions.toml
# → Adds implementation(libs.kmp.bubble) to shared/build.gradle.kts
# → Inserts createBubble() usage example
```

### Scenario B — Check only (no writes)

```bash
/sync-bubble --check
# Shows what WOULD change without modifying any files
```

### Scenario C — Version bump

```bash
# 1. Library team bumps version in sync-bubble.md
# 2. Run:
/sync-bubble
# → Detects stale version, updates libs.versions.toml to match
```

---

## Gate Reference

| Gate | Check | Auto-Fix |
|------|-------|----------|
| 1 | `kmp-bubble = "2.1.0"` in libs.versions.toml | Insert / update entry |
| 1 | `implementation(libs.kmp.bubble)` in commonMain | Append to dependencies |
| 2 | — N/A — | — |
| 3 | `createBubble()` used in project | Insert example in ViewModel or App |

---

## Single Source of Truth

The skill contract lives at:
```
source/kmp-toolkit/.claude-runtime/commands/sync-bubble.md
```

When the library team updates version or API, they update that file.
`/lib-sync cmp-bubble` picks it up automatically on next run — no framework file changes needed.
