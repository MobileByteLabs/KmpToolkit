package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CompositeNetworkMonitorTest {

    @Test
    fun reportsOnlineIfAnyMonitorIsOnline() {
        val m1 = FakeNetworkMonitor(initialOnline = true)
        val m2 = FakeNetworkMonitor(initialOnline = false)
        val composite = CompositeNetworkMonitor(listOf(m1, m2))

        assertTrue(composite.isOnline.value)
        composite.close()
    }

    @Test
    fun reportsOfflineIfAllMonitorsOffline() {
        val m1 = FakeNetworkMonitor(initialOnline = false)
        val m2 = FakeNetworkMonitor(initialOnline = false)
        val composite = CompositeNetworkMonitor(listOf(m1, m2))

        assertFalse(composite.isOnline.value)
        composite.close()
    }

    @Test
    fun selectsBestQualityStatus() {
        val wifiInfo = NetworkInfo(type = NetworkType.WiFi, downstreamBandwidthKbps = 50_000)
        val cellInfo = NetworkInfo(type = NetworkType.Cellular, isMetered = true, downstreamBandwidthKbps = 1_000)

        val m1 = FakeNetworkMonitor()
        val m2 = FakeNetworkMonitor()
        m1.setNetworkStatus(NetworkStatus.Available(wifiInfo))
        m2.setNetworkStatus(NetworkStatus.Available(cellInfo))

        val composite = CompositeNetworkMonitor(listOf(m1, m2))
        val status = composite.networkStatus.value
        assertIs<NetworkStatus.Available>(status)
        // WiFi with 50Mbps should be Excellent, Cellular with 1Mbps should be Fair
        // Composite picks best quality
        assertEquals(NetworkType.WiFi, status.info.type)
        composite.close()
    }

    @Test
    fun plusOperatorCombinesTwoMonitors() {
        val m1 = FakeNetworkMonitor(initialOnline = true)
        val m2 = FakeNetworkMonitor(initialOnline = false)
        val combined = m1 + m2

        assertIs<CompositeNetworkMonitor>(combined)
        assertTrue(combined.isOnline.value)
        combined.close()
    }

    @Test
    fun closeClosesAllConstituents() {
        val m1 = FakeNetworkMonitor()
        val m2 = FakeNetworkMonitor()
        val composite = CompositeNetworkMonitor(listOf(m1, m2))

        composite.close()
        assertTrue(m1.isClosed)
        assertTrue(m2.isClosed)
    }

    @Test
    fun returnsUnavailableWhenAllMonitorsUnavailable() {
        val m1 = FakeNetworkMonitor(initialOnline = false)
        val m2 = FakeNetworkMonitor(initialOnline = false)
        val composite = CompositeNetworkMonitor(listOf(m1, m2))

        assertEquals(NetworkStatus.Unavailable, composite.networkStatus.value)
        composite.close()
    }
}
