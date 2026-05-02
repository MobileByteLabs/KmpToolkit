package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Sealed class representing the current network connectivity status.
 */
sealed class NetworkStatus {

    /** Device has validated internet connection. */
    data class Available(val info: NetworkInfo) : NetworkStatus()

    /** No validated internet connection. */
    data object Unavailable : NetworkStatus()

    /** Device is connected but behind a captive portal (e.g., hotel WiFi login page). */
    data class CaptivePortal(val info: NetworkInfo, val redirectUrl: String? = null) : NetworkStatus()

    /** Convenience: true if [Available]. */
    val isOnline: Boolean get() = this is Available
}
