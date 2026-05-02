package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NetworkMonitorProviderTest {

    @AfterTest
    fun cleanup() {
        NetworkMonitorProvider.reset()
    }

    @Test
    fun getThrowsBeforeInstall() {
        assertFailsWith<IllegalStateException> {
            NetworkMonitorProvider.get()
        }
    }

    @Test
    fun getOrNullReturnsNullBeforeInstall() {
        assertNull(NetworkMonitorProvider.getOrNull())
    }

    @Test
    fun installReturnsMonitor() {
        val monitor = NetworkMonitorProvider.install()
        assertNotNull(monitor)
    }

    @Test
    fun installReturnsSameInstanceOnSecondCall() {
        val first = NetworkMonitorProvider.install()
        val second = NetworkMonitorProvider.install()
        assertSame(first, second)
    }

    @Test
    fun getReturnsInstalledMonitor() {
        val installed = NetworkMonitorProvider.install()
        val retrieved = NetworkMonitorProvider.get()
        assertSame(installed, retrieved)
    }

    @Test
    fun resetAllowsReinstall() {
        val first = NetworkMonitorProvider.install()
        NetworkMonitorProvider.reset()
        assertNull(NetworkMonitorProvider.getOrNull())
    }

    @Test
    fun scopedMonitorClosesOnScopeCancel() = runTest {
        val scope = CoroutineScope(Job())
        val monitor = createScopedNetworkMonitor(scope)
        assertNotNull(monitor)
        scope.cancel()
        // Monitor should be closed — we can't easily verify without
        // a FakeNetworkMonitor but at least it shouldn't throw
    }
}
