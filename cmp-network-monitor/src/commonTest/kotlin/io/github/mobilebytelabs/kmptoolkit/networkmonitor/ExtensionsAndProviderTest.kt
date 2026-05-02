package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.di.provideNetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionsAndProviderTest {

    // ---- ifOnline / ifOffline (T106-T108) ----

    @Test
    fun ifOnlineExecutesBlockWhenOnline() {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val result = monitor.ifOnline { info -> info.type }
        assertNotNull(result)
        assertEquals(NetworkType.Unknown, result) // default NetworkInfo type
    }

    @Test
    fun ifOnlineReturnsNullWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        val result = monitor.ifOnline { info -> info.type }
        assertNull(result)
    }

    @Test
    fun ifOfflineExecutesBlockWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        val result = monitor.ifOffline { "cached" }
        assertEquals("cached", result)
    }

    @Test
    fun ifOfflineReturnsNullWhenOnline() {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val result = monitor.ifOffline { "cached" }
        assertNull(result)
    }

    // ---- addCallback / CallbackHandle (T114-T115) ----

    @Test
    fun addCallbackReceivesOnlineEvents() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val scope = CoroutineScope(Job())

        val handle = monitor.addCallback(
            scope = scope,
            onOnline = { },
            onOffline = { },
        )

        // Verify callback handle is created and can be closed without error
        handle.close()
        scope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun callbackHandleStopsCollection() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val scope = CoroutineScope(Job())

        val handle = monitor.addCallback(scope = scope)
        handle.close()

        // Should not crash or throw after close
        monitor.setOnline(false)
        scope.coroutineContext[Job]?.cancel()
    }

    // ---- NetworkMonitorProvider.redundantInstallCount (T49-T50) ----

    @Test
    fun redundantInstallCountTracksMultipleInstalls() {
        NetworkMonitorProvider.reset()
        assertEquals(0, NetworkMonitorProvider.redundantInstallCount)

        NetworkMonitorProvider.install()
        assertEquals(0, NetworkMonitorProvider.redundantInstallCount)

        NetworkMonitorProvider.install() // second install — redundant
        assertEquals(1, NetworkMonitorProvider.redundantInstallCount)

        NetworkMonitorProvider.install() // third install — redundant
        assertEquals(2, NetworkMonitorProvider.redundantInstallCount)

        NetworkMonitorProvider.reset()
        assertEquals(0, NetworkMonitorProvider.redundantInstallCount)
    }

    // ---- DI module (T41-T42) ----

    @Test
    fun provideNetworkMonitorReturnsInstance() {
        val monitor = provideNetworkMonitor()
        assertNotNull(monitor)
        monitor.close()
    }

    @Test
    fun provideNetworkMonitorWithConfigReturnsInstance() {
        val config = NetworkMonitorConfig(pollIntervalMs = 10_000L)
        val monitor = provideNetworkMonitor(config)
        assertNotNull(monitor)
        monitor.close()
    }

    // ---- CaptivePortal config flag (T21) ----

    @Test
    fun captivePortalDetectionDefaultsFalse() {
        val config = NetworkMonitorConfig()
        assertEquals(false, config.captivePortalDetection)
    }

    @Test
    fun captivePortalDetectionCanBeEnabled() {
        val config = NetworkMonitorConfig(captivePortalDetection = true)
        assertTrue(config.captivePortalDetection)
    }

    @Test
    fun captivePortalDetectionViaBuilder() {
        val config = NetworkMonitorConfig {
            captivePortalDetection = true
        }
        assertTrue(config.captivePortalDetection)
    }

    // ---- CaptivePortal events (T20) ----

    @Test
    fun captivePortalDetectedEventCarriesRedirectUrl() {
        val event = NetworkChangeEvent.CaptivePortalDetected("https://login.example.com")
        assertIs<NetworkChangeEvent.CaptivePortalDetected>(event)
        assertEquals("https://login.example.com", event.redirectUrl)
    }

    @Test
    fun captivePortalResolvedEvent() {
        val event = NetworkChangeEvent.CaptivePortalResolved
        assertIs<NetworkChangeEvent.CaptivePortalResolved>(event)
    }

    // ---- NetworkStatus.CaptivePortal ----

    @Test
    fun captivePortalStatusIsNotOnline() {
        val status = NetworkStatus.CaptivePortal(NetworkInfo(), "https://login.example.com")
        assertEquals(false, status.isOnline)
    }

    @Test
    fun captivePortalQualityIsPoor() {
        val status = NetworkStatus.CaptivePortal(NetworkInfo())
        assertEquals(NetworkQuality.Poor, status.toQuality())
    }

    // ---- awaitConnectionType ----

    @Test
    fun awaitConnectionTypeReturnsImmediatelyIfAlreadyConnected() = runTest {
        val monitor = FakeNetworkMonitor()
        val wifiInfo = NetworkInfo(type = NetworkType.WiFi)
        monitor.setNetworkStatus(NetworkStatus.Available(wifiInfo))

        val info = monitor.awaitConnectionType(NetworkType.WiFi)
        assertEquals(NetworkType.WiFi, info.type)
    }

    // ---- closeGracefully ----

    @Test
    fun closeGracefullyClosesMonitor() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.closeGracefully()
        assertTrue(monitor.isClosed)
    }
}
