package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor

/**
 * CompositionLocal for providing [NetworkMonitor] through the composition tree.
 *
 * Usage:
 * ```kotlin
 * // At the root
 * val monitor = rememberNetworkMonitor()
 * CompositionLocalProvider(LocalNetworkMonitor provides monitor) {
 *     MyApp()
 * }
 *
 * // Anywhere in the tree
 * val monitor = LocalNetworkMonitor.current
 * val isOnline by monitor.collectIsOnlineAsState()
 * ```
 *
 * Throws [IllegalStateException] if accessed without a provider.
 */
val LocalNetworkMonitor: ProvidableCompositionLocal<NetworkMonitor> =
    staticCompositionLocalOf {
        error(
            "No NetworkMonitor provided. Wrap your content with CompositionLocalProvider(LocalNetworkMonitor provides monitor).",
        )
    }
