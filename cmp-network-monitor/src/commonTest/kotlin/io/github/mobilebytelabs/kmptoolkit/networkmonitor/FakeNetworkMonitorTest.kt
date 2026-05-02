package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeNetworkMonitorTest {

    @Test
    fun defaultIsOnline() {
        val monitor = FakeNetworkMonitor()
        assertTrue(monitor.isOnline.value)
        assertIs<NetworkStatus.Available>(monitor.networkStatus.value)
    }

    @Test
    fun canStartOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        assertFalse(monitor.isOnline.value)
        assertIs<NetworkStatus.Unavailable>(monitor.networkStatus.value)
    }

    @Test
    fun setOnlineUpdatesState() {
        val monitor = FakeNetworkMonitor()
        monitor.setOnline(false)
        assertFalse(monitor.isOnline.value)
        assertIs<NetworkStatus.Unavailable>(monitor.networkStatus.value)
    }

    @Test
    fun setOnlineEmitsDisconnectedEvent() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.networkChanges.test {
            monitor.setOnline(false)
            val event = awaitItem()
            assertIs<NetworkChangeEvent.Disconnected>(event)
        }
    }

    @Test
    fun setOnlineEmitsConnectedEvent() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        monitor.networkChanges.test {
            monitor.setOnline(true)
            val event = awaitItem()
            assertIs<NetworkChangeEvent.Connected>(event)
        }
    }

    @Test
    fun setOnlineSameStateDoesNotEmit() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        monitor.networkChanges.test {
            monitor.setOnline(true) // already online
            expectNoEvents()
        }
    }

    @Test
    fun setNetworkStatusUpdatesIsOnline() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(NetworkStatus.Unavailable)
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun setNetworkStatusEmitsTypeChangedEvent() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.networkChanges.test {
            val cellularStatus = NetworkStatus.Available(
                NetworkInfo(type = NetworkType.Cellular),
            )
            monitor.setNetworkStatus(cellularStatus)
            val event = awaitItem()
            assertIs<NetworkChangeEvent.TypeChanged>(event)
            assertEquals(NetworkType.Unknown, event.from)
            assertEquals(NetworkType.Cellular, event.to)
        }
    }

    @Test
    fun setNetworkStatusEmitsMeteredChangedEvent() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.networkChanges.test {
            val meteredStatus = NetworkStatus.Available(
                NetworkInfo(isMetered = true),
            )
            monitor.setNetworkStatus(meteredStatus)
            val event = awaitItem()
            assertIs<NetworkChangeEvent.MeteredChanged>(event)
            assertEquals(true, event.isMetered)
        }
    }

    @Test
    fun closeMarksAsClosed() {
        val monitor = FakeNetworkMonitor()
        assertFalse(monitor.isClosed)
        monitor.close()
        assertTrue(monitor.isClosed)
    }

    @Test
    fun currentStatusReflectsNetworkStatus() {
        val monitor = FakeNetworkMonitor()
        assertEquals(monitor.networkStatus.value, monitor.currentStatus)
        monitor.setOnline(false)
        assertEquals(NetworkStatus.Unavailable, monitor.currentStatus)
    }

    @Test
    fun customInitialStatus() {
        val info = NetworkInfo(type = NetworkType.Ethernet, isMetered = false)
        val monitor = FakeNetworkMonitor(
            initialOnline = true,
            initialStatus = NetworkStatus.Available(info),
        )
        val status = monitor.networkStatus.value
        assertIs<NetworkStatus.Available>(status)
        assertEquals(NetworkType.Ethernet, status.info.type)
    }
}
