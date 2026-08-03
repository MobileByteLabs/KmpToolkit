package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Reactive network connectivity monitor — the public API consumers code against.
 *
 * A [NetworkMonitor] IS a [ConnectivityProvider] (the swappable low-level engine) plus consumer
 * conveniences ([currentStatus], [isMonitoring], [force]). Obtain one via [createNetworkMonitor] —
 * either the platform default (`createNetworkMonitor(config)`) or by wrapping a custom
 * [ConnectivityProvider] (`createNetworkMonitor(provider)`).
 *
 * Platform implementations use native APIs (ConnectivityManager on Android, NWPathMonitor on
 * Apple, polling on JVM/Linux/Windows, navigator.onLine on JS/WasmJS).
 *
 * Thread-safety: safe to collect from any dispatcher. Platform callbacks may fire on arbitrary
 * threads — this is safe.
 */
interface NetworkMonitor : ConnectivityProvider {

    /**
     * One-shot synchronous read of the current [networkStatus] value. NOT a re-query — reads
     * [kotlinx.coroutines.flow.StateFlow.value] directly. Use [probe] to actively re-check.
     */
    val currentStatus: NetworkStatus get() = networkStatus.value

    /** Convenience for `monitoring.value` — whether the monitor is actively observing right now. */
    val isMonitoring: Boolean get() = monitoring.value

    /**
     * Fire-and-forget request to re-validate connectivity NOW — launches an async [probe] on the
     * monitor's own scope. Unlike [probe] it does not suspend or return a value; use it from UI
     * callbacks (pull-to-refresh, foreground resume) to nudge a fresh check. Default no-op.
     */
    fun force() {}
}
