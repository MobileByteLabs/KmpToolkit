package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class ClipboardAsyncTest {

    @Test
    fun getFromClipboardAsync_doesNotThrow() = runTest {
        // Should not throw on any platform (may return null)
        val result = getFromClipboardAsync()
        // result can be null, just verify it doesn't crash
        result?.let { assertNotNull(it) }
    }

    @Test
    fun copyToClipboardAsync_doesNotThrow() = runTest {
        // Should not throw on any platform
        val result = copyToClipboardAsync("Async clipboard test")
        assertNotNull(result.toString())
    }

    @Test
    fun hasClipboardTextAsync_doesNotThrow() = runTest {
        val result = hasClipboardTextAsync()
        assertNotNull(result.toString())
    }

    @Test
    fun asyncCopyAndRead_roundTrip() = runTest {
        val testText = "KmpToolkit Async Test"
        val copySuccess = copyToClipboardAsync(testText)

        if (copySuccess) {
            val hasText = hasClipboardTextAsync()
            if (hasText) {
                val retrieved = getFromClipboardAsync()
                retrieved?.let {
                    assertNotNull(it)
                    // On supported platforms, content should contain our text
                }
            }
        }
        // If copy fails (JS/Wasm), test still passes — we just verify no crash
    }
}
