package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor

/**
 * Connectivity status bar that sits at the very top of the screen.
 *
 * Always occupies at least the status-bar height so downstream toolbars never need to
 * handle the status-bar inset themselves. When the device goes offline the container
 * background animates to [backgroundColor] and the message row expands into view below
 * the status-bar icons; when back online the row collapses and the background fades back
 * to transparent — the toolbar stays pinned at its position throughout.
 *
 * Place this composable inside the [Scaffold] `topBar` slot (above the real [TopAppBar])
 * so the Scaffold measures it as part of the top bar and calls `consumeWindowInsets` for
 * the content area automatically.
 *
 * @param monitor The [NetworkMonitor] to observe. Defaults to the global singleton.
 * @param message Text shown in the banner when offline.
 * @param backgroundColor Banner background color (animates in/out).
 * @param contentColor Banner text/icon color.
 * @param modifier Modifier for the outer container.
 * @param debounceMs Optional debounce window in milliseconds. When `> 0L`, transient
 *   flicker (e.g. WiFi <-> Cellular handoff) is suppressed.
 * @param icon Optional leading icon displayed before the message.
 * @param trailingContent Optional composable slot rendered after the message.
 */
@Composable
fun ConnectivityBanner(
    modifier: Modifier = Modifier,
    monitor: NetworkMonitor = rememberNetworkMonitor(),
    message: String = "No internet connection",
    backgroundColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
    debounceMs: Long = 0L,
    icon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val isOnline by monitor.collectIsOnlineAsState(debounceMs)

    // Outer Box always provides statusBarsPadding so the status-bar inset is consumed
    // for the Scaffold content below — toolbars below this never double-apply it.
    // Background animates between transparent (online) and error color (offline).
    val bgColor by animateColorAsState(
        targetValue = if (isOnline) Color.Transparent else backgroundColor,
        label = "connectivity_banner_bg",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding(),
    ) {
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)

            if (icon != null || trailingContent != null) {
                Row(
                    modifier = contentModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = message,
                        color = contentColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    trailingContent?.invoke()
                }
            } else {
                Box(
                    modifier = contentModifier,
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
    }
}
