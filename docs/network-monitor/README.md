# cmp-network-monitor

Reactive network connectivity monitoring for **Kotlin Multiplatform** — StateFlow-based, all 21 KMP targets.

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
