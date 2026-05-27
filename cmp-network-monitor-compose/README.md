# cmp-network-monitor-compose

Compose Multiplatform extensions for [`cmp-network-monitor`](../cmp-network-monitor/) — drop-in
banners, `@Composable` collectors, `CompositionLocal` injection, and subtree scoping.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-network-monitor-compose)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-network-monitor-compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> Core library docs (platform support, `NetworkStatus` model, validation strategies, captive
> portal, quality, bandwidth, extensions): see [`docs/network-monitor/README.md`](../docs/network-monitor/README.md).

## Platform Support

| Platform | Supported |
|----------|-----------|
| Android, iOS (x64/arm64/simulator), macOS (x64/arm64), JVM, JS, WasmJS | Yes |
| tvOS, watchOS, Linux, Windows, WasmWASI | No (Compose Multiplatform limitation) |

## Installation

```toml
# libs.versions.toml
[versions]
cmp-network-monitor         = "3.3.0"

[libraries]
cmp-network-monitor         = { module = "io.github.mobilebytelabs:cmp-network-monitor",         version.ref = "cmp-network-monitor" }
cmp-network-monitor-compose = { module = "io.github.mobilebytelabs:cmp-network-monitor-compose", version.ref = "cmp-network-monitor" }
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.cmp.network.monitor)
    implementation(libs.cmp.network.monitor.compose)
}
```

## API Surface (Compose-only)

| API | Purpose |
|-----|---------|
| `NetworkAwareContent(monitor, offlineContent, captivePortalContent, debounceMs, onlineContent)` | Auto switching between online / offline / captive portal subtrees. |
| `ConnectivityBanner(modifier, monitor, message, backgroundColor, contentColor, debounceMs)` | Animated slide-in offline banner (Material 3). |
| `rememberNetworkMonitor(config)` | Process-wide singleton; reset-aware via `NetworkMonitorProvider.version`. |
| `rememberScopedNetworkMonitor(config)` | Per-composition lifecycle; auto-closes on dispose. |
| `LocalNetworkMonitor` | `CompositionLocal<NetworkMonitor>`; throws if accessed without a provider. |
| `ProvideNetworkMonitor(monitor, content)` | Wrapper for `CompositionLocalProvider(LocalNetworkMonitor provides monitor)` — also populates the internal nullable mirror used by `rememberOrLocalNetworkMonitor`. |
| `rememberOrLocalNetworkMonitor(config)` | Returns the `ProvideNetworkMonitor`-injected monitor if present; falls back to `rememberNetworkMonitor(config)`. |
| `NetworkMonitor.collectIsOnlineAsState(debounceMs)` | `State<Boolean>`. |
| `NetworkMonitor.collectNetworkStatusAsState(debounceMs)` | `State<NetworkStatus>`. |
| `NetworkMonitor.collectNetworkQualityAsState(debounceMs)` | `State<NetworkQuality>` (debounce applied to upstream status before mapping). |

## Quick Start

```kotlin
// Singleton — most apps
@Composable
fun App() {
    val isOnline by rememberNetworkMonitor().collectIsOnlineAsState()
    if (!isOnline) OfflineHint()
}

// Drop-in banner
@Composable
fun Screen() {
    Column {
        ConnectivityBanner()                 // raw — flicker-honest
        ConnectivityBanner(debounceMs = 500L) // suppresses WiFi <-> Cellular handoff
        ScreenBody()
    }
}

// State-driven content switching
@Composable
fun DataScreen() {
    NetworkAwareContent(
        debounceMs = 300L,
        offlineContent = { OfflineEmptyState() },
        captivePortalContent = { portal -> CaptivePortalPrompt(portal.redirectUrl) },
        onlineContent = { available -> DataList(connectionType = available.info.type) },
    )
}
```

## What's new in v3.3.0

### Debounce on every collect / banner / content helper (M-005)

All five Compose collectors and both wrapper composables gained an optional
`debounceMs: Long = 0L` parameter. **Default `0L` is identical to the pre-3.3.0 behaviour** —
no migration required. Pass a positive value to suppress WiFi <-> Cellular handoff flicker:

```kotlin
val online  by monitor.collectIsOnlineAsState(debounceMs = 300L)
val status  by monitor.collectNetworkStatusAsState(debounceMs = 300L)
val quality by monitor.collectNetworkQualityAsState(debounceMs = 300L)

ConnectivityBanner(debounceMs = 500L)
NetworkAwareContent(debounceMs = 500L) { /* onlineContent */ }
```

