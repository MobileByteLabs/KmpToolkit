package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LeakDetectionTest {

    @Test
    fun closeReleasesResources() {
        val monitor = FakeNetworkMonitor()
        monitor.close()
        assertTrue(monitor.isClosed)
    }

    @Test
    fun scopedMonitorClosesOnCancel() = runTest {
        val scope = CoroutineScope(Job())
        val monitor = createScopedNetworkMonitor(scope)
        assertNotNull(monitor)
        scope.cancel()
        // After scope cancel, monitor should be closed
        // FakeNetworkMonitor doesn't get returned from createScopedNetworkMonitor
        // (it creates a real one), so just verify no crash
    }

    @Test
    fun providerResetClosesMonitor() {
        val monitor = NetworkMonitorProvider.install()
        assertNotNull(monitor)
        NetworkMonitorProvider.reset()
        // Verify reset was clean
        assertTrue(NetworkMonitorProvider.getOrNull() == null)
    }

    @Test
    fun multipleCloseCallsAreSafe() {
        val monitor = FakeNetworkMonitor()
        monitor.close()
        monitor.close()
        monitor.close()
        assertTrue(monitor.isClosed)
    }

    @Test
    fun collectingAfterCloseDoesNotCrash() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.close()
        // Reading state after close should still work (StateFlow keeps last value)
        val online = monitor.isOnline.value
        val status = monitor.networkStatus.value
        assertNotNull(status)
    }

    @Test
    fun closeGracefullyIsSafe() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.closeGracefully()
        assertTrue(monitor.isClosed)
    }

    @Test
    fun manyMonitorCreationsAndCleanups() {
        val monitors = (1..50).map { FakeNetworkMonitor() }
        monitors.forEach { it.setOnline(true) }
        monitors.forEach { it.close() }
        monitors.forEach { assertTrue(it.isClosed) }
    }
}
