package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NetworkMonitorExtensionsTest {

    @Test
    fun requireOnlineSucceedsWhenOnline() {
        val monitor = FakeNetworkMonitor()
        monitor.requireOnline() // should not throw
    }

    @Test
    fun requireOnlineThrowsWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        assertFailsWith<NetworkUnavailableException> {
            monitor.requireOnline()
        }
    }

    @Test
    fun ensureOnlineReturnsImmediatelyWhenOnline() = runTest {
        val monitor = FakeNetworkMonitor()
        monitor.ensureOnline() // should return without suspending
    }

    @Test
    fun ensureOnlineSuspendsUntilOnline() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        var resumed = false
        val job = launch {
            monitor.ensureOnline()
            resumed = true
        }
        // Still suspended
        assertFalse(resumed)
        // Go online
        monitor.setOnline(true)
        job.join()
        assertTrue(resumed)
    }

    @Test
    fun withNetworkGuardExecutesBlock() = runTest {
        val monitor = FakeNetworkMonitor()
        val result = monitor.withNetworkGuard { 42 }
        assertEquals(42, result)
    }

    @Test
    fun awaitOnlineReturnsInfoWhenOnline() = runTest {
        val monitor = FakeNetworkMonitor()
        val info = monitor.awaitOnline()
        assertEquals(NetworkType.Unknown, info.type)
    }

    @Test
    fun awaitOnlineSuspendsUntilOnline() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        var info: NetworkInfo? = null
        val job = launch {
            info = monitor.awaitOnline()
        }
        monitor.setOnline(true)
        job.join()
        assertEquals(NetworkType.Unknown, info?.type)
    }

    @Test
    fun isConnectedViaReturnsTrueForMatchingType() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi)),
        )
        assertTrue(monitor.isConnectedVia(NetworkType.WiFi))
    }

    @Test
    fun isConnectedViaReturnsFalseForMismatchedType() {
        val monitor = FakeNetworkMonitor()
        monitor.setNetworkStatus(
            NetworkStatus.Available(NetworkInfo(type = NetworkType.WiFi)),
        )
        assertFalse(monitor.isConnectedVia(NetworkType.Cellular))
    }

    @Test
    fun isConnectedViaReturnsFalseWhenOffline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        assertFalse(monitor.isConnectedVia(NetworkType.WiFi))
    }

    @Test
    fun onlyWhileOnlineCompletesWhenOffline() = runTest {
        val monitor = FakeNetworkMonitor()
        val collected = mutableListOf<NetworkStatus>()
        val job = launch {
            monitor.onlyWhileOnline().collect { collected.add(it) }
        }
        yield()
        monitor.setOnline(false)
        job.join()
        assertTrue(collected.isNotEmpty())
        collected.forEach { assertIs<NetworkStatus.Available>(it) }
    }
}
