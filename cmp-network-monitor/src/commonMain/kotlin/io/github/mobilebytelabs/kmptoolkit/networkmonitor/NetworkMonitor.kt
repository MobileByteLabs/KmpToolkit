package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive network connectivity monitor.
 *
 * Provides hot-shared [StateFlow] properties for current connectivity state and
 * a [SharedFlow] for discrete transition events. Platform implementations use native
 * APIs (ConnectivityManager on Android, NWPathMonitor on Apple, polling on JVM/Linux/Windows,
 * navigator.onLine on JS/WasmJS).
 *
 * Thread-safety: Safe to collect from any dispatcher. [MutableStateFlow] handles concurrent
 * updates internally. Platform callbacks may fire on arbitrary threads — this is safe.
 *
 * Obtain an instance via [createNetworkMonitor].
 */
interface NetworkMonitor {

    /**
     * Hot-shared state: `true` = validated internet, `false` = no connection.
     * Emits current state immediately on collection (no cold-start gap).
     * Single platform callback for all collectors.
     */
    val isOnline: StateFlow<Boolean>

    /**
     * Rich network status with type, metered, and bandwidth info.
     * Emits [NetworkStatus.Unavailable] when offline.
     * Bandwidth rounded to nearest 100kbps to avoid spurious emissions.
     */
    val networkStatus: StateFlow<NetworkStatus>

    /**
     * Discrete network change events for logging, analytics, and UI toasts.
     * Complements [isOnline]/[networkStatus] (current state) with transition events.
     */
    val networkChanges: SharedFlow<NetworkChangeEvent>

    /**
     * Release platform resources (unregister callbacks, cancel monitors, stop polling).
     * Singleton usage (via [createNetworkMonitor]) typically never calls this.
     * Required for scoped/test usage to prevent callback leaks.
     * No-op if already closed. Safe to call multiple times.
     */
    fun close()

    /**
     * One-shot synchronous check. Returns current [networkStatus] value.
     * NOT suspend — reads [StateFlow.value] directly.
     */
    val currentStatus: NetworkStatus get() = networkStatus.value
}
