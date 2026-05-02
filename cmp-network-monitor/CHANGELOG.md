# Changelog

All notable changes to `cmp-network-monitor` will be documented in this file.

## [Unreleased]

### Added
- **Captive Portal Detection**: `NetworkStatus.CaptivePortal` status, `CaptivePortalDetected`/`CaptivePortalResolved` events, `captivePortalDetection` config flag. Platform-specific detection via HTTP redirect (302/307) on JVM, Android, Apple, JS.
- **Adaptive Polling**: Polling-based monitors (JVM, Linux, MinGW) now grow poll intervals during stable periods and reset on network changes. Reduces CPU/battery usage by up to 80% during idle periods.
- **Validation Backoff**: Exponential backoff on HTTP validation failures prevents hammering unreachable endpoints.
- **Bandwidth Sampling**: `bandwidthSamples()` Flow, `currentBandwidth` property, `hasSufficientBandwidth(minKbps)` check for bandwidth-aware features.
- **Composite Monitor**: Combine multiple `NetworkMonitor` instances via `monitor1 + monitor2`. Reports online if ANY constituent is online, picks best-quality status.
- **Cold-Start Disk Cache**: Last-known network state persisted to disk (SharedPreferences on Android, UserDefaults on Apple, file on JVM/Linux/MinGW). Provides instant state on app cold-start.
- **Lifecycle Observer (Android)**: `LifecycleNetworkObserver` for Activity/Fragment scoped observation without manual lifecycle management.
- **DI Module**: `provideNetworkMonitor()` factory function with documented patterns for Koin, Hilt, and manual DI.
- **Singleton Provider Enhancements**: `NetworkMonitorProvider.redundantInstallCount` tracks multiple install calls.
- **Extension Functions**:
  - `ifOnline { }` / `ifOffline { }` — conditional execution based on connectivity
  - `measureLatency()` — HTTP round-trip timing
  - `addCallback(scope, onOnline, onOffline)` — callback-style API with `CallbackHandle`
  - `awaitConnectionType(type)` — suspend until specific `NetworkType`
  - `closeGracefully()` — suspend-safe close
  - `withNetworkState(monitor)` — combine data Flow with NetworkStatus
  - `onlyWhileOnline(monitor)` — filter Flow to online-only emission
- **Network Quality**: `NetworkQuality` enum (Excellent/Good/Fair/Poor/Offline) with `networkQuality()` Flow and `currentQuality` property.
- **Network Constraints**: `NetworkConstraint` predicate system with `allOf()` combinator.
- **Retry Policy**: Configurable retry with exponential backoff and jitter.
- **Memory Pressure (Android)**: `ComponentCallbacks2` integration suppresses bandwidth-only updates under memory pressure.
- **Testing Enhancements**: `FakeNetworkMonitor` now tracks `statusHistory`, `eventHistory`, `updateCount`, and supports `simulateHandoff()`, `simulateFlicker()`, `resetState()`.

### Changed
- `NetworkStatus` sealed class now has 3 variants: `Available`, `Unavailable`, `CaptivePortal`.
- Linux, MinGW, and JVM monitors now use adaptive polling instead of fixed intervals.

## [1.0.0] — 2026-05-01

### Added
- Initial release with 21 KMP target support.
- `NetworkMonitor` interface with `isOnline`, `networkStatus`, `networkChanges`.
- Platform implementations: Android (ConnectivityManager), Apple (NWPathMonitor), JVM (polling), JS (navigator.onLine), WasmJS, Linux (/sys/class/net), MinGW (Winsock), WasmWASI (stub).
- `NetworkMonitorConfig` with DSL builder, `ValidationStrategy` enum.
- `NetworkType` enum: WiFi, Cellular, FiveG, Ethernet, VPN, Bluetooth, Unknown.
- `NetworkInfo` data class with type, metered, bandwidth.
- `NetworkChangeEvent` sealed class for transition events.
- `createNetworkMonitor()` expect/actual factory.
- `FakeNetworkMonitor` for testing.
- `StubNetworkMonitor` for unsupported platforms.
- `NetworkMonitorProvider` singleton lifecycle manager.
- Binary compatibility validation via `kotlinx-binary-compatibility-validator`.
