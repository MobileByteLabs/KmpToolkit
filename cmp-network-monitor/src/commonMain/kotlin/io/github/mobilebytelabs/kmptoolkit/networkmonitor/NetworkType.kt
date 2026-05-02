package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Type of network connection.
 */
enum class NetworkType {
    WiFi,
    Cellular,

    /** 5G New Radio connection. Detected on Android 10+ via [NetworkCapabilities.TRANSPORT_CELLULAR] + NR check. */
    FiveG,
    Ethernet,
    VPN,
    Bluetooth,
    Unknown,
}
