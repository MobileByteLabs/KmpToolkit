package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorConfig
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkQuality
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.createNetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.currentQuality
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.networkQuality
import kotlinx.coroutines.flow.map

/**
 * Remember and provide a [NetworkMonitor] singleton via [NetworkMonitorProvider].
 *
 * Uses the global singleton — safe to call from multiple composables.
 * The monitor is NOT disposed when the composable leaves composition
 * (it's a process-level singleton).
 *
 * @param config Configuration for the monitor. Only used on first install.
 */
@Composable
fun rememberNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor =
    remember { NetworkMonitorProvider.install(config) }

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
 */
@Composable
fun NetworkMonitor.collectIsOnlineAsState(): State<Boolean> = isOnline.collectAsState()

/**
 * Collect [NetworkMonitor.networkStatus] as Compose [State].
 */
@Composable
fun NetworkMonitor.collectNetworkStatusAsState(): State<NetworkStatus> = networkStatus.collectAsState()

/**
 * Collect [NetworkQuality] as Compose [State].
 */
@Composable
fun NetworkMonitor.collectNetworkQualityAsState(): State<NetworkQuality> =
    networkQuality().collectAsState(initial = currentQuality)
