package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkQualityTest {

    @Test
    fun excellentForFastUnmeteredWifi() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 50_000)
        assertEquals(NetworkQuality.Excellent, info.toQuality())
    }

    @Test
    fun goodForWifiWithDecentBandwidth() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 8_000)
        assertEquals(NetworkQuality.Good, info.toQuality())
    }

    @Test
    fun goodForWifiWithUnknownBandwidth() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = -1)
        assertEquals(NetworkQuality.Good, info.toQuality())
    }

    @Test
    fun fairForCellularWithModerateBandwidth() {
        val info = NetworkInfo(type = NetworkType.Cellular, isMetered = true, downstreamBandwidthKbps = 2_000)
        assertEquals(NetworkQuality.Fair, info.toQuality())
    }

    @Test
    fun fairForCellularWithUnknownBandwidth() {
        val info = NetworkInfo(type = NetworkType.Cellular, isMetered = true, downstreamBandwidthKbps = -1)
        assertEquals(NetworkQuality.Fair, info.toQuality())
    }

    @Test
    fun poorForLowBandwidth() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 500)
        assertEquals(NetworkQuality.Poor, info.toQuality())
    }

    @Test
    fun poorForUnknownTypeAndBandwidth() {
        val info = NetworkInfo(type = NetworkType.Unknown, isMetered = false, downstreamBandwidthKbps = -1)
        assertEquals(NetworkQuality.Poor, info.toQuality())
    }

    @Test
    fun offlineForUnavailableStatus() {
        assertEquals(NetworkQuality.Offline, NetworkStatus.Unavailable.toQuality())
    }

    @Test
    fun excellentForFast5G() {
        val info = NetworkInfo(type = NetworkType.FiveG, isMetered = true, downstreamBandwidthKbps = 200_000)
        assertEquals(NetworkQuality.Excellent, info.toQuality())
    }

    @Test
    fun goodFor5GWithModerateBandwidth() {
        val info = NetworkInfo(type = NetworkType.FiveG, isMetered = true, downstreamBandwidthKbps = 50_000)
        assertEquals(NetworkQuality.Good, info.toQuality())
    }

    @Test
    fun qualityFromAvailableStatus() {
        val status = NetworkStatus.Available(
            NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 50_000),
        )
        assertEquals(NetworkQuality.Excellent, status.toQuality())
    }
}
