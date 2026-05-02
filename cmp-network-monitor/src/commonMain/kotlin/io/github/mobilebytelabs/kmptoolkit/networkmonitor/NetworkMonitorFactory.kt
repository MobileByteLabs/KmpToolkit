package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Creates a platform-specific [NetworkMonitor] instance.
 *
 * - **Android**: Uses ConnectivityManager + NetworkCallback (push-based). Context obtained
 *   automatically via ContentProvider; call [setApplicationContext] as fallback.
 * - **Apple** (iOS/macOS/tvOS/watchOS): Uses NWPathMonitor (push-based).
 * - **JVM**: Polling with configurable [NetworkMonitorConfig.pollIntervalMs].
 * - **JS/WasmJS**: Uses `navigator.onLine` + online/offline events.
 * - **Linux**: Reads `/sys/class/net` + polling.
 * - **Windows**: Uses `InternetGetConnectedState` + polling.
 * - **WasmWASI**: No-op stub (WASI has no network observation API).
 *
 * @param config Optional configuration for polling interval, validation URL, and strategy.
 */
expect fun createNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor
