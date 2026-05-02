package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Snapshot of bandwidth at a point in time.
 *
 * @property downstreamKbps Downstream bandwidth in kbps, or -1 if unknown.
 * @property upstreamKbps Upstream bandwidth in kbps, or -1 if unknown.
 * @property timestampMs Epoch milliseconds when the sample was taken.
 */
data class BandwidthSample(
    val downstreamKbps: Int = -1,
    val upstreamKbps: Int = -1,
    val timestampMs: Long = currentTimeMillis(),
)

/**
 * Flow of [BandwidthSample] derived from the network monitor's status.
 * Emits only when bandwidth values change.
 */
fun NetworkMonitor.bandwidthSamples(): Flow<BandwidthSample> = networkStatus.map { status ->
    when (status) {
        is NetworkStatus.Available -> BandwidthSample(
            downstreamKbps = status.info.downstreamBandwidthKbps,
            upstreamKbps = status.info.upstreamBandwidthKbps,
        )

        is NetworkStatus.CaptivePortal -> BandwidthSample(
            downstreamKbps = status.info.downstreamBandwidthKbps,
            upstreamKbps = status.info.upstreamBandwidthKbps,
        )

        is NetworkStatus.Unavailable -> BandwidthSample()
    }
}.distinctUntilChanged()

/**
 * Current bandwidth snapshot.
 */
val NetworkMonitor.currentBandwidth: BandwidthSample
    get() = when (val status = networkStatus.value) {
        is NetworkStatus.Available -> BandwidthSample(
            downstreamKbps = status.info.downstreamBandwidthKbps,
            upstreamKbps = status.info.upstreamBandwidthKbps,
        )

        is NetworkStatus.CaptivePortal -> BandwidthSample(
            downstreamKbps = status.info.downstreamBandwidthKbps,
            upstreamKbps = status.info.upstreamBandwidthKbps,
        )

        is NetworkStatus.Unavailable -> BandwidthSample()
    }

/**
 * Check if current bandwidth meets the minimum threshold.
 *
 * @param minDownstreamKbps Minimum downstream bandwidth in kbps.
 * @return true if bandwidth is known and >= threshold.
 */
fun NetworkMonitor.hasSufficientBandwidth(minDownstreamKbps: Int): Boolean {
    val status = networkStatus.value
    if (status !is NetworkStatus.Available) return false
    val downstream = status.info.downstreamBandwidthKbps
    return downstream >= minDownstreamKbps
}

/**
 * Platform-specific current time in milliseconds.
 */
internal expect fun currentTimeMillis(): Long
