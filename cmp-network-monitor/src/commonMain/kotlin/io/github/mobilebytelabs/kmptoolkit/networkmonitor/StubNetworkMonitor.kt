package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Temporary stub implementation used by platforms that haven't been
 * implemented yet. Reports as online with [NetworkType.Unknown].
 *
 * Will be removed once all platform actuals are implemented (Phases 4-8).
 */
internal class StubNetworkMonitor : NetworkMonitor {

    override val isOnline: StateFlow<Boolean> =
        MutableStateFlow(true).asStateFlow()

    override val networkStatus: StateFlow<NetworkStatus> =
        MutableStateFlow<NetworkStatus>(
            NetworkStatus.Available(NetworkInfo()),
        ).asStateFlow()

    override val networkChanges: SharedFlow<NetworkChangeEvent> =
        MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()

    override fun close() {
        // No-op for stub
    }
}
