package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NetworkStatusTest {

    @Test
    fun availableIsOnline() {
        val status = NetworkStatus.Available(NetworkInfo())
        assertTrue(status.isOnline)
    }

    @Test
    fun unavailableIsOffline() {
        val status = NetworkStatus.Unavailable
        assertFalse(status.isOnline)
    }

    @Test
    fun availableCarriesNetworkInfo() {
        val info = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
        val status = NetworkStatus.Available(info)
        assertIs<NetworkStatus.Available>(status)
        assertEquals(NetworkType.WiFi, status.info.type)
        assertFalse(status.info.isMetered)
    }

    @Test
    fun unavailableIsSingleton() {
        val a = NetworkStatus.Unavailable
        val b = NetworkStatus.Unavailable
        assertEquals(a, b)
    }

    @Test
    fun availableEqualityBasedOnInfo() {
        val info = NetworkInfo(type = NetworkType.Cellular, isMetered = true)
        val a = NetworkStatus.Available(info)
        val b = NetworkStatus.Available(info)
        assertEquals(a, b)
    }

    @Test
    fun availableAndUnavailableAreNotEqual() {
        val available = NetworkStatus.Available(NetworkInfo())
        val unavailable = NetworkStatus.Unavailable
        assertFalse(available == unavailable)
    }
}
