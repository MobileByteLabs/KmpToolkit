package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Sealed class representing the current network connectivity status.
 */
sealed class NetworkStatus {

    /** Device has validated internet connection. */
    data class Available(val info: NetworkInfo) : NetworkStatus()

    /** No validated internet connection. */
    data object Unavailable : NetworkStatus()

    /** Convenience: true if [Available]. */
    val isOnline: Boolean get() = this is Available
}
