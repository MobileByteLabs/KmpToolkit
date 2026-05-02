# cmp-network-monitor — Manual Setup Guide

## 1. Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
cmp-network-monitor = "3.2.1"

[libraries]
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "cmp-network-monitor" }

# Optional: Compose extensions
cmp-network-monitor-compose = { module = "io.github.mobilebytelabs:cmp-network-monitor-compose", version.ref = "cmp-network-monitor" }
```

```kotlin
// build.gradle.kts (shared KMP module)
commonMain.dependencies {
    implementation(libs.cmp.network.monitor)
    // Optional: Compose extensions
    implementation(libs.cmp.network.monitor.compose)
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

## 3. Basic Usage

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

// Rich network status
monitor.networkStatus.collect { status ->
    when (status) {
        is NetworkStatus.Available -> {
            println("Type: ${status.info.type}")
            println("Metered: ${status.info.isMetered}")
        }
        is NetworkStatus.CaptivePortal -> println("Captive portal: ${status.redirectUrl}")
        is NetworkStatus.Unavailable -> println("Offline")
    }
}

// Cleanup (for scoped usage)
monitor.close()
```

## 4. Extension Functions

```kotlin
// Fail-fast
monitor.requireOnline()                        // throws NetworkUnavailableException

// Suspend until online
monitor.ensureOnline()

// Guard a network operation
val data = monitor.withNetworkGuard { api.fetchData() }

// Conditional execution
val result = monitor.ifOnline { info -> api.fetch() }  // returns null if offline
monitor.ifOffline { showCachedData() }                   // returns null if online

// Retry on reconnect
val result = monitor.retryOnReconnect(maxRetries = 3) { api.upload(payload) }

// Check type
if (monitor.isConnectedVia(NetworkType.WiFi)) { downloadLargeFile() }

// Network quality
monitor.networkQuality().collect { quality ->
    when (quality) {
        NetworkQuality.Excellent -> loadHighResImages()
        NetworkQuality.Good -> loadStandardImages()
        NetworkQuality.Fair -> loadThumbnails()
        NetworkQuality.Poor -> showTextOnly()
        NetworkQuality.Offline -> showCachedContent()
    }
}

// Combine with data Flow
dataFlow.withNetworkState(monitor).collect { (data, status) ->
    if (status.isOnline) showData(data) else showOfflineBanner(data)
}

// Debounced (filters WiFi<->Cellular handoff flicker)
monitor.isOnlineDebounced(300L).collect { online -> ... }

// Measure latency
val latencyMs = monitor.measureLatency()

// Callback-style API
val handle = monitor.addCallback(scope,
    onOnline = { info -> log("Online: ${info.type}") },
    onOffline = { log("Offline") },
)
handle.close() // stop receiving callbacks
```

## 5. Captive Portal Detection

```kotlin
val monitor = createNetworkMonitor(NetworkMonitorConfig {
    captivePortalDetection = true
})

monitor.networkStatus.collect { status ->
    when (status) {
        is NetworkStatus.CaptivePortal -> showLoginPrompt(status.redirectUrl)
        is NetworkStatus.Available -> hideAllBanners()
        is NetworkStatus.Unavailable -> showOfflineBanner()
    }
}
```

## 6. Composite Monitor

```kotlin
val wifiMonitor = createNetworkMonitor(config1)
val cellularMonitor = createNetworkMonitor(config2)
val combined = wifiMonitor + cellularMonitor

// Reports online if either is online, picks best quality
combined.isOnline.collect { ... }
```

## 7. DI Integration

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

// Singleton Provider (manual)
NetworkMonitorProvider.install()
val monitor = NetworkMonitorProvider.get()
```

## 8. Android Lifecycle

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

## 9. Compose Multiplatform (cmp-network-monitor-compose)

**Platform support**: Android, iOS, macOS, JVM, JS, WasmJS. tvOS, watchOS, Linux, Windows, and WasmWASI are not supported by Compose Multiplatform — use the core `cmp-network-monitor` module directly on those platforms.

```kotlin
// Provide monitor via CompositionLocal
CompositionLocalProvider(LocalNetworkMonitor provides monitor) {
    MyScreen()
}

// Use rememberNetworkMonitor
val monitor = rememberNetworkMonitor()

// NetworkAwareContent — auto-switches between online/offline UI
NetworkAwareContent(
    monitor = monitor,
    onlineContent = { MainContent() },
    offlineContent = { OfflinePlaceholder() },
    captivePortalContent = { portal -> CaptivePortalBanner(portal.redirectUrl) },
)

// ConnectivityBanner — shows banner when offline
ConnectivityBanner(monitor = monitor)
```

## 10. Testing

```kotlin
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor

val fakeMonitor = FakeNetworkMonitor(initialOnline = true)

// Simulate state changes
fakeMonitor.setOnline(false)  // triggers Disconnected event
fakeMonitor.setOnline(true)   // triggers Connected event

// Simulate handoff
fakeMonitor.simulateHandoff(from = NetworkType.WiFi, to = NetworkType.Cellular)

// Simulate flicker
fakeMonitor.simulateFlicker()

// Assert on history
assertEquals(3, fakeMonitor.updateCount)
assertTrue(fakeMonitor.eventHistory.any { it is NetworkChangeEvent.Disconnected })

// Reset between tests
fakeMonitor.resetState(online = true)

// Verify cleanup
viewModel.onCleared()
assertTrue(fakeMonitor.isClosed)
```

## 11. Configuration (optional)

```kotlin
val monitor = createNetworkMonitor(
    NetworkMonitorConfig {
        pollIntervalMs = 5_000L                        // JVM/Linux/Windows only
        validationUrl = "https://example.com/generate_204"
        validationTimeoutMs = 3_000L
        validationStrategy = ValidationStrategy.NativeThenHttp
        backgroundPollIntervalMs = 30_000L             // slow polling when idle
        maxValidationBackoffMs = 60_000L               // max backoff on failure
        captivePortalDetection = true                  // opt-in captive portal
    }
)
```
