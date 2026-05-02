package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor

/**
 * Animated banner that appears when the device goes offline.
 *
 * Slides in from the top when offline, slides out when online.
 * Designed to be placed at the top of a screen layout.
 *
 * @param monitor The [NetworkMonitor] to observe. Defaults to the global singleton.
 * @param message Text shown in the banner.
 * @param backgroundColor Banner background color.
 * @param contentColor Banner text color.
 * @param modifier Modifier for the outer container.
 */
@Composable
fun ConnectivityBanner(
    modifier: Modifier = Modifier,
    monitor: NetworkMonitor = rememberNetworkMonitor(),
    message: String = "No internet connection",
    backgroundColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
) {
    val isOnline by monitor.collectIsOnlineAsState()

    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
