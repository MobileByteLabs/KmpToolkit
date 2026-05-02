package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class NetworkInfoTest {

    @Test
    fun defaultValuesAreCorrect() {
        val info = NetworkInfo()
        assertEquals(NetworkType.Unknown, info.type)
        assertFalse(info.isMetered)
        assertEquals(-1, info.downstreamBandwidthKbps)
        assertEquals(-1, info.upstreamBandwidthKbps)
    }

    @Test
    fun customValuesArePreserved() {
        val info = NetworkInfo(
            type = NetworkType.WiFi,
            isMetered = false,
            downstreamBandwidthKbps = 54_000,
            upstreamBandwidthKbps = 12_000,
        )
        assertEquals(NetworkType.WiFi, info.type)
        assertFalse(info.isMetered)
        assertEquals(54_000, info.downstreamBandwidthKbps)
        assertEquals(12_000, info.upstreamBandwidthKbps)
    }

    @Test
    fun meteredCellularNetwork() {
        val info = NetworkInfo(
            type = NetworkType.Cellular,
            isMetered = true,
            downstreamBandwidthKbps = 10_000,
            upstreamBandwidthKbps = 2_000,
        )
        assertEquals(NetworkType.Cellular, info.type)
        assertEquals(true, info.isMetered)
    }

    @Test
    fun dataClassEqualityWorks() {
        val a = NetworkInfo(type = NetworkType.WiFi)
        val b = NetworkInfo(type = NetworkType.WiFi)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun dataClassInequalityOnDifferentType() {
        val wifi = NetworkInfo(type = NetworkType.WiFi)
        val cell = NetworkInfo(type = NetworkType.Cellular)
        assertNotEquals(wifi, cell)
    }

    @Test
    fun copyPreservesUnchangedFields() {
        val original = NetworkInfo(
            type = NetworkType.Ethernet,
            isMetered = false,
            downstreamBandwidthKbps = 1_000_000,
            upstreamBandwidthKbps = 500_000,
        )
        val copied = original.copy(isMetered = true)
        assertEquals(NetworkType.Ethernet, copied.type)
        assertEquals(true, copied.isMetered)
        assertEquals(1_000_000, copied.downstreamBandwidthKbps)
    }
}
