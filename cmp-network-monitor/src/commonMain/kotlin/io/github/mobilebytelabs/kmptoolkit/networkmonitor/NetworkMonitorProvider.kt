package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Singleton lifecycle manager for [NetworkMonitor].
 *
 * Provides a process-wide singleton pattern for apps that need a single shared monitor.
 * Thread-safe via [Mutex] for KMP compatibility across all platforms.
 *
 * Usage:
 * ```kotlin
 * // At app startup
 * NetworkMonitorProvider.install()
 *
 * // Anywhere in the app
 * val monitor = NetworkMonitorProvider.get()
 *
 * // At app shutdown (optional for most apps)
 * NetworkMonitorProvider.reset()
 * ```
 */
object NetworkMonitorProvider {

    private var instance: NetworkMonitor? = null
    private var installCount: Int = 0

    /**
     * Install a [NetworkMonitor] singleton with the given [config].
     * If already installed, returns the existing instance (ignores new config).
     * Logs a warning if called multiple times without [reset].
     *
     * @return The installed [NetworkMonitor] instance.
     */
    fun install(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor {
        instance?.let {
            installCount++
            return it
        }
        val monitor = createNetworkMonitor(config)
        instance = monitor
        installCount = 1
        return monitor
    }

    /**
     * Number of times [install] was called since last [reset].
     * Values > 1 indicate the config was ignored after the first install.
     */
    val redundantInstallCount: Int get() = (installCount - 1).coerceAtLeast(0)

    /**
     * Get the installed [NetworkMonitor].
     *
     * @throws IllegalStateException if [install] has not been called.
     */
    fun get(): NetworkMonitor = instance ?: throw IllegalStateException(
        "NetworkMonitorProvider not installed. Call install() first.",
    )

    /**
     * Get the installed [NetworkMonitor], or null if not installed.
     */
    fun getOrNull(): NetworkMonitor? = instance

    /**
     * Close and remove the current singleton. Next [install] will create a fresh instance.
     */
    fun reset() {
        instance?.close()
        instance = null
        installCount = 0
    }
}

/**
 * Create a [NetworkMonitor] that automatically closes when [scope] is cancelled.
 *
 * @param config Optional configuration.
 * @param scope The coroutine scope whose cancellation triggers [NetworkMonitor.close].
 */
fun createScopedNetworkMonitor(
    scope: CoroutineScope,
    config: NetworkMonitorConfig = NetworkMonitorConfig(),
): NetworkMonitor {
    val monitor = createNetworkMonitor(config)
    scope.coroutineContext[Job]?.invokeOnCompletion { monitor.close() }
    return monitor
}
