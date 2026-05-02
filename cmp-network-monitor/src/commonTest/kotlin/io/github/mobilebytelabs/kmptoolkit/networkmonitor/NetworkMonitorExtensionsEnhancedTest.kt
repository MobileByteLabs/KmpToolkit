package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NetworkMonitorExtensionsEnhancedTest {

    @Test
    fun withNetworkStateCombinesDataAndStatus() = runTest {
        val monitor = FakeNetworkMonitor()
        val dataFlow = flowOf("hello")

        dataFlow.withNetworkState(monitor).test {
            val first = awaitItem()
            assertEquals("hello", first.first)
            assertTrue(first.second.isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onlyWhileOnlineFiltersOfflineValues() = runTest {
        val monitor = FakeNetworkMonitor()
        val dataFlow = flowOf(1, 2, 3)

        dataFlow.onlyWhileOnline(monitor).test {
            val first = awaitItem()
            assertTrue(first in 1..3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun awaitConnectionTypeSuspendsUntilType() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = NetworkType.Cellular)),
        )

        var result: NetworkInfo? = null
        val job = launch {
            result = monitor.awaitConnectionType(NetworkType.WiFi)
        }

        // Switch to WiFi
        monitor.setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi)),
        )
        job.join()

        assertEquals(NetworkType.WiFi, result?.type)
    }

    @Test
    fun closeGracefullyClosesMonitor() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.closeGracefully()
        assertTrue(monitor.isClosed)
    }

    @Test
    fun networkQualityFlowEmitsDistinctValues() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(
                NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 50_000),
            ),
        )

        monitor.networkQuality().test {
            val first = awaitItem()
            assertEquals(NetworkQuality.Excellent, first)

            // Change to poor
            monitor.setNetworkStatus(
                NetworkStatus.Available(
                    NetworkInfo(type = NetworkType.Unknown, downstreamBandwidthKbps = 500),
                ),
            )
            val second = awaitItem()
            assertEquals(NetworkQuality.Poor, second)

            cancel()
        }
    }

    @Test
    fun currentQualityReflectsStatus() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(
                NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 50_000),
            ),
        )
        assertEquals(NetworkQuality.Excellent, monitor.currentQuality)

        monitor.setOnline(false)
        assertEquals(NetworkQuality.Offline, monitor.currentQuality)
    }
}
