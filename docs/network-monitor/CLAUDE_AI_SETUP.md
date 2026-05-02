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
+-------------------------------------------------------------+
|  GATE 1b: Compose Module (optional)                          |
|  If app uses Compose Multiplatform:                          |
|  Check: cmp-network-monitor-compose dependency               |
|  Fix:   Auto-insert if detected                              |
|  Result: PASS / FIXED / SKIPPED                              |
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
|  GATE 1   Gradle     [OK]  cmp-network-monitor:1.0.0             |
|  GATE 1b  Compose    [OK]  cmp-network-monitor-compose (optional) |
|  GATE 2   Supabase   N/A   no backend                            |
|  GATE 3   Wiring     N/A   zero-config module                    |
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

### Scenario D — Add Compose module

```bash
/sync-network-monitor
# Gate 1b detects Compose Multiplatform in the project
# Auto-adds cmp-network-monitor-compose dependency
# Provides NetworkAwareContent, ConnectivityBanner, LocalNetworkMonitor
```

---

## Library API Quick Reference

### Core
| API | Description |
|-----|-------------|
| `createNetworkMonitor(config?)` | Create a platform-specific monitor |
| `provideNetworkMonitor(config?)` | DI factory function (Koin/Hilt/manual) |
| `NetworkMonitorProvider.install()` | Singleton lifecycle manager |
| `monitor1 + monitor2` | Combine monitors (online if ANY is online) |

### State
| API | Description |
|-----|-------------|
| `isOnline: StateFlow<Boolean>` | Hot-shared connectivity state |
| `networkStatus: StateFlow<NetworkStatus>` | Rich status (Available/Unavailable/CaptivePortal) |
| `networkChanges: SharedFlow<NetworkChangeEvent>` | Discrete transition events |
| `networkQuality(): Flow<NetworkQuality>` | Quality signal (Excellent/Good/Fair/Poor/Offline) |

### Extensions
| API | Description |
|-----|-------------|
| `requireOnline()` | Throws if offline |
| `ensureOnline()` | Suspends until online |
| `ifOnline { }` / `ifOffline { }` | Conditional execution |
| `retryOnReconnect(max) { }` | Retry on network reconnect |
| `measureLatency()` | HTTP round-trip timing |
| `addCallback(scope, onOnline, onOffline)` | Callback-style with handle |
| `withNetworkState(monitor)` | Combine data Flow + network state |
| `isOnlineDebounced(300L)` | Debounced (filters handoff flicker) |

### Testing
| API | Description |
|-----|-------------|
| `FakeNetworkMonitor()` | Test double with state/event history |
| `simulateHandoff(from, to)` | Simulate WiFi↔Cellular switch |
| `simulateFlicker(info)` | Simulate brief offline→online |
| `statusHistory` / `eventHistory` | Assert on recorded states |

### Compose (`cmp-network-monitor-compose`)
| API | Description |
|-----|-------------|
| `NetworkAwareContent(monitor)` | Auto online/offline/captive UI switching |
| `ConnectivityBanner(monitor)` | Shows banner when offline |
| `rememberNetworkMonitor()` | Remember + auto-cleanup |
| `LocalNetworkMonitor` | CompositionLocal provider |
