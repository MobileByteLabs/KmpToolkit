package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkChangeEventTest {

    @Test
    fun connectedCarriesInfo() {
        val info = NetworkInfo(type = NetworkType.WiFi)
        val event = NetworkChangeEvent.Connected(info)
        assertIs<NetworkChangeEvent.Connected>(event)
        assertEquals(NetworkType.WiFi, event.info.type)
    }

    @Test
    fun disconnectedIsSingleton() {
        val a = NetworkChangeEvent.Disconnected
        val b = NetworkChangeEvent.Disconnected
        assertEquals(a, b)
    }

    @Test
    fun typeChangedCarriesBothTypes() {
        val event = NetworkChangeEvent.TypeChanged(
            from = NetworkType.WiFi,
            to = NetworkType.Cellular,
        )
        assertEquals(NetworkType.WiFi, event.from)
        assertEquals(NetworkType.Cellular, event.to)
    }

    @Test
    fun meteredChangedCarriesFlag() {
        val event = NetworkChangeEvent.MeteredChanged(isMetered = true)
        assertEquals(true, event.isMetered)
    }

    @Test
    fun connectedEquality() {
        val info = NetworkInfo(type = NetworkType.Ethernet)
        val a = NetworkChangeEvent.Connected(info)
        val b = NetworkChangeEvent.Connected(info)
        assertEquals(a, b)
    }
}
