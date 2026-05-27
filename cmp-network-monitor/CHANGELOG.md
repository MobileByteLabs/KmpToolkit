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

### Added (Phase 04 — per-platform test coverage)
- **M-006**: First platform-specific test sourcesets for `cmp-network-monitor`. `jvmTest` gains 3 JVM-Desktop tests (`JvmNetworkMonitorCloseTest`); `appleTest` gains 2 tests covering iOS/macOS/tvOS/watchOS (`AppleNetworkMonitorCloseTest`); `jsTest` + `wasmJsTest` host the Phase 01 race-regression tests verified live in `jsBrowserTest` / `wasmJsBrowserTest`. Build-side: `applyDefaultHierarchyTemplate()` auto-wires every platform test sourceset — no `build.gradle.kts` edits required. `TESTING.md` documents the per-platform run matrix + toolchain caveats (full Xcode.app required for Apple native test EXECUTION; sample-toolkit JS resolution unblocked).
- **Phase 04 T0**: `samples/sample-toolkit/composeApp` no longer declares `js { browser() }` / `wasmJs { browser() }`. The catalog consumed `:cmp-toast`, `:cmp-clipboard`, `:cmp-in-app-update`, `:cmp-bubble` and other modules without matching JS targets, and the root `:kotlinNpmInstall` aggregation propagated the failure into every `:cmp-*` module's JS test pipeline (`:cmp-network-monitor:jsNodeTest` could not even reach the "Running tests" phase). Removing the unbuildable JS / WasmJs targets matches the catalog's actual platform support (Android + iOS + Desktop).

### Added (Phase 03 — composition stability + ergonomics)
- **M-004**: `NetworkMonitorProvider.version: StateFlow<Int>` — generation counter that increments on each fresh `install()` and on `reset()` when the instance was non-null. Skips no-op cases (redundant installs, resets-on-empty-state) so reactive consumers don't see spurious recompositions. `rememberNetworkMonitor()` now keys `remember` on `version`, so composables drop their stale closed `NetworkMonitor` reference and re-install a fresh one after `NetworkMonitorProvider.reset()` elsewhere in the process. `NetworkMonitorProvider` thread-safety KDoc corrected: it is NOT thread-safe; callers must serialize `install`/`reset` at app-startup/shutdown.
- **M-007 (residual ergonomics)**: `LocalNetworkMonitor` was already present; added two Compose helpers around it: `ProvideNetworkMonitor(monitor, content)` provides a `NetworkMonitor` to the composition subtree (wraps `CompositionLocalProvider`), and `rememberOrLocalNetworkMonitor(config)` returns the subtree-provided monitor if any, otherwise falls back to the process singleton via `rememberNetworkMonitor(config)`.

### Added (Phase 02 — debounced StateFlow + Compose debounce config)
- **M-003**: New `NetworkMonitor.isOnlineDebouncedState(scope, timeoutMs = 300L)` and `NetworkMonitor.networkStatusDebouncedState(scope, timeoutMs = 300L)` extension functions returning hot `StateFlow<T>` with the monitor's current value as the seed. Late subscribers see the current value immediately, eliminating the cold-Flow "wait for next change" surprise of the pre-existing `isOnlineDebounced` / `debouncedNetworkStatus` cold-Flow helpers (which remain unchanged for source compatibility).
- **M-005**: `ConnectivityBanner`, `NetworkAwareContent`, and `NetworkMonitor.collectIsOnlineAsState` / `collectNetworkStatusAsState` / `collectNetworkQualityAsState` now accept an optional `debounceMs: Long = 0L` parameter. Default (`0L`) preserves the prior raw `collectAsState` semantics exactly; any positive value transparently routes through the new debounced StateFlow helpers. `collectNetworkQualityAsState(debounceMs > 0L)` debounces the upstream `NetworkStatus` BEFORE mapping to quality, so quality transitions reflect the settled connection rather than transient flicker.

### Fixed
- **M-001 (HIGH, JS)**: `JsNetworkMonitor.close()` now removes `online`/`offline` event listeners BEFORE cancelling the coroutine scope. The previous order created a race window where a network event firing between `scope.cancel()` and `removeEventListener` would invoke a handler against a cancelled scope, surfacing as console-visible `CancellationException` during rapid app teardown.
- **M-001 mirror (HIGH, WasmJs)**: `WasmJsNetworkMonitor.close()` applies the same ordering fix; both `online`/`offline` handlers also defensively check `closed && scope.isActive` before mutating state.
- **M-002 (HIGH, Android)**: `AndroidNetworkMonitor` no longer drops `NetworkCallback` events that fire during cold-start `seedInitialState()`. Replaced the `AtomicBoolean callbackReady` drop-gate with a `CompletableDeferred<Unit> seedComplete` queue-gate — callbacks now wrap their body in `scope.launch { seedComplete.await(); … }` so events queue until seed completes instead of being silently discarded. Seed runs inside a `try/finally` to guarantee the gate releases even if seeding throws.

## [3.2.1] — 2026-05-01

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
