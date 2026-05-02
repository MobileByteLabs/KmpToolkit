# /sync-network-monitor - Full Instructions

> **Single source of truth** for `cmp-network-monitor` sync contract.
> The framework `/lib-sync cmp-network-monitor` delegates to this file.
> Update this file when the library version changes.

---

# /sync-network-monitor — cmp-network-monitor Sync

Verify-gated sync of `cmp-network-monitor` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A. Gate 3: N/A (zero-config module).

---

## Module Contract (update when library changes)

```yaml
module:    cmp-network-monitor
artifact:  io.github.mobilebytelabs:cmp-network-monitor
version:   3.2.1
package:   io.github.mobilebytelabs.kmptoolkit.networkmonitor
supabase:  false
di:        false   # factory function (provideNetworkMonitor), no Koin module
nav:       false   # no nav destinations
config:    none    # zero configuration required (Android auto-init via ContentProvider)

companion_module:
  artifact:  io.github.mobilebytelabs:cmp-network-monitor-compose
  package:   io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose
  requires:  compose-multiplatform

api:
  # Factory
  - createNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor
  - createScopedNetworkMonitor(scope: CoroutineScope, config): NetworkMonitor

  # DI Factory
  - provideNetworkMonitor(config: NetworkMonitorConfig? = null): NetworkMonitor

  # Interface: NetworkMonitor
  - isOnline: StateFlow<Boolean>
  - networkStatus: StateFlow<NetworkStatus>
  - networkChanges: SharedFlow<NetworkChangeEvent>
  - currentStatus: NetworkStatus
  - close()

  # Singleton Provider
  - NetworkMonitorProvider.install(config): NetworkMonitor
  - NetworkMonitorProvider.get(): NetworkMonitor
  - NetworkMonitorProvider.getOrNull(): NetworkMonitor?
  - NetworkMonitorProvider.reset()
  - NetworkMonitorProvider.redundantInstallCount: Int

  # Composite
  - NetworkMonitor.plus(other): NetworkMonitor   # operator+
  - CompositeNetworkMonitor(monitors: List<NetworkMonitor>)

  # Extensions — State Queries
  - NetworkMonitor.requireOnline()
  - NetworkMonitor.ensureOnline()
  - NetworkMonitor.withNetworkGuard(block): T
  - NetworkMonitor.awaitOnline(): NetworkInfo
  - NetworkMonitor.awaitConnectionType(type): NetworkInfo
  - NetworkMonitor.isConnectedVia(type): Boolean
  - NetworkMonitor.ifOnline(block): T?
  - NetworkMonitor.ifOffline(block): T?
  - NetworkMonitor.currentQuality: NetworkQuality

  # Extensions — Flow Operators
  - NetworkMonitor.onlyWhileOnline(): Flow<NetworkStatus>
  - NetworkMonitor.isOnlineDebounced(timeoutMs): Flow<Boolean>
  - NetworkMonitor.debouncedNetworkStatus(timeoutMs): Flow<NetworkStatus>
  - NetworkMonitor.networkQuality(): Flow<NetworkQuality>
  - Flow<T>.withNetworkState(monitor): Flow<Pair<T, NetworkStatus>>
  - Flow<T>.onlyWhileOnline(monitor): Flow<T>

  # Extensions — Retry & Lifecycle
  - NetworkMonitor.retryOnReconnect(maxRetries, timeoutMs, action): T
  - NetworkMonitor.closeGracefully()
  - NetworkMonitor.measureLatency(): Long
  - NetworkMonitor.addCallback(scope, onOnline, onOffline): CallbackHandle
  - NetworkMonitor.detectCaptivePortal(): CaptivePortalResult

  # Extensions — Bandwidth
  - NetworkMonitor.bandwidthSamples(): Flow<BandwidthSample>
  - NetworkMonitor.currentBandwidth: BandwidthSample?
  - NetworkMonitor.hasSufficientBandwidth(minKbps): Boolean

  # Testing
  - FakeNetworkMonitor(initialOnline, initialStatus)
    - setOnline(online: Boolean)
    - setNetworkStatus(status: NetworkStatus)
    - simulateHandoff(from: NetworkType, to: NetworkType)
    - simulateFlicker(info: NetworkInfo)
    - resetState(online: Boolean)
    - statusHistory: List<NetworkStatus>
    - eventHistory: List<NetworkChangeEvent>
    - updateCount: Int
    - isClosed: Boolean

  # Types
  - NetworkStatus (Available, Unavailable, CaptivePortal)
  - NetworkInfo (type, isMetered, downstreamBandwidthKbps, upstreamBandwidthKbps)
  - NetworkType (WiFi, Cellular, FiveG, Ethernet, VPN, Bluetooth, Unknown)
  - NetworkChangeEvent (Connected, Disconnected, TypeChanged, MeteredChanged, CaptivePortalDetected, CaptivePortalResolved)
  - NetworkQuality (Excellent, Good, Fair, Poor, Offline)
  - NetworkConstraint (predicate system with allOf() combinator)
  - NetworkMonitorConfig (pollIntervalMs, validationUrl, validationTimeoutMs, validationStrategy, backgroundPollIntervalMs, maxValidationBackoffMs, captivePortalDetection)
  - ValidationStrategy (NativeOnly, HttpOnly, NativeThenHttp)
  - CaptivePortalResult (NoCaptivePortal, CaptivePortalDetected, DetectionFailed)
  - BandwidthSample (downstreamKbps, upstreamKbps, timestampMs)
  - CallbackHandle (fun interface with close())
  - RetryPolicy (maxRetries, initialDelayMs, maxDelayMs, backoffMultiplier)
  - NetworkUnavailableException

  # Android-only
  - setApplicationContext(context: Context)  # fallback if ContentProvider disabled
  - LifecycleNetworkObserver(monitor, onStatusChanged)  # start(scope)/stop()

  # Compose module (cmp-network-monitor-compose)
  - NetworkAwareContent(monitor, offlineContent, captivePortalContent, onlineContent)
  - ConnectivityBanner(modifier, monitor, message, backgroundColor, contentColor)
  - rememberNetworkMonitor(config): NetworkMonitor
  - rememberScopedNetworkMonitor(config): NetworkMonitor
  - LocalNetworkMonitor: ProvidableCompositionLocal<NetworkMonitor>
```

