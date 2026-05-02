package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Detailed information about the current network connection.
 *
 * @property type The type of network (WiFi, Cellular, etc.)
 * @property isMetered Whether the network is metered (e.g., mobile data)
 * @property downstreamBandwidthKbps Downstream bandwidth in kbps, or -1 if unknown
 * @property upstreamBandwidthKbps Upstream bandwidth in kbps, or -1 if unknown
 */
data class NetworkInfo(
    val type: NetworkType = NetworkType.Unknown,
    val isMetered: Boolean = false,
    val downstreamBandwidthKbps: Int = -1,
    val upstreamBandwidthKbps: Int = -1,
)
