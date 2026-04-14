package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardChangeTest {

    @Test
    fun create_withDefaults() {
        val change = ClipboardChange(
            content = "Hello",
            timestamp = 1000L,
        )
        assertEquals("Hello", change.content)
        assertEquals(ClipboardContentType.Text, change.contentType)
        assertEquals(1000L, change.timestamp)
        assertEquals(ClipboardSource.Unknown, change.source)
        assertNull(change.previousContent)
    }

    @Test
    fun create_withAllFields() {
        val change = ClipboardChange(
            content = "https://instagram.com/reel/123",
            contentType = ClipboardContentType.Uri,
            timestamp = 2000L,
            source = ClipboardSource.External,
            previousContent = "old text",
        )
        assertEquals(ClipboardContentType.Uri, change.contentType)
        assertEquals(ClipboardSource.External, change.source)
        assertEquals("old text", change.previousContent)
    }

    @Test
    fun isUrl_withHttps() {
        val change = ClipboardChange(
            content = "https://example.com",
            timestamp = 1000L,
        )
        assertTrue(change.isUrl())
    }

    @Test
    fun isUrl_withHttp() {
        val change = ClipboardChange(
            content = "http://example.com",
            timestamp = 1000L,
        )
        assertTrue(change.isUrl())
    }

    @Test
    fun isUrl_withUriContentType() {
        val change = ClipboardChange(
            content = "content://media/photo",
            contentType = ClipboardContentType.Uri,
            timestamp = 1000L,
        )
        assertTrue(change.isUrl())
    }

    @Test
    fun isUrl_plainText_returnsFalse() {
        val change = ClipboardChange(
            content = "just some text",
            timestamp = 1000L,
        )
        assertFalse(change.isUrl())
    }

    @Test
    fun hasChanged_withNoPrevious_returnsTrue() {
        val change = ClipboardChange(
            content = "new text",
            timestamp = 1000L,
            previousContent = null,
        )
        assertTrue(change.hasChanged())
    }

    @Test
    fun hasChanged_withDifferentPrevious_returnsTrue() {
        val change = ClipboardChange(
            content = "new text",
            timestamp = 1000L,
            previousContent = "old text",
        )
        assertTrue(change.hasChanged())
    }

    @Test
    fun hasChanged_withSamePrevious_returnsFalse() {
        val change = ClipboardChange(
            content = "same text",
            timestamp = 1000L,
            previousContent = "same text",
        )
        assertFalse(change.hasChanged())
    }

    @Test
    fun contentType_allValues() {
        assertEquals(5, ClipboardContentType.entries.size)
        assertTrue(ClipboardContentType.entries.contains(ClipboardContentType.Text))
        assertTrue(ClipboardContentType.entries.contains(ClipboardContentType.Uri))
        assertTrue(ClipboardContentType.entries.contains(ClipboardContentType.Html))
        assertTrue(ClipboardContentType.entries.contains(ClipboardContentType.Image))
        assertTrue(ClipboardContentType.entries.contains(ClipboardContentType.Unknown))
    }

    @Test
    fun clipboardSource_allValues() {
        assertEquals(3, ClipboardSource.entries.size)
        assertTrue(ClipboardSource.entries.contains(ClipboardSource.Internal))
        assertTrue(ClipboardSource.entries.contains(ClipboardSource.External))
        assertTrue(ClipboardSource.entries.contains(ClipboardSource.Unknown))
    }
}
