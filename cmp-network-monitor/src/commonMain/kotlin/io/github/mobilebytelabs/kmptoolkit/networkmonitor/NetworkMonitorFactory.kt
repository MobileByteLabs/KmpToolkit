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

/**
 * Wraps a custom [ConnectivityProvider] engine as a full [NetworkMonitor], adding the consumer
 * conveniences ([NetworkMonitor.currentStatus], [NetworkMonitor.isMonitoring],
 * [NetworkMonitor.force]) on top of whatever engine you supply — a test fake, an HTTP reachability
 * poller, a VPN/on-prem detector, or a different platform API. The built-in platform monitors
 * already satisfy [ConnectivityProvider], so this is only for INJECTING your own engine.
 */
fun createNetworkMonitor(provider: ConnectivityProvider): NetworkMonitor =
    if (provider is NetworkMonitor) provider else ProviderBackedNetworkMonitor(provider)

private class ProviderBackedNetworkMonitor(
    private val delegate: ConnectivityProvider,
) : NetworkMonitor, ConnectivityProvider by delegate {
    // `force()` is a NetworkMonitor convenience (not on the engine) — best-effort: the caller can
    // observe the refreshed state via the flows. Providers with their own scope override probe().
    override fun force() { /* delegate exposes no scope; consumers call probe() for a suspending refresh */ }
}

