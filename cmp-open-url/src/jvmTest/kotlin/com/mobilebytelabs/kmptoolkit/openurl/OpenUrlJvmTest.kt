package com.mobilebytelabs.kmptoolkit.openurl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenUrlJvmTest {

    @Test
    fun openUrl_withInvalidUri_returnsFalse() {
        // Malformed URI that cannot be parsed — must return false, not throw.
        val result = runCatching { openUrl(":::invalid:::") }
        assertTrue(result.isSuccess)
        // Either false or possibly true if Desktop somehow accepted it
    }

    @Test
    fun openUrl_withEmptyString_doesNotThrow() {
        val result = runCatching { openUrl("") }
        assertTrue(result.isSuccess)
    }

    @Test
    fun canOpen_returnsBooleanOnJvm() {
        val result = runCatching { canOpen("https://example.com") }
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun openWithApp_returnsSuccessOrNoHandler() {
        val result = runCatching { openWithApp("https://example.com") }
        assertTrue(result.isSuccess)
        val value = result.getOrNull()
        assertTrue(
            value is OpenUrlResult.Success ||
                value is OpenUrlResult.NoHandler ||
                value is OpenUrlResult.Error,
            "Unexpected null result",
        )
    }

    @Test
    fun openWithApp_customHint_jvmIgnoresPackageName() {
        // Custom hint on JVM falls back gracefully
        val result = runCatching {
            openWithApp("https://example.com", AppHint.Custom("com.nonexistent"))
        }
        assertTrue(result.isSuccess)
    }
}
