package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Shared default for [ConnectivityProvider.monitoring] on always-observing implementations. */
internal val ALWAYS_MONITORING: StateFlow<Boolean> = MutableStateFlow(true)

/**
 * Low-level connectivity **engine** contract — the pluggable source behind a [NetworkMonitor].
 *
 * Every built-in platform monitor (Android `ConnectivityManager`, Apple `NWPathMonitor`, JVM
 * polling, JS `navigator.onLine`, and the native-less HTTP reachability engines) is a
 * `ConnectivityProvider`. Implement this to inject a **custom** engine — a test fake, a
 * corporate-VPN/on-prem detector, a different platform API, or an HTTP reachability poller — and
 * wrap it as a full [NetworkMonitor] via `createNetworkMonitor(provider)`.
 *
 * This is the seam that makes the engine swappable without touching the public [NetworkMonitor]
 * surface consumers code against.
 */
interface ConnectivityProvider {

    /** Hot-shared state: `true` = validated internet, `false` = no connection. */
    val isOnline: StateFlow<Boolean>

    /** Rich network status with type, metered, and bandwidth info. */
    val networkStatus: StateFlow<NetworkStatus>

    /** Discrete network change events for logging, analytics, and UI toasts. */
    val networkChanges: SharedFlow<NetworkChangeEvent>

    /**
     * Whether this engine is actively observing — `true` between [start] and [stop]/[close].
     * Always-observing engines report `true`.
     */
    val monitoring: StateFlow<Boolean> get() = ALWAYS_MONITORING

    /**
     * Begin observing. Most engines auto-start on creation (see
     * [NetworkMonitorConfig.autoStart]); call after constructing with `autoStart = false`, or to
     * resume after [stop]. Idempotent. Default no-op for always-on engines.
     */
    fun start() {}

    /**
     * Pause observing (unregister callbacks / stop polling) WITHOUT permanently releasing the
     * instance — re-startable via [start]. Use [close] for permanent teardown. Idempotent.
     * Default no-op.
     */
    fun stop() {}

    /**
     * On-demand active re-query of the platform's *current* connectivity — does NOT return the
     * cached [networkStatus] value, but re-reads the source now, updates the hot flows, and returns
     * the fresh result. Eliminates the stale-flow class of bug. Default returns the current value;
     * engines override to actually re-query.
     */
    suspend fun probe(): NetworkStatus = networkStatus.value

    /**
     * Release platform resources (unregister callbacks, cancel monitors, stop polling).
     * No-op if already closed. Safe to call multiple times.
     */
    fun close()
}
