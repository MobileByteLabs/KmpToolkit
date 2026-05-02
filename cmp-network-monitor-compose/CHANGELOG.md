# Changelog — cmp-network-monitor-compose

All notable changes to `cmp-network-monitor-compose` will be documented in this file.

## [Unreleased]

## [3.2.1] — 2026-05-02

### Added
- `NetworkAwareContent` — auto online/offline/captive portal UI switching
- `ConnectivityBanner` — animated slide-in/out offline banner with Material 3
- `rememberNetworkMonitor()` — singleton lifecycle via `NetworkMonitorProvider`
- `rememberScopedNetworkMonitor()` — per-composition lifecycle with auto-close
- `collectIsOnlineAsState()` — Boolean state from `NetworkMonitor.isOnline`
- `collectNetworkStatusAsState()` — `NetworkStatus` state from `NetworkMonitor.networkStatus`
- `collectNetworkQualityAsState()` — `NetworkQuality` state from `NetworkMonitor.networkQuality()`
- `LocalNetworkMonitor` — `CompositionLocal` for tree-wide provision

### Platform Support
- Android, iOS (x64/arm64/simulator), macOS (x64/arm64), JVM, JS, WasmJS
- tvOS, watchOS, Linux, Windows, WasmWASI not supported (Compose Multiplatform limitation)
