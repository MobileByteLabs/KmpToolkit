package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkConstraintTest {

    private val wifiOnline = NetworkStatus.Available(
        NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 10_000),
    )

    private val cellularOnline = NetworkStatus.Available(
        NetworkInfo(type = NetworkType.Cellular, isMetered = true, downstreamBandwidthKbps = 5_000),
    )

    @Test
    fun requiresConnectivitySatisfiedWhenOnline() {
        assertTrue(NetworkConstraint.RequiresConnectivity.isSatisfiedBy(wifiOnline))
    }

    @Test
    fun requiresConnectivityNotSatisfiedWhenOffline() {
        assertFalse(NetworkConstraint.RequiresConnectivity.isSatisfiedBy(NetworkStatus.Unavailable))
    }

    @Test
    fun requiresWifiSatisfiedOnWifi() {
        assertTrue(NetworkConstraint.RequiresWiFi.isSatisfiedBy(wifiOnline))
    }

    @Test
    fun requiresWifiNotSatisfiedOnCellular() {
        assertFalse(NetworkConstraint.RequiresWiFi.isSatisfiedBy(cellularOnline))
    }

    @Test
    fun requiresUnmeteredSatisfiedOnUnmetered() {
        assertTrue(NetworkConstraint.RequiresUnmetered.isSatisfiedBy(wifiOnline))
    }

    @Test
    fun requiresUnmeteredNotSatisfiedOnMetered() {
        assertFalse(NetworkConstraint.RequiresUnmetered.isSatisfiedBy(cellularOnline))
    }

    @Test
    fun requiresMinBandwidthSatisfied() {
        assertTrue(NetworkConstraint.RequiresMinBandwidth(5_000).isSatisfiedBy(wifiOnline))
    }

    @Test
    fun requiresMinBandwidthNotSatisfied() {
        assertFalse(NetworkConstraint.RequiresMinBandwidth(20_000).isSatisfiedBy(wifiOnline))
    }

    @Test
    fun executeWithConstraintSucceeds() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(wifiOnline)
        val result = monitor.executeWithConstraint(NetworkConstraint.RequiresWiFi) { 42 }
        assertTrue(result == 42)
    }

    @Test
    fun executeWithConstraintThrowsWhenNotMet() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(cellularOnline)
        assertFailsWith<NetworkUnavailableException> {
            monitor.executeWithConstraint(NetworkConstraint.RequiresWiFi) { 42 }
        }
    }

    @Test
    fun checkConstraintsMultiple() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(wifiOnline)
        assertTrue(
            monitor.checkConstraints(
                NetworkConstraint.RequiresConnectivity,
                NetworkConstraint.RequiresWiFi,
                NetworkConstraint.RequiresUnmetered,
            ),
        )
    }

    @Test
    fun checkConstraintsFailsIfAnyNotMet() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(cellularOnline)
        assertFalse(
            monitor.checkConstraints(
                NetworkConstraint.RequiresConnectivity,
                NetworkConstraint.RequiresWiFi,
            ),
        )
    }
}
