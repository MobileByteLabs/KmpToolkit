package io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
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
            _networkStatus.value = NetworkStatus.Available(info)
            _networkChanges.tryEmit(NetworkChangeEvent.Connected(info))
        } else if (!online && wasOnline) {
            _networkStatus.value = NetworkStatus.Unavailable
            _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
        }
    }

    /** Set the full network status. Also updates [isOnline] accordingly. */
    fun setNetworkStatus(status: NetworkStatus) {
        val oldStatus = _networkStatus.value
        _networkStatus.value = status
        _isOnline.value = status.isOnline

        // Emit change events
        when {
            status is NetworkStatus.Available && oldStatus is NetworkStatus.Unavailable -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Connected(status.info))
            }

            status is NetworkStatus.Unavailable && oldStatus is NetworkStatus.Available -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
            }

            status is NetworkStatus.Available && oldStatus is NetworkStatus.Available -> {
                if (status.info.type != oldStatus.info.type) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.TypeChanged(oldStatus.info.type, status.info.type),
                    )
                }
                if (status.info.isMetered != oldStatus.info.isMetered) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.MeteredChanged(status.info.isMetered),
                    )
                }
            }
        }
    }

    override fun close() {
        closed = true
    }

    /** Check if [close] was called. Useful for verifying cleanup in tests. */
    val isClosed: Boolean get() = closed
}
