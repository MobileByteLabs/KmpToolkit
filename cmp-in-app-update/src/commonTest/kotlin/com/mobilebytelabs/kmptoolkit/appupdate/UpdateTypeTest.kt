package com.mobilebytelabs.kmptoolkit.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateTypeTest {

    @Test
    fun noneExists() {
        assertEquals("NONE", UpdateType.NONE.name)
    }

    @Test
    fun flexibleExists() {
        assertEquals("FLEXIBLE", UpdateType.FLEXIBLE.name)
    }

    @Test
    fun immediateExists() {
        assertEquals("IMMEDIATE", UpdateType.IMMEDIATE.name)
    }

    @Test
    fun enumValuesCount() {
        assertEquals(3, UpdateType.entries.size)
    }

    @Test
    fun enumValuesContainsAll() {
        val types = UpdateType.entries
        assertTrue(types.contains(UpdateType.NONE))
        assertTrue(types.contains(UpdateType.FLEXIBLE))
        assertTrue(types.contains(UpdateType.IMMEDIATE))
    }

    @Test
    fun valueOfNone() {
        assertEquals(UpdateType.NONE, UpdateType.valueOf("NONE"))
    }

    @Test
    fun valueOfFlexible() {
        assertEquals(UpdateType.FLEXIBLE, UpdateType.valueOf("FLEXIBLE"))
    }

    @Test
    fun valueOfImmediate() {
        assertEquals(UpdateType.IMMEDIATE, UpdateType.valueOf("IMMEDIATE"))
    }
}
