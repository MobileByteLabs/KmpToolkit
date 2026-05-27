package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorConfig

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

/**
 * Internal nullable mirror of [LocalNetworkMonitor], populated in lock-step by
 * [ProvideNetworkMonitor]. Exists because [LocalNetworkMonitor].current `error()`s when
 * unset — which can't be wrapped in `try`/`runCatching` inside a `@Composable` (compose
 * compiler rejects both). [rememberOrLocalNetworkMonitor] reads this nullable lookup so
 * it can detect "no provider in this subtree" without a thrown exception.
 */
private val LocalNetworkMonitorOrNull: ProvidableCompositionLocal<NetworkMonitor?> =
    staticCompositionLocalOf { null }

/**
 * Provide a [NetworkMonitor] to the composition subtree via [LocalNetworkMonitor].
 *
 * Convenience wrapper for `CompositionLocalProvider(LocalNetworkMonitor provides monitor) { ... }`.
 * Also populates an internal nullable mirror so [rememberOrLocalNetworkMonitor] can
 * detect the presence/absence of a provider without triggering the throwing default.
 *
 * ```kotlin
 * val customMonitor = remember { createNetworkMonitor(myConfig) }
 * ProvideNetworkMonitor(customMonitor) {
 *     MyScreenSubtree() // anything inside sees customMonitor via LocalNetworkMonitor.current
 * }
 * ```
 */
@Composable
fun ProvideNetworkMonitor(monitor: NetworkMonitor, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNetworkMonitor provides monitor,
        LocalNetworkMonitorOrNull provides monitor,
        content = content,
    )
}

/**
 * Return the [NetworkMonitor] from the surrounding [LocalNetworkMonitor] if one was
 * provided (via [ProvideNetworkMonitor] / `CompositionLocalProvider`); otherwise
 * fall back to the process-wide singleton from [rememberNetworkMonitor].
 *
 * Use this as the default `monitor` parameter on consumer composables that want to
 * accept both explicit scoping AND implicit singleton usage without two overloads.
 *
 * Note: detection of "no provider" uses the internal [LocalNetworkMonitorOrNull]
 * mirror that [ProvideNetworkMonitor] populates — direct
 * `CompositionLocalProvider(LocalNetworkMonitor provides monitor)` callers will
 * still see the singleton fallback here. Prefer [ProvideNetworkMonitor] to take
 * full advantage of this helper.
 */
@Composable
fun rememberOrLocalNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor {
    val fromLocal = LocalNetworkMonitorOrNull.current
    return fromLocal ?: rememberNetworkMonitor(config)
}
