# cmp-network-monitor — Manual Setup Guide

## 1. Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
cmp-network-monitor = "1.0.0"

[libraries]
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "cmp-network-monitor" }
```

```kotlin
// build.gradle.kts (shared KMP module)
commonMain.dependencies {
    implementation(libs.cmp.network.monitor)
}
```

## 2. Android (automatic)

The library auto-initializes via `NetworkMonitorInitProvider`. The `ACCESS_NETWORK_STATE` permission is included in the library manifest and auto-merged.

No additional setup required.

**Optional fallback** (if ContentProvider is removed):
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setApplicationContext(this)
    }
}
```

## 3. Usage

```kotlin
// Create a monitor (use as singleton or scoped)
val monitor = createNetworkMonitor()

// Check current state
val isOnline = monitor.isOnline.value
val status = monitor.currentStatus

// Collect reactively
scope.launch {
    monitor.isOnline.collect { online ->
        updateUI(online)
    }
}

// Extension functions
monitor.requireOnline()                    // throws if offline
monitor.ensureOnline()                     // suspends until online
val data = monitor.withNetworkGuard { ... } // wait + execute
val info = monitor.awaitOnline()           // get NetworkInfo
monitor.isConnectedVia(NetworkType.WiFi)   // check type

// Cleanup (for scoped usage)
monitor.close()
```

## 4. Testing

```kotlin
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor

val fakeMonitor = FakeNetworkMonitor(initialOnline = true)
fakeMonitor.setOnline(false)  // simulate offline
fakeMonitor.setNetworkStatus(NetworkStatus.Available(
    NetworkInfo(type = NetworkType.WiFi, isMetered = false)
))
```

## 5. Configuration (optional)

```kotlin
val monitor = createNetworkMonitor(
    NetworkMonitorConfig(
        pollIntervalMs = 5_000L,                        // JVM/Linux/Windows only
        validationUrl = "https://example.com/generate_204",
        validationTimeoutMs = 3_000L,
        validationStrategy = ValidationStrategy.NativeThenHttp,
    )
)
```
