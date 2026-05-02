# cmp-network-monitor

Reactive network connectivity monitoring for **Kotlin Multiplatform** — StateFlow-based, all 21 KMP targets.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-network-monitor)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-network-monitor)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Features

- **StateFlow-first API** — hot-shared `isOnline`, `networkStatus`, and `networkChanges` flows
- **Rich network info** — connection type (WiFi/Cellular/Ethernet/VPN/Bluetooth), metered status, bandwidth
- **Discrete change events** — Connected, Disconnected, TypeChanged, MeteredChanged, CaptivePortalDetected
- **Captive portal detection** — opt-in HTTP redirect detection for hotel WiFi, airport networks
- **Configurable validation** — NativeOnly, HttpOnly, or NativeThenHttp (catches captive portals)
- **Adaptive polling** — polling-based monitors grow intervals during stable periods, reset on change
- **Bandwidth sampling** — `bandwidthSamples()` Flow, `currentBandwidth`, quality signals (Excellent/Good/Fair/Poor)
- **Composite monitors** — combine monitors via `monitor1 + monitor2`, reports online if ANY is online
- **Cold-start cache** — persists last-known state to disk for instant startup
- **21 KMP targets** across 11 platforms — Android, iOS, macOS, tvOS, watchOS, JVM, JS, WasmJS, WasmWASI, Linux, Windows
- **Zero dependencies** beyond kotlinx-coroutines — no DI, no Compose required
- **Test-friendly** — `FakeNetworkMonitor` published in main artifact with status/event history tracking
- **15+ extension functions** — `requireOnline()`, `ensureOnline()`, `ifOnline{}`, `ifOffline{}`, `measureLatency()`, `addCallback()`, `retryOnReconnect{}`, and more

## Platform Implementations

| Platform | API | Type |
|----------|-----|------|
| Android | ConnectivityManager + NetworkCallback | Push-based |
| iOS/macOS/tvOS/watchOS | NWPathMonitor | Push-based |
| JVM | NetworkInterface + HTTP HEAD | Poll + validate |
| JS | navigator.onLine + events | Push-based |
| WasmJS | navigator.onLine + events | Push-based |
| Linux | /sys/class/net polling | Poll-based |
| Windows (MinGW) | Polling stub | Poll-based |
| WasmWASI | No-op stub | Stub |

## Installation

### Gradle (libs.versions.toml)

```toml
[versions]
cmp-network-monitor = "3.2.1"

[libraries]
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "cmp-network-monitor" }
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.cmp.network.monitor)
}
```

### Android Setup

The library automatically initializes via `NetworkMonitorInitProvider` (ContentProvider).
No manual setup needed. The `ACCESS_NETWORK_STATE` permission is included in the library manifest.

If you disable the ContentProvider (e.g., via `tools:node="remove"`), call manually:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setApplicationContext(this)
    }
}
```

## Usage

### Basic — Check connectivity

```kotlin
val monitor = createNetworkMonitor()

// One-shot check
if (monitor.isOnline.value) {
    println("Online!")
}

// Reactive — collect in a coroutine
monitor.isOnline.collect { online ->
    println("Connectivity: $online")
}
```

### Rich network info

```kotlin
monitor.networkStatus.collect { status ->
    when (status) {
        is NetworkStatus.Available -> {
            println("Type: ${status.info.type}")       // WiFi, Cellular, etc.
            println("Metered: ${status.info.isMetered}")
            println("Down: ${status.info.downstreamBandwidthKbps} kbps")
        }
        NetworkStatus.Unavailable -> println("Offline")
    }
}
```

### Discrete change events

```kotlin
monitor.networkChanges.collect { event ->
    when (event) {
        is NetworkChangeEvent.Connected -> println("Connected via ${event.info.type}")
        is NetworkChangeEvent.Disconnected -> println("Lost connection")
        is NetworkChangeEvent.TypeChanged -> println("Switched ${event.from} -> ${event.to}")
        is NetworkChangeEvent.MeteredChanged -> println("Metered: ${event.isMetered}")
    }
}
```

### Extension functions

```kotlin
// Fail-fast if offline
monitor.requireOnline() // throws NetworkUnavailableException

// Suspend until online
monitor.ensureOnline()

// Guard a network operation
val data = monitor.withNetworkGuard {
    api.fetchData()
}

// Get NetworkInfo when online
val info = monitor.awaitOnline()

// Flow that completes when offline
monitor.onlyWhileOnline().collect { status -> ... }

// Retry on reconnect
val result = monitor.retryOnReconnect(maxRetries = 3) {
    api.upload(payload)
}

