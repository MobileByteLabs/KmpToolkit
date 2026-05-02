package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Lifecycle-aware [NetworkMonitor] observation handle for Android.
 *
 * Starts collecting [NetworkMonitor.networkStatus] when [start] is called
 * and stops when [stop] is called. Designed to be called from lifecycle
 * callbacks (onStart/onStop) without adding an `androidx.lifecycle` dependency.
 *
 * Usage with AndroidX Lifecycle:
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *     private val observer = LifecycleNetworkObserver(monitor) { status ->
 *         when (status) {
 *             is NetworkStatus.Available -> hideOfflineBanner()
 *             is NetworkStatus.Unavailable -> showOfflineBanner()
 *             is NetworkStatus.CaptivePortal -> showCaptivePortalBanner()
 *         }
 *     }
 *
 *     override fun onStart() {
 *         super.onStart()
 *         observer.start(lifecycleScope)
 *     }
 *
 *     override fun onStop() {
 *         super.onStop()
 *         observer.stop()
 *     }
 * }
 * ```
 *
 * Usage with Compose:
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * DisposableEffect(monitor) {
 *     val observer = LifecycleNetworkObserver(monitor) { status -> ... }
 *     observer.start(scope)
 *     onDispose { observer.stop() }
 * }
 * ```
 */
class LifecycleNetworkObserver(
    private val monitor: NetworkMonitor,
    private val onStatusChanged: (NetworkStatus) -> Unit,
) {
    private var collectionJob: Job? = null

    /**
     * Start collecting network status changes.
     * Safe to call multiple times — cancels previous collection.
     */
    fun start(scope: CoroutineScope) {
        collectionJob?.cancel()
        collectionJob = monitor.networkStatus
            .onEach { status -> onStatusChanged(status) }
            .launchIn(scope)
    }

    /**
     * Stop collecting network status changes.
     * Safe to call multiple times.
     */
    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }

    /** Whether the observer is currently active. */
    val isActive: Boolean get() = collectionJob?.isActive == true
}