Internally these route through `isOnlineDebouncedState` / `networkStatusDebouncedState`
(see core module) when `debounceMs > 0L`; the raw `collectAsState()` path is used when
`debounceMs <= 0L`. `collectNetworkQualityAsState(debounceMs > 0L)` debounces the upstream
`NetworkStatus` BEFORE mapping to quality, so quality transitions reflect the settled connection.

### Reset-aware `rememberNetworkMonitor()` (M-004)

`rememberNetworkMonitor()` now keys `remember` on `NetworkMonitorProvider.version`. If your app
calls `NetworkMonitorProvider.reset()` (logout, account switch, test cleanup), downstream
composables correctly recompose and re-acquire the **fresh** instance instead of holding the
closed reference.

```kotlin
// Pre-3.3.0: this remembered a closed NetworkMonitor after reset(); collect() emitted nothing.
// 3.3.0+:   recomposes on version bump and re-installs.
val monitor = rememberNetworkMonitor()
```

`reset()` while no instance is installed is a **no-op** — `version` does not advance, so reactive
consumers don't see a spurious recomposition.

> `NetworkMonitorProvider` is **NOT** thread-safe. Serialize `install()` / `reset()` from a single
> thread (app startup / shutdown or test teardown). Reads via `get()` / `getOrNull()` /
> `version` / `currentStatus` are safe from any thread.

### `ProvideNetworkMonitor` + `rememberOrLocalNetworkMonitor` (M-007 residual)

`LocalNetworkMonitor` already existed; 3.3.0 adds two ergonomic helpers around it for subtree
scoping (per-screen isolation, multi-window, integration tests with a `FakeNetworkMonitor`).

```kotlin
// Wrap a subtree with a custom monitor
val custom = remember { createNetworkMonitor(myConfig) }
ProvideNetworkMonitor(custom) {
    MyScreenSubtree()
}

// Reusable consumer composable — accepts BOTH provider-injected and singleton-fallback callers
@Composable
fun OfflineHint(
    monitor: NetworkMonitor = rememberOrLocalNetworkMonitor(),
) {
    val online by monitor.collectIsOnlineAsState()
    if (!online) Text("Offline")
}
```

Direct `CompositionLocalProvider(LocalNetworkMonitor provides custom) { ... }` still works, but
`rememberOrLocalNetworkMonitor()` reads an internal nullable mirror populated by
`ProvideNetworkMonitor` — prefer `ProvideNetworkMonitor` to get the singleton-fallback wiring for
free.

## Testing

Use a `FakeNetworkMonitor` from the core artifact and inject it via `ProvideNetworkMonitor`:

```kotlin
@Test
fun bannerShowsWhenOffline() = runComposeUiTest {
    val fake = FakeNetworkMonitor(initialOnline = true)
    setContent {
        ProvideNetworkMonitor(fake) {
            Column {
                ConnectivityBanner()
                TestTagText("body")
            }
        }
    }

    onNodeWithText("No internet connection").assertDoesNotExist()
    fake.setOnline(false)
    onNodeWithText("No internet connection").assertIsDisplayed()
}
```

For unit tests of state-collecting composables, prefer `rememberOrLocalNetworkMonitor()` as the
parameter default so the test can inject the fake without touching the global singleton.

## Cross-references

- Core API: [`docs/network-monitor/README.md`](../docs/network-monitor/README.md)
- Manual setup: [`docs/network-monitor/SETUP.md`](../docs/network-monitor/SETUP.md)
- AI-assisted setup: [`docs/network-monitor/CLAUDE_AI_SETUP.md`](../docs/network-monitor/CLAUDE_AI_SETUP.md)
- 3.2.x → 3.3.0 migration: [`cmp-network-monitor/MIGRATION.md`](../cmp-network-monitor/MIGRATION.md)
- Per-platform test matrix: [`cmp-network-monitor/TESTING.md`](../cmp-network-monitor/TESTING.md)
- Changelog: [`cmp-network-monitor/CHANGELOG.md`](../cmp-network-monitor/CHANGELOG.md) ·
  [`cmp-network-monitor-compose/CHANGELOG.md`](CHANGELOG.md)

## License

```
Copyright 2026 MobileByteLabs

Licensed under the Apache License, Version 2.0
```
