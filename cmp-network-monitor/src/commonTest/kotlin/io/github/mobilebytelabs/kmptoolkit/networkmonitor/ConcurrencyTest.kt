package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConcurrencyTest {

    @Test
    fun multipleCollectorsReceiveUpdates() = runTest {
        val monitor = FakeNetworkMonitor()
        val results = mutableListOf<Boolean>()

        val collectors = (1..5).map {
            async {
                monitor.isOnline.value
            }
        }

        val values = collectors.awaitAll()
        assertTrue(values.all { it })
    }

    @Test
    fun rapidStateChangesAreConsistent() = runTest {
        val monitor = FakeNetworkMonitor()

        repeat(100) { i ->
            monitor.setOnline(i % 2 == 0)
        }

        // After 100 toggles (0-99), last is i=99, 99%2==1 → setOnline(false)
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun concurrentStatusReadsAreSafe() = runTest {
        val monitor = FakeNetworkMonitor()
        val wifiStatus = NetworkStatus.Available(
            NetworkInfo(type = NetworkType.WiFi, downstreamBandwidthKbps = 50_000),
        )
        monitor.setNetworkStatus(wifiStatus)

        val jobs = (1..10).map {
            launch {
                repeat(50) {
                    monitor.networkStatus.value
                    monitor.isOnline.value
                    monitor.currentStatus
                    monitor.currentQuality
                    yield()
                }
            }
        }
        jobs.forEach { it.join() }
        // No crash = pass
    }

    @Test
    fun providerConcurrentAccess() = runTest {
        NetworkMonitorProvider.reset()
        val monitor = NetworkMonitorProvider.install()

        val jobs = (1..10).map {
            launch {
                repeat(50) {
                    val retrieved = NetworkMonitorProvider.get()
                    assertTrue(retrieved === monitor)
                    yield()
                }
            }
        }
        jobs.forEach { it.join() }
        NetworkMonitorProvider.reset()
    }

    @Test
    fun constraintCheckUnderRapidChanges() = runTest {
        val monitor = FakeNetworkMonitor()

        repeat(50) { i ->
            val status = if (i % 2 == 0) {
                NetworkStatus.Available(
                    NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 10_000),
                )
            } else {
                NetworkStatus.Unavailable
            }
            monitor.setNetworkStatus(status)

            // Constraint check should never crash
            monitor.checkConstraints(
                NetworkConstraint.RequiresConnectivity,
                NetworkConstraint.RequiresMinBandwidth(5_000),
            )
        }
    }
}
