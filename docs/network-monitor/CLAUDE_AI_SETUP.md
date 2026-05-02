# cmp-network-monitor — Claude AI Setup Guide

Use `/sync-network-monitor` (or `/lib-sync cmp-network-monitor`) to automatically verify that
`cmp-network-monitor` is correctly integrated into your KMP project.

---

## Quick Commands

```bash
/sync-network-monitor           # Full sync (Gate 1 Gradle only)
/sync-network-monitor --check   # Dry run — show status, no writes
/lib-sync cmp-network-monitor   # Same as /sync-network-monitor (framework alias)
```

---

## What the Skill Does

`cmp-network-monitor` is zero-configuration — sync is a single gate:

```
/sync-network-monitor
      |
      v
+-------------------------------------------------------------+
|  GATE 1: Gradle Dependency                                   |
|  Check: cmp-network-monitor:1.0.0 in libs.versions.toml     |
|  Check: used in commonMain.dependencies                      |
|  Fix:   Auto-insert correct entries                          |
|  Result: PASS / FIXED / BLOCKED                              |
+-----------------------------+--------------------------------+
                              | PASS
                              v
             SYNC COMPLETE — state summary
```

**Gate 2 (Supabase)** and **Gate 3 (Wiring)** are skipped — zero-config, no DI, no nav.

---

## State Summary Output

```
+==================================================================+
|  /sync-network-monitor — COMPLETE                                 |
+==================================================================+
|  GATE 1  Gradle   [OK]  cmp-network-monitor:1.0.0               |
|  GATE 2  Supabase N/A   no backend                               |
|  GATE 3  Wiring   N/A   zero-config module                       |
+------------------------------------------------------------------+
|  Docs: docs/network-monitor/SETUP.md                             |
+==================================================================+
```

---

## Team Scenarios

### Scenario A — First time setup

```bash
/sync-network-monitor
# Gate 1 adds dependency to libs.versions.toml and build.gradle.kts
# Done — start using createNetworkMonitor() in your code
```

### Scenario B — Version bump

```bash
/sync-network-monitor
# Gate 1 detects old version, updates to latest
```

### Scenario C — Just checking

```bash
/sync-network-monitor --check
# Shows current sync status without modifying files
```
