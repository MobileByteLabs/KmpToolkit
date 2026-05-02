package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NetworkTypeTest {

    @Test
    fun allEnumValuesExist() {
        val types = NetworkType.entries
        assertEquals(7, types.size)
    }

    @Test
    fun enumNamesMatchExpected() {
        val names = NetworkType.entries.map { it.name }
        assertEquals(
            listOf("WiFi", "Cellular", "FiveG", "Ethernet", "VPN", "Bluetooth", "Unknown"),
            names,
        )
    }

    @Test
    fun enumValuesAreDistinct() {
        val types = NetworkType.entries
        assertEquals(types.size, types.toSet().size)
    }

    @Test
    fun valueOfRoundTrips() {
        NetworkType.entries.forEach { type ->
            assertEquals(type, NetworkType.valueOf(type.name))
        }
    }
}