// Check specific connection type
if (monitor.isConnectedVia(NetworkType.WiFi)) {
    downloadLargeFile()
}
```

### Custom configuration

```kotlin
val monitor = createNetworkMonitor(
    NetworkMonitorConfig(
        pollIntervalMs = 5_000L,                    // JVM/Linux/Windows only
        validationUrl = "https://example.com/check", // HTTP validation endpoint
        validationTimeoutMs = 3_000L,
        validationStrategy = ValidationStrategy.NativeThenHttp, // default
    )
)
```

### Compose Multiplatform integration

The library doesn't depend on Compose to maintain full target coverage.
Use these patterns in your Compose UI:

```kotlin
@Composable
fun NetworkAwareScreen() {
    val monitor = remember { createNetworkMonitor() }
    DisposableEffect(monitor) { onDispose { monitor.close() } }

    val isOnline by monitor.isOnline.collectAsState()
    val status by monitor.networkStatus.collectAsState()

    if (!isOnline) {
        OfflineBanner()
    }

    // Listen for change events
    LaunchedEffect(monitor) {
        monitor.networkChanges.collect { event ->
            when (event) {
                is NetworkChangeEvent.Disconnected -> showSnackbar("Connection lost")
                is NetworkChangeEvent.Connected -> showSnackbar("Back online")
                else -> {}
            }
        }
    }
}
```

### Testing

`FakeNetworkMonitor` is published in the main artifact — no separate test dependency needed:

```kotlin
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor

@Test
fun viewModelHandlesOffline() = runTest {
    val monitor = FakeNetworkMonitor(initialOnline = true)
    val viewModel = MyViewModel(monitor)

    monitor.setOnline(false) // triggers Disconnected event
    assertEquals(UiState.Offline, viewModel.state.value)

    monitor.setOnline(true) // triggers Connected event
    assertEquals(UiState.Online, viewModel.state.value)

    // Verify cleanup
    viewModel.onCleared()
    assertTrue(monitor.isClosed)
}
```

### Cleanup

```kotlin
// Close when done (releases platform resources)
monitor.close()

// For singleton usage (app-level), typically never close
```

## API Reference

### Core Interface

| Property/Method | Type | Description |
|----------------|------|-------------|
| `isOnline` | `StateFlow<Boolean>` | Hot-shared connectivity state |
| `networkStatus` | `StateFlow<NetworkStatus>` | Rich status with type/metered/bandwidth |
| `networkChanges` | `SharedFlow<NetworkChangeEvent>` | Discrete transition events |
| `currentStatus` | `NetworkStatus` | One-shot synchronous read |
| `close()` | `Unit` | Release platform resources |

### Types

| Type | Values |
|------|--------|
| `NetworkType` | WiFi, Cellular, FiveG, Ethernet, VPN, Bluetooth, Unknown |
| `NetworkStatus` | Available(info), Unavailable, CaptivePortal(info, redirectUrl) |
| `NetworkChangeEvent` | Connected, Disconnected, TypeChanged, MeteredChanged, CaptivePortalDetected, CaptivePortalResolved |
| `NetworkQuality` | Excellent, Good, Fair, Poor, Offline |
| `ValidationStrategy` | NativeOnly, HttpOnly, NativeThenHttp |

## Advanced Usage

### Captive Portal Detection

```kotlin
val monitor = createNetworkMonitor(NetworkMonitorConfig {
    captivePortalDetection = true
})

monitor.networkStatus.collect { status ->
    when (status) {
        is NetworkStatus.CaptivePortal -> showCaptivePortalBanner(status.redirectUrl)
        is NetworkStatus.Available -> hideAllBanners()
        is NetworkStatus.Unavailable -> showOfflineBanner()
    }
}
```

### Network Quality Signal

```kotlin
monitor.networkQuality().collect { quality ->
    when (quality) {
        NetworkQuality.Excellent -> loadHighResImages()
        NetworkQuality.Good -> loadStandardImages()
        NetworkQuality.Fair -> loadThumbnails()
        NetworkQuality.Poor -> showTextOnly()
        NetworkQuality.Offline -> showCachedContent()
    }
}
```

### Combine Data Flow with Network State

```kotlin
dataFlow.withNetworkState(monitor).collect { (data, status) ->
    if (status.isOnline) showData(data) else showOfflineBanner(data)
}
```

### Conditional Execution

```kotlin
val data = monitor.ifOnline { info ->
    if (info.type == NetworkType.WiFi) api.fetchFullData() else api.fetchLiteData()
}

monitor.ifOffline {
    showCachedData()
}
```

### Callback-style API

```kotlin
val handle = monitor.addCallback(
    scope = viewModelScope,
    onOnline = { info -> updateUI(info) },
    onOffline = { showOfflineBanner() },
)
// Later:
handle.close()
```

### Composite Monitor

```kotlin
val wifiMonitor = createNetworkMonitor(NetworkMonitorConfig { ... })
val cellularMonitor = createNetworkMonitor(NetworkMonitorConfig { ... })
val combined = wifiMonitor + cellularMonitor
combined.isOnline.collect { ... } // true if either is online
```

### DI Integration

```kotlin
// Koin
val appModule = module {
    single<NetworkMonitor> { provideNetworkMonitor(getOrNull()) }
}

// Hilt
@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideMonitor(): NetworkMonitor = provideNetworkMonitor()
}
```

### Android Lifecycle

```kotlin
class MyActivity : AppCompatActivity() {
    private val observer = LifecycleNetworkObserver(monitor) { status ->
        when (status) {
            is NetworkStatus.Available -> hideOfflineBanner()
            is NetworkStatus.Unavailable -> showOfflineBanner()
            is NetworkStatus.CaptivePortal -> showCaptivePortalBanner()
        }
    }

    override fun onStart() {
        super.onStart()
        observer.start(lifecycleScope)
    }

    override fun onStop() {
        super.onStop()
        observer.stop()
    }
}
```

## License

```
Copyright 2026 MobileByteLabs

Licensed under the Apache License, Version 2.0
```
