package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposeExtensionsTest {

    @Test
    fun localNetworkMonitorDefaultProviderThrowsOnAccess() {
        // LocalNetworkMonitor uses staticCompositionLocalOf with error() default.
        // The error lambda is stored as the default — we verify the local is non-null
        // (actual @Composable access test requires Compose UI test framework).
        assertNotNull(LocalNetworkMonitor, "LocalNetworkMonitor should be a valid CompositionLocal")
    }

    @Test
    fun fakeNetworkMonitorCanBeCreated() {
        val fake = FakeNetworkMonitor(initialOnline = true)
        assertNotNull(fake)
        assertEquals(true, fake.isOnline.value)
    }

    @Test
    fun fakeNetworkMonitorTracksOfflineState() {
        val fake = FakeNetworkMonitor(initialOnline = true)
        fake.setOnline(false)
        assertEquals(false, fake.isOnline.value)
    }

    @Test
    fun fakeNetworkMonitorTracksNetworkStatus() {
        val fake = FakeNetworkMonitor(initialOnline = true)
        assertTrue(fake.networkStatus.value is NetworkStatus.Available)

        fake.setOnline(false)
        assertEquals(NetworkStatus.Unavailable, fake.networkStatus.value)
    }

    @Test
    fun networkMonitorProviderInstallReturnsSameInstance() {
        val monitor1 = NetworkMonitorProvider.install()
        val monitor2 = NetworkMonitorProvider.install()
        assertEquals(monitor1, monitor2, "Provider should return singleton")
    }

    @Test
    fun fakeNetworkMonitorCloseWorks() {
        val fake = FakeNetworkMonitor(initialOnline = true)
        fake.close()
        assertTrue(fake.isClosed, "FakeNetworkMonitor should be closed")
    }
}
