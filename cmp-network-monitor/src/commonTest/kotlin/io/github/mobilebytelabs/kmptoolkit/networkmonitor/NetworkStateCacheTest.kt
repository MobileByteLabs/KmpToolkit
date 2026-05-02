package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkStateCacheTest {

    @Test
    fun fromAvailableStatusCreatesCache() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
        val status = NetworkStatus.Available(info)
        val cached = CachedNetworkState.from(status)

        assertNotNull(cached)
        assertTrue(cached.wasOnline)
        assertEquals("WiFi", cached.networkTypeName)
        assertEquals(false, cached.isMetered)
        assertTrue(cached.timestampMs > 0)
    }

    @Test
    fun fromUnavailableStatusCreatesOfflineCache() {
        val cached = CachedNetworkState.from(NetworkStatus.Unavailable)

        assertNotNull(cached)
        assertEquals(false, cached.wasOnline)
        assertEquals("Unknown", cached.networkTypeName)
    }

    @Test
    fun fromCaptivePortalCreatesCache() {
        val info = NetworkInfo(type = NetworkType.WiFi)
        val cached = CachedNetworkState.from(NetworkStatus.CaptivePortal(info, "https://login.example.com"))

        assertNotNull(cached)
        assertEquals(false, cached.wasOnline) // captive portal = not online
        assertEquals("WiFi", cached.networkTypeName)
    }

    @Test
    fun toNetworkInfoRestoresType() {
        val cached = CachedNetworkState(
            wasOnline = true,
            networkTypeName = "Cellular",
            isMetered = true,
            timestampMs = 1000L,
        )
        val info = cached.toNetworkInfo()

        assertEquals(NetworkType.Cellular, info.type)
        assertEquals(true, info.isMetered)
    }

    @Test
    fun toNetworkInfoHandlesUnknownTypeName() {
        val cached = CachedNetworkState(
            wasOnline = true,
            networkTypeName = "SomeNewType",
            isMetered = false,
            timestampMs = 1000L,
        )
        val info = cached.toNetworkInfo()

        assertEquals(NetworkType.Unknown, info.type)
    }

    @Test
    fun maxAgeIsReasonable() {
        // Should be 5 minutes = 300_000ms
        assertEquals(300_000L, CachedNetworkState.MAX_AGE_MS)
    }
}
