package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateNetworkMonitorSmokeTest {

    @Test
    fun createNetworkMonitorReturnsNonNull() {
        val monitor = createNetworkMonitor()
        assertNotNull(monitor)
        monitor.close()
    }

    @Test
    fun createNetworkMonitorWithDefaultConfig() {
        val monitor = createNetworkMonitor(NetworkMonitorConfig())
        assertNotNull(monitor)
        monitor.close()
    }

    @Test
    fun createdMonitorHasValidInitialState() {
        val monitor = createNetworkMonitor()
        // Should have a valid status (either Available or Unavailable)
        val status = monitor.networkStatus.value
        assertTrue(
            status is NetworkStatus.Available || status is NetworkStatus.Unavailable,
        )
        monitor.close()
    }

    @Test
    fun createdMonitorIsOnlineConsistentWithStatus() {
        val monitor = createNetworkMonitor()
        val isOnline = monitor.isOnline.value
        val status = monitor.networkStatus.value
        if (isOnline) {
            assertIs<NetworkStatus.Available>(status)
        } else {
            assertIs<NetworkStatus.Unavailable>(status)
        }
        monitor.close()
    }
}
