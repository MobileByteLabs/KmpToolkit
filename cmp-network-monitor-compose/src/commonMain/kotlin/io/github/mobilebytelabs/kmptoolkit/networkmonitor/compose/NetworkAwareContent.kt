package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus

/**
 * Renders [onlineContent] when the network is available, [offlineContent] when unavailable,
 * and [captivePortalContent] when a captive portal is detected.
 *
 * @param monitor The [NetworkMonitor] to observe. Defaults to the global singleton.
 * @param offlineContent Composable shown when offline.
 * @param captivePortalContent Composable shown when a captive portal is detected. Defaults to [offlineContent].
 * @param debounceMs Optional debounce window in milliseconds applied to the
 * underlying [NetworkStatus]. When `<= 0L` (default), behaviour is identical to
 * the pre-debounce version. When `> 0L`, transient flicker (e.g. WiFi <->
 * Cellular handoff) is suppressed before content switches.
 * @param onlineContent Composable shown when online, receives [NetworkStatus.Available].
 */
@Composable
fun NetworkAwareContent(
    monitor: NetworkMonitor = rememberNetworkMonitor(),
    offlineContent: @Composable () -> Unit = {},
    captivePortalContent: @Composable (NetworkStatus.CaptivePortal) -> Unit = { offlineContent() },
    debounceMs: Long = 0L,
    onlineContent: @Composable (NetworkStatus.Available) -> Unit,
) {
    val status by monitor.collectNetworkStatusAsState(debounceMs)

    when (val current = status) {
        is NetworkStatus.Available -> onlineContent(current)
        is NetworkStatus.CaptivePortal -> captivePortalContent(current)
        is NetworkStatus.Unavailable -> offlineContent()
    }
}
