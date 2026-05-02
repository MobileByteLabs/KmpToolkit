package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus

/**
 * Renders [onlineContent] when the network is available, [offlineContent] when unavailable.
 *
 * @param monitor The [NetworkMonitor] to observe. Defaults to the global singleton.
 * @param offlineContent Composable shown when offline.
 * @param onlineContent Composable shown when online, receives [NetworkStatus.Available].
 */
@Composable
fun NetworkAwareContent(
    monitor: NetworkMonitor = rememberNetworkMonitor(),
    offlineContent: @Composable () -> Unit = {},
    onlineContent: @Composable (NetworkStatus.Available) -> Unit,
) {
    val status by monitor.collectNetworkStatusAsState()

    when (val current = status) {
        is NetworkStatus.Available -> onlineContent(current)
        is NetworkStatus.CaptivePortal -> offlineContent()
        is NetworkStatus.Unavailable -> offlineContent()
    }
}
