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
 *
 * NOT thread-safe. `install()` and `reset()` should be serialized by the caller — in
 * practice this happens naturally because both are app-startup / app-shutdown operations
 * invoked from a single thread. Reads via `get()` / `getOrNull()` / `version` / `currentStatus`
 * are safe to call from any thread once installation has settled.
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

    private val _version = MutableStateFlow(0)

    /**
     * Generation counter for the currently installed singleton.
     *
     * Increments exactly once on each successful [install] that creates a NEW instance
     * (redundant installs that return the cached instance do NOT bump it), and once on
     * every [reset]. Designed for reactive consumers — primarily Compose's
     * `rememberNetworkMonitor` — that need to drop a stale [NetworkMonitor] reference
     * when the singleton is reset or re-installed.
     */
    val version: StateFlow<Int> = _version.asStateFlow()

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
        _version.value += 1
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
     *
     * No-op when no singleton is installed (in particular: [version] does NOT advance on
     * a no-op reset, so reactive Compose consumers won't see a spurious recomposition).
     */
    fun reset() {
        if (instance == null) return
        instance?.close()
        instance = null
        installCount = 0
        _version.value += 1
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
