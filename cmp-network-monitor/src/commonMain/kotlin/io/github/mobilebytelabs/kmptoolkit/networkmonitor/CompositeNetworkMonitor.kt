package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Combines multiple [NetworkMonitor]s into a single view.
 *
 * Reports online if ANY constituent monitor reports online (best-of union).
 * Picks the best-quality [NetworkStatus] when multiple are available.
 * Merges change events from all monitors.
 *
 * Usage:
 * ```kotlin
 * val composite = monitor1 + monitor2
 * composite.isOnline.collect { ... } // true if either is online
 * ```
 */
class CompositeNetworkMonitor(private val monitors: List<NetworkMonitor>) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isOnline = MutableStateFlow(monitors.any { it.isOnline.value })
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow(selectBestStatus())
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    init {
        // Combine all isOnline flows
        scope.launch {
            combine(monitors.map { it.isOnline }) { values ->
                values.any { it }
            }.collect { anyOnline ->
                _isOnline.value = anyOnline
            }
        }

        // Combine all networkStatus flows
        scope.launch {
            combine(monitors.map { it.networkStatus }) { _ ->
                selectBestStatus()
            }.collect { best ->
                _networkStatus.value = best
            }
        }

        // Merge all change event flows
        for (monitor in monitors) {
            scope.launch {
                monitor.networkChanges.collect { event ->
                    _networkChanges.tryEmit(event)
                }
            }
        }
    }

    private fun selectBestStatus(): NetworkStatus {
        val available = monitors.mapNotNull { m ->
            val s = m.networkStatus.value
            if (s is NetworkStatus.Available) s else null
        }
        if (available.isEmpty()) return NetworkStatus.Unavailable

        // Pick the one with best quality (lowest ordinal = Excellent)
        return available.minByOrNull { it.info.toQuality().ordinal }
            ?: NetworkStatus.Unavailable
    }

    override fun close() {
        scope.cancel()
        monitors.forEach { it.close() }
    }
}

/**
 * Combine two [NetworkMonitor]s. Reports online if either is online.
 */
operator fun NetworkMonitor.plus(other: NetworkMonitor): NetworkMonitor {
    val list = when {
        this is CompositeNetworkMonitor && other is CompositeNetworkMonitor ->
            listOf(this, other)

        else -> listOf(this, other)
    }
    return CompositeNetworkMonitor(list)
}
