package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClipboardHistoryTest {

    @Test
    fun createHistory_returnsInstance() {
        val history = createClipboardHistory()
        assertNotNull(history)
    }

    @Test
    fun createHistory_defaultMaxSize() {
        val history = createClipboardHistory()
        assertEquals(20, history.maxSize)
    }

    @Test
    fun createHistory_customMaxSize() {
        val history = createClipboardHistory(maxSize = 50)
        assertEquals(50, history.maxSize)
    }

    @Test
    fun history_initiallyEmpty() {
        val history = createClipboardHistory()
        assertTrue(history.entries.value.isEmpty())
    }

    @Test
    fun history_initiallyNotCapturing() {
        val history = createClipboardHistory()
        assertFalse(history.isCapturing)
    }

    @Test
    fun history_startCapturing() {
        val history = createClipboardHistory()
        history.startCapturing()
        assertTrue(history.isCapturing)
        history.stopCapturing()
    }

    @Test
    fun history_stopCapturing() {
        val history = createClipboardHistory()
        history.startCapturing()
        history.stopCapturing()
        assertFalse(history.isCapturing)
    }

    @Test
    fun history_doubleStartIsIdempotent() {
        val history = createClipboardHistory()
        history.startCapturing()
        history.startCapturing() // should not throw
        assertTrue(history.isCapturing)
        history.stopCapturing()
    }

    @Test
    fun history_doubleStopIsIdempotent() {
        val history = createClipboardHistory()
        history.startCapturing()
        history.stopCapturing()
        history.stopCapturing() // should not throw
        assertFalse(history.isCapturing)
    }

    @Test
    fun history_clear() {
        val history = createClipboardHistory()
        history.clear()
        assertTrue(history.entries.value.isEmpty())
    }

    @Test
    fun historyEntry_creation() {
        val entry = ClipboardHistoryEntry(
            content = "Hello",
            timestamp = 1000L,
            contentType = ClipboardContentType.Text
        )
        assertEquals("Hello", entry.content)
        assertEquals(1000L, entry.timestamp)
        assertEquals(ClipboardContentType.Text, entry.contentType)
    }

    @Test
    fun historyEntry_uriContentType() {
        val entry = ClipboardHistoryEntry(
            content = "https://example.com",
            timestamp = 2000L,
            contentType = ClipboardContentType.Uri
        )
        assertEquals(ClipboardContentType.Uri, entry.contentType)
    }

    @Test
    fun historyEntry_defaultContentType() {
        val entry = ClipboardHistoryEntry(
            content = "text",
            timestamp = 1000L
        )
        assertEquals(ClipboardContentType.Text, entry.contentType)
    }

    @Test
    fun history_copyToClipboard_doesNotThrow() {
        val history = createClipboardHistory()
        val entry = ClipboardHistoryEntry(
            content = "Test copy",
            timestamp = 1000L
        )
        // Should not throw on any platform
        history.copyToClipboard(entry)
    }

    @Test
    fun history_remove_doesNotThrow() {
        val history = createClipboardHistory()
        val entry = ClipboardHistoryEntry(
            content = "Test remove",
            timestamp = 1000L
        )
        // Remove from empty history — should not throw
        history.remove(entry)
    }
}