---

## Usage

```bash
/sync-network-monitor           # Full sync
/sync-network-monitor --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-network-monitor
        |
        v
+--------------------------------------------------------------+
|  GATE 1: Gradle Dependency                                   |
|  Check: cmp-network-monitor:3.2.1 in libs.versions.toml     |
|  Check: used in commonMain.dependencies                      |
|  Fix:   Auto-insert correct entries                          |
|  Result: PASS / FIXED / BLOCKED                              |
+-------------------------------+------------------------------+
                                | PASS
                                v
+--------------------------------------------------------------+
|  GATE 1b: Compose Module (optional)                          |
|  If consuming app uses Compose Multiplatform:                |
|  Check: cmp-network-monitor-compose in libs.versions.toml    |
|  Check: used in commonMain.dependencies                      |
|  Fix:   Auto-insert correct entries                          |
|  Result: PASS / FIXED / SKIPPED                              |
+-------------------------------+------------------------------+
                                | PASS
                                v
         SYNC COMPLETE (Gate 2 + Gate 3 not applicable)
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   -> search "cmp-network-monitor"
   -> if found: verify version = 3.2.1
   -> if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   -> search "cmp.network.monitor"
   -> if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
cmp-network-monitor = "3.2.1"

# libs.versions.toml [libraries]
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "cmp-network-monitor" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.cmp.network.monitor)
```

### Gate 1b: Compose Module (optional)
```toml
# libs.versions.toml [libraries] — only if app uses Compose
cmp-network-monitor-compose = { module = "io.github.mobilebytelabs:cmp-network-monitor-compose", version.ref = "cmp-network-monitor" }
```
```kotlin
// build.gradle.kts commonMain.dependencies — only if app uses Compose
implementation(libs.cmp.network.monitor.compose)
```

---

## Gate 2 + Gate 3: Not Applicable

`cmp-network-monitor` is zero-config:
- No Supabase backend
- No DI module (factory function, no wiring needed)
- No nav destinations
- Android auto-initializes via ContentProvider (ACCESS_NETWORK_STATE permission auto-merged)

After Gate 1 passes, sync is complete.

---

## --check (Dry Run)

```
GATE 1   Gradle     [status]  cmp-network-monitor:3.2.1
GATE 1b  Compose    [status]  cmp-network-monitor-compose (optional)
GATE 2   Supabase   N/A
GATE 3   Wiring     N/A
```

---

## State Summary Output

```
+==================================================================+
|  /sync-network-monitor — COMPLETE                                 |
+==================================================================+
|  GATE 1   Gradle     [OK]  cmp-network-monitor:3.2.1             |
|  GATE 1b  Compose    [OK]  cmp-network-monitor-compose (optional) |
|  GATE 2   Supabase   N/A   no backend                            |
|  GATE 3   Wiring     N/A   zero-config module                    |
+------------------------------------------------------------------+
|  Docs: docs/network-monitor/SETUP.md                             |
+==================================================================+
```

---

## How to Evolve This File

1. **Version bump** -> update `version: 3.2.1` above
2. **New API method** -> update `api:` section
3. **If DI added** -> add Gate 3 wiring steps
