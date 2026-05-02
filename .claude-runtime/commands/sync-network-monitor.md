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
version:   1.0.0
package:   io.github.mobilebytelabs.kmptoolkit.networkmonitor
supabase:  false
di:        false   # no DI module — factory function
nav:       false   # no nav destinations
config:    none    # zero configuration required (Android auto-init via ContentProvider)

api:
  # Factory
  - createNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor

  # Interface: NetworkMonitor
  - isOnline: StateFlow<Boolean>
  - networkStatus: StateFlow<NetworkStatus>
  - networkChanges: SharedFlow<NetworkChangeEvent>
  - currentStatus: NetworkStatus
  - close()

  # Extensions
  - NetworkMonitor.requireOnline()
  - NetworkMonitor.ensureOnline()
  - NetworkMonitor.withNetworkGuard(block): T
  - NetworkMonitor.awaitOnline(): NetworkInfo
  - NetworkMonitor.onlyWhileOnline(): Flow<NetworkStatus>
  - NetworkMonitor.retryOnReconnect(maxRetries, action): T
  - NetworkMonitor.isConnectedVia(type): Boolean

  # Testing
  - FakeNetworkMonitor(initialOnline, initialStatus)

  # Types
  - NetworkStatus (Available, Unavailable)
  - NetworkInfo (type, isMetered, downstreamBandwidthKbps, upstreamBandwidthKbps)
  - NetworkType (WiFi, Cellular, Ethernet, VPN, Bluetooth, Unknown)
  - NetworkChangeEvent (Connected, Disconnected, TypeChanged, MeteredChanged)
  - NetworkMonitorConfig (pollIntervalMs, validationUrl, validationTimeoutMs, validationStrategy)
  - ValidationStrategy (NativeOnly, HttpOnly, NativeThenHttp)
  - NetworkUnavailableException

  # Android-only
  - setApplicationContext(context: Context)  # fallback if ContentProvider disabled
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
|  Check: cmp-network-monitor:1.0.0 in libs.versions.toml     |
|  Check: used in commonMain.dependencies                      |
|  Fix:   Auto-insert correct entries                          |
|  Result: PASS / FIXED / BLOCKED                              |
+------------------------------+-------------------------------+
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
   -> if found: verify version = 1.0.0
   -> if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   -> search "cmp.network.monitor"
   -> if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
cmp-network-monitor = "1.0.0"

# libs.versions.toml [libraries]
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "cmp-network-monitor" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.cmp.network.monitor)
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
GATE 1  Gradle   [status]  cmp-network-monitor:1.0.0
GATE 2  Supabase N/A
GATE 3  Wiring   N/A
```

---

## State Summary Output

```
+==================================================================+
|  /sync-network-monitor — COMPLETE                                 |
+==================================================================+
|  GATE 1  Gradle     [OK]  cmp-network-monitor:1.0.0              |
|  GATE 2  Supabase   N/A   no backend                             |
|  GATE 3  Wiring     N/A   zero-config module                     |
+------------------------------------------------------------------+
|  Docs: docs/network-monitor/SETUP.md                             |
+==================================================================+
```

---

## How to Evolve This File

1. **Version bump** -> update `version: 1.0.0` above
2. **New API method** -> update `api:` section
3. **If DI added** -> add Gate 3 wiring steps
