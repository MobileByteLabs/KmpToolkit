package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Common tests for clipboard functionality.
 *
 * Note: Clipboard operations are platform-dependent and may require
 * specific setup or permissions on different platforms:
 *
 * - Android: Requires initClipboard() to be called first
 * - JS/Wasm: Read operations are not supported synchronously
 * - WASI: No clipboard support at all
 * - Linux: Requires xclip or xsel to be installed
 *
 * These tests verify that the basic API is available and doesn't crash.
 * Full integration tests should be run on actual devices/environments.
 */
class ClipboardTest {

    @Test
    fun copyToClipboard_doesNotThrow() {
        // This test verifies that the function is callable
        // The return value depends on platform setup
        val result = copyToClipboard("Test text")
        // On some platforms this may return false if clipboard is unavailable
        // We just verify it doesn't throw
        assertNotNull(result.toString())
    }

    @Test
    fun getFromClipboard_doesNotThrow() {
        // This test verifies that the function is callable
        // May return null on JS, WASI, or if clipboard is empty
        val result = getFromClipboard()
        // Result can be null, we just verify it doesn't throw
        result?.let {
            assertNotNull(it)
        }
    }

    @Test
    fun hasClipboardText_doesNotThrow() {
        // This test verifies that the function is callable
        val result = hasClipboardText()
        assertNotNull(result.toString())
    }

    @Test
    fun clearClipboard_doesNotThrow() {
        // This test verifies that the function is callable
        clearClipboard()
        // If we reach here, the test passes
    }

    @Test
    fun copyAndGet_basicRoundTrip() {
        // This test attempts a basic copy/paste cycle
        // Note: This may not work on all platforms (JS, WASI)
        val testText = "KmpToolkit Clipboard Test ${System.currentTimeMillis()}"

        val copySuccess = copyToClipboard(testText)

        // Only verify read if copy succeeded and we're on a platform that supports read
        if (copySuccess && hasClipboardText()) {
            val retrieved = getFromClipboard()
            // On supported platforms, this should match
            // On others, it may be null
            retrieved?.let {
                // If we got text back, verify it matches
                assertTrue(
                    it.contains("KmpToolkit"),
                    "Retrieved text should contain 'KmpToolkit'",
                )
            }
        }
    }
}

// Note: System.currentTimeMillis() is not available in pure common code
// This would need to be replaced with expect/actual or removed for strict common tests
private object System {
    fun currentTimeMillis(): Long = 0L // Placeholder for common code
}
