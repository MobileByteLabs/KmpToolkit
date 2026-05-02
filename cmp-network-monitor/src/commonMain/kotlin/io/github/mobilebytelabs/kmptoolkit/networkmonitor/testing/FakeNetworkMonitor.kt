package io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [NetworkMonitor].
 *
 * Backed by [MutableStateFlow] so tests can control the connectivity state.
 * Published in the main artifact (not test-only) so consumer projects can
 * import it directly in their test sources without a separate `-testing` artifact.
 *
 * Usage:
 * ```kotlin
 * val fakeMonitor = FakeNetworkMonitor()
 * fakeMonitor.setOnline(true)
 * fakeMonitor.setNetworkStatus(NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi)))
 * ```
 */
class FakeNetworkMonitor(
    initialOnline: Boolean = true,
    initialStatus: NetworkStatus = if (initialOnline) {
        NetworkStatus.Available(NetworkInfo())
    } else {
        NetworkStatus.Unavailable
    },
) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow(initialStatus)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private var closed = false

    /** Set the online state and emit the corresponding [NetworkChangeEvent]. */
    fun setOnline(online: Boolean) {
        val wasOnline = _isOnline.value
        _isOnline.value = online
        if (online && !wasOnline) {
            val info = (_networkStatus.value as? NetworkStatus.Available)?.info ?: NetworkInfo()
            val status = NetworkStatus.Available(info)
            _networkStatus.value = status
            val event = NetworkChangeEvent.Connected(info)
            _networkChanges.tryEmit(event)
            trackUpdate(status)
            trackEvent(event)
        } else if (!online && wasOnline) {
            val status = NetworkStatus.Unavailable
            _networkStatus.value = status
            val event = NetworkChangeEvent.Disconnected
            _networkChanges.tryEmit(event)
            trackUpdate(status)
            trackEvent(event)
        }
    }

    /** Set the full network status. Also updates [isOnline] accordingly. */
    fun setNetworkStatus(status: NetworkStatus) {
        val oldStatus = _networkStatus.value
        _networkStatus.value = status
        _isOnline.value = status.isOnline
        trackUpdate(status)

        // Emit change events
        when {
            status is NetworkStatus.Available && oldStatus is NetworkStatus.Unavailable -> {
                val event = NetworkChangeEvent.Connected(status.info)
                _networkChanges.tryEmit(event)
                trackEvent(event)
            }

            status is NetworkStatus.Unavailable && oldStatus is NetworkStatus.Available -> {
                val event = NetworkChangeEvent.Disconnected
                _networkChanges.tryEmit(event)
                trackEvent(event)
            }

            status is NetworkStatus.Available && oldStatus is NetworkStatus.Available -> {
                if (status.info.type != oldStatus.info.type) {
                    val event = NetworkChangeEvent.TypeChanged(oldStatus.info.type, status.info.type)
                    _networkChanges.tryEmit(event)
                    trackEvent(event)
                }
                if (status.info.isMetered != oldStatus.info.isMetered) {
                    val event = NetworkChangeEvent.MeteredChanged(status.info.isMetered)
                    _networkChanges.tryEmit(event)
                    trackEvent(event)
                }
            }
        }
    }

    override fun close() {
        closed = true
    }

    /** Check if [close] was called. Useful for verifying cleanup in tests. */
    val isClosed: Boolean get() = closed

    /** History of all status changes for assertion. */
    private val _statusHistory = mutableListOf(initialStatus)

    /** Get the complete history of [NetworkStatus] changes. */
    val statusHistory: List<NetworkStatus> get() = _statusHistory.toList()

    /** History of all change events emitted. */
    private val _eventHistory = mutableListOf<NetworkChangeEvent>()

    /** Get the complete history of [NetworkChangeEvent]s. */
    val eventHistory: List<NetworkChangeEvent> get() = _eventHistory.toList()

    /** Number of times [setOnline] or [setNetworkStatus] was called. */
    var updateCount: Int = 0
        private set

    /** Simulate a rapid WiFi ↔ Cellular handoff sequence. */
    fun simulateHandoff(from: NetworkType, to: NetworkType) {
        setNetworkStatus(NetworkStatus.Unavailable)
        setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = to, isMetered = to == NetworkType.Cellular)),
        )
    }

    /** Simulate going offline briefly then back online with the given [info]. */
    fun simulateFlicker(info: NetworkInfo = NetworkInfo()) {
        setOnline(false)
        setOnline(true)
        setNetworkStatus(NetworkStatus.Available(info))
    }

    /** Reset all state to initial values. Useful between test cases. */
    fun resetState(online: Boolean = true) {
        closed = false
        _statusHistory.clear()
        _eventHistory.clear()
        updateCount = 0
        val status = if (online) {
            NetworkStatus.Available(NetworkInfo())
        } else {
            NetworkStatus.Unavailable
        }
        _isOnline.value = online
        _networkStatus.value = status
        _statusHistory.add(status)
    }

    // Track history on updates
    private fun trackUpdate(status: NetworkStatus) {
        _statusHistory.add(status)
        updateCount++
    }

    // Override to track events
    private fun trackEvent(event: NetworkChangeEvent) {
        _eventHistory.add(event)
    }
}
