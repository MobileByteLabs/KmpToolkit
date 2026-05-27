# cmp-network-monitor

Reactive network connectivity monitoring for **Kotlin Multiplatform** — StateFlow-based, all 21 KMP targets.

## What's new in v3.3.0

Source-compatible release — no required code changes. Highlights:

- **HIGH-severity teardown / cold-start race fixes** (M-001 JS/WasmJs `close()` ordering; M-002 Android `NetworkCallback` events dropped during cold-start seed).
- **Debounced StateFlow helpers** — `isOnlineDebouncedState(scope, timeoutMs)` and `networkStatusDebouncedState(scope, timeoutMs)` for late-subscriber UIs (cold-Flow `isOnlineDebounced` / `debouncedNetworkStatus` retained, unchanged).
- **Compose `debounceMs` opt-in** — `ConnectivityBanner`, `NetworkAwareContent`, `collectIsOnlineAsState`, `collectNetworkStatusAsState`, `collectNetworkQualityAsState` now accept `debounceMs: Long = 0L` (default `0L` preserves behaviour).
- **Reset-aware `rememberNetworkMonitor()`** — keys `remember` on `NetworkMonitorProvider.version` so composables drop stale closed references after `NetworkMonitorProvider.reset()`.
- **`ProvideNetworkMonitor` + `rememberOrLocalNetworkMonitor`** — subtree-scoped monitor injection with singleton fallback.
- **First per-platform test sourcesets** — `jvmTest`, `appleTest`, `jsTest`, `wasmJsTest` (M-006); plus four `commonTest` files covering M-002/M-003/M-004 patterns.

See:
- [`cmp-network-monitor/MIGRATION.md`](../../cmp-network-monitor/MIGRATION.md) — 3.2.x → 3.3.0 migration guide (before/after snippets, opt-in checklist)
- [`cmp-network-monitor/CHANGELOG.md`](../../cmp-network-monitor/CHANGELOG.md) — full per-change notes
- [`cmp-network-monitor-compose/README.md`](../../cmp-network-monitor-compose/README.md) — Compose-specific API surface
- [`cmp-network-monitor/TESTING.md`](../../cmp-network-monitor/TESTING.md) — per-platform test matrix

## Documentation

| Document | Description |
|----------|-------------|
| [SETUP.md](SETUP.md) | Manual integration guide with all features |
| [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) | AI-assisted setup via `/sync-network-monitor` |
| [README.md (module)](../../cmp-network-monitor/README.md) | Full module README with badges |
| [CHANGELOG.md](../../cmp-network-monitor/CHANGELOG.md) | Version history |

## Quick Start

```kotlin
// 1. Add dependency
commonMain.dependencies {
    implementation(libs.cmp.network.monitor)
}

// 2. Create monitor
val monitor = createNetworkMonitor()

// 3. Observe
monitor.isOnline.collect { online -> updateUI(online) }

// 4. Use extensions
monitor.ifOnline { api.fetchData() }
monitor.ifOffline { showCachedData() }
```

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `cmp-network-monitor` | `io.github.mobilebytelabs:cmp-network-monitor` | Core library (zero dependencies beyond coroutines) |
| `cmp-network-monitor-compose` | `io.github.mobilebytelabs:cmp-network-monitor-compose` | Compose Multiplatform extensions |

## Platform Support

| Platform | API | Type |
|----------|-----|------|
| Android | ConnectivityManager + NetworkCallback | Push-based |
| iOS/macOS/tvOS/watchOS | NWPathMonitor | Push-based |
| JVM | NetworkInterface + HTTP HEAD + Adaptive Polling | Poll + validate |
| JS | navigator.onLine + events | Push-based |
| WasmJS | navigator.onLine + events | Push-based |
| Linux | /sys/class/net + Adaptive Polling | Poll-based |
| Windows (MinGW) | Winsock2 + Adaptive Polling | Poll-based |
| WasmWASI | Stub (no network API) | Stub |
