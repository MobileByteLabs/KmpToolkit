package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Coarse network quality signal derived from [NetworkInfo].
 *
 * Quality is inferred from bandwidth, network type, and metered status:
 * - **Excellent**: WiFi/Ethernet, unmetered, downstream >= 10 Mbps
 * - **Good**: WiFi/Ethernet with decent bandwidth, or fast cellular
 * - **Fair**: Metered connection or moderate bandwidth
 * - **Poor**: Low bandwidth or unknown type
 * - **Offline**: No connection
 */
enum class NetworkQuality {
    Excellent,
    Good,
    Fair,
    Poor,
    Offline,
}

/**
 * Derive [NetworkQuality] from [NetworkInfo].
 */
fun NetworkInfo.toQuality(): NetworkQuality {
    val bandwidth = downstreamBandwidthKbps
    return when {
        // 5G is always at least Good
        type == NetworkType.FiveG -> if (bandwidth >= 100_000) NetworkQuality.Excellent else NetworkQuality.Good

        // Fast unmetered connection
        !isMetered && bandwidth >= 10_000 -> NetworkQuality.Excellent

        // Good wired/WiFi or fast cellular
        type in setOf(NetworkType.WiFi, NetworkType.Ethernet) && bandwidth >= 5_000 -> NetworkQuality.Good

        bandwidth >= 5_000 -> NetworkQuality.Good

        // Moderate connection
        type == NetworkType.Cellular && bandwidth >= 1_000 -> NetworkQuality.Fair

        type in setOf(NetworkType.WiFi, NetworkType.Ethernet) && bandwidth == -1 -> NetworkQuality.Good

        // unknown bandwidth on WiFi = assume good
        bandwidth >= 1_000 -> NetworkQuality.Fair

        // Low bandwidth or unknown
        bandwidth in 1..999 -> NetworkQuality.Poor

        // Unknown bandwidth on non-WiFi
        type == NetworkType.Cellular -> NetworkQuality.Fair

        type == NetworkType.Unknown -> NetworkQuality.Poor

        // Default for connected but unknown
        else -> NetworkQuality.Fair
    }
}

/**
 * Derive [NetworkQuality] from [NetworkStatus].
 */
fun NetworkStatus.toQuality(): NetworkQuality = when (this) {
    is NetworkStatus.Available -> info.toQuality()
    is NetworkStatus.CaptivePortal -> NetworkQuality.Poor
    is NetworkStatus.Unavailable -> NetworkQuality.Offline
}
