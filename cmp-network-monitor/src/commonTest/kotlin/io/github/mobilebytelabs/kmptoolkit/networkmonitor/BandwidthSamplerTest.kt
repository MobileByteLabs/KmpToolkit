package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BandwidthSamplerTest {

    @Test
    fun currentBandwidthWhenOnline() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(
                NetworkInfo(
                    type = NetworkType.WiFi,
                    downstreamBandwidthKbps = 50_000,
                    upstreamBandwidthKbps = 10_000,
                ),
            ),
        )
        val sample = monitor.currentBandwidth
        assertEquals(50_000, sample.downstreamKbps)
        assertEquals(10_000, sample.upstreamKbps)
    }

    @Test
    fun currentBandwidthWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        val sample = monitor.currentBandwidth
        assertEquals(-1, sample.downstreamKbps)
        assertEquals(-1, sample.upstreamKbps)
    }

    @Test
    fun hasSufficientBandwidthTrue() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(
                NetworkInfo(downstreamBandwidthKbps = 10_000),
            ),
        )
        assertTrue(monitor.hasSufficientBandwidth(5_000))
    }

    @Test
    fun hasSufficientBandwidthFalse() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(
                NetworkInfo(downstreamBandwidthKbps = 1_000),
            ),
        )
        assertFalse(monitor.hasSufficientBandwidth(5_000))
    }

    @Test
    fun hasSufficientBandwidthWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        assertFalse(monitor.hasSufficientBandwidth(1))
    }

    @Test
    fun bandwidthSampleDefaultValues() {
        val sample = BandwidthSample()
        assertEquals(-1, sample.downstreamKbps)
        assertEquals(-1, sample.upstreamKbps)
    }
}
