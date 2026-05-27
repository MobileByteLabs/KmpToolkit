package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorConfig
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkQuality
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.createNetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.currentQuality
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.isOnlineDebouncedState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.networkQuality
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.networkStatusDebouncedState
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.toQuality
import kotlinx.coroutines.flow.map

/**
 * Remember and provide a [NetworkMonitor] singleton via [NetworkMonitorProvider].
 *
 * Uses the global singleton — safe to call from multiple composables.
 * The monitor is NOT disposed when the composable leaves composition
 * (it's a process-level singleton).
 *
 * The cached reference is keyed on [NetworkMonitorProvider.version], so if the
 * singleton is [NetworkMonitorProvider.reset] (or re-installed) elsewhere in the
 * process, the next recomposition discards the closed reference and re-installs
 * a fresh monitor — preventing composables from holding stale closed instances.
 *
 * @param config Configuration for the monitor. Only used on first install.
 */
@Composable
fun rememberNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor {
    val version by NetworkMonitorProvider.version.collectAsState()
    return remember(version) { NetworkMonitorProvider.install(config) }
}

/**
 * Remember a scoped [NetworkMonitor] that is created and closed with the composition.
 *
 * Unlike [rememberNetworkMonitor], this creates a NEW monitor instance that is
 * disposed when the composable leaves composition. Use for isolated monitoring
 * in specific screens.
 *
 * @param config Configuration for the monitor.
 */
@Composable
fun rememberScopedNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor {
    val monitor = remember { createNetworkMonitor(config) }
    DisposableEffect(Unit) {
        onDispose { monitor.close() }
    }
    return monitor
}

/**
 * Collect [NetworkMonitor.isOnline] as Compose [State].
 *
 * @param debounceMs Optional debounce window in milliseconds to suppress rapid
 * flicker (e.g. WiFi <-> Cellular handoff). When `<= 0L` (default), behaviour
 * is identical to collecting [NetworkMonitor.isOnline] directly. When `> 0L`,
 * a debounced hot [kotlinx.coroutines.flow.StateFlow] is created via
 * [isOnlineDebouncedState] scoped to the current composition.
 */
@Composable
fun NetworkMonitor.collectIsOnlineAsState(debounceMs: Long = 0L): State<Boolean> {
    if (debounceMs <= 0L) return isOnline.collectAsState()
    val scope = rememberCoroutineScope()
    val flow = remember(this, debounceMs) { isOnlineDebouncedState(scope, debounceMs) }
    return flow.collectAsState()
}

/**
 * Collect [NetworkMonitor.networkStatus] as Compose [State].
 *
 * @param debounceMs Optional debounce window in milliseconds. See
 * [collectIsOnlineAsState] for semantics.
 */
@Composable
fun NetworkMonitor.collectNetworkStatusAsState(debounceMs: Long = 0L): State<NetworkStatus> {
    if (debounceMs <= 0L) return networkStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val flow = remember(this, debounceMs) { networkStatusDebouncedState(scope, debounceMs) }
    return flow.collectAsState()
}

/**
 * Collect [NetworkQuality] as Compose [State].
 *
 * @param debounceMs Optional debounce window in milliseconds. When `> 0L`, the
 * upstream [NetworkStatus] is debounced BEFORE mapping to quality, so quality
 * transitions reflect the settled connection rather than transient flicker.
 */
@Composable
fun NetworkMonitor.collectNetworkQualityAsState(debounceMs: Long = 0L): State<NetworkQuality> {
    if (debounceMs <= 0L) return networkQuality().collectAsState(initial = currentQuality)
    val scope = rememberCoroutineScope()
    // No trailing distinctUntilChanged needed — networkStatusDebouncedState already
    // dedupes the upstream status; a pure-function mapping (status → quality) cannot
    // introduce new duplicates.
    val flow = remember(this, debounceMs) {
        networkStatusDebouncedState(scope, debounceMs).map { it.toQuality() }
    }
    return flow.collectAsState(initial = currentQuality)
}
