package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeNetworkMonitorEnhancedTest {

    @Test
    fun statusHistoryTracksChanges() {
        val monitor = FakeNetworkMonitor()
        monitor.setOnline(false)
        monitor.setOnline(true)
        // Initial + 2 changes
        assertTrue(monitor.statusHistory.size >= 3)
    }

    @Test
    fun eventHistoryTracksEvents() {
        val monitor = FakeNetworkMonitor()
        monitor.setOnline(false)
        monitor.setOnline(true)
        assertEquals(2, monitor.eventHistory.size)
        assertIs<NetworkChangeEvent.Disconnected>(monitor.eventHistory[0])
        assertIs<NetworkChangeEvent.Connected>(monitor.eventHistory[1])
    }

    @Test
    fun simulateHandoffProducesEvents() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi)),
        )
        monitor.simulateHandoff(from = NetworkType.WiFi, to = NetworkType.Cellular)
        // Should have gone offline then back online with Cellular
        assertTrue(monitor.isOnline.value)
        val status = monitor.networkStatus.value
        assertIs<NetworkStatus.Available>(status)
        assertEquals(NetworkType.Cellular, status.info.type)
    }

    @Test
    fun simulateFlickerGoesOfflineThenOnline() {
        val monitor = FakeNetworkMonitor()
        val wifiInfo = NetworkInfo(type = NetworkType.WiFi, downstreamBandwidthKbps = 50_000)
        monitor.simulateFlicker(wifiInfo)
        assertTrue(monitor.isOnline.value)
        val status = monitor.networkStatus.value
        assertIs<NetworkStatus.Available>(status)
        assertEquals(NetworkType.WiFi, status.info.type)
    }

    @Test
    fun resetStateClearsEverything() {
        val monitor = FakeNetworkMonitor()
        monitor.setOnline(false)
        monitor.setOnline(true)
        monitor.close()
        monitor.resetState()
        assertFalse(monitor.isClosed)
        assertEquals(0, monitor.updateCount)
        assertTrue(monitor.eventHistory.isEmpty())
        assertEquals(1, monitor.statusHistory.size) // only initial
    }

    @Test
    fun updateCountIncrements() {
        val monitor = FakeNetworkMonitor()
        assertEquals(0, monitor.updateCount)
        monitor.setOnline(false)
        assertEquals(1, monitor.updateCount)
        monitor.setOnline(true)
        assertEquals(2, monitor.updateCount)
    }

    @Test
    fun setNetworkStatusTracksHistory() {
        val monitor = FakeNetworkMonitor()
        val wifiStatus = NetworkStatus.Available(
            NetworkInfo(type = NetworkType.WiFi),
        )
        monitor.setNetworkStatus(wifiStatus)
        assertTrue(monitor.statusHistory.last() == wifiStatus)
    }
}
