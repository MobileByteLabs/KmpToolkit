package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Discrete network change events for logging, analytics, and UI toasts.
 * Complements [NetworkMonitor.networkStatus] (current state) with transition events.
 */
sealed class NetworkChangeEvent {
    data class Connected(val info: NetworkInfo) : NetworkChangeEvent()
    data object Disconnected : NetworkChangeEvent()
    data class TypeChanged(val from: NetworkType, val to: NetworkType) : NetworkChangeEvent()
    data class MeteredChanged(val isMetered: Boolean) : NetworkChangeEvent()
}
