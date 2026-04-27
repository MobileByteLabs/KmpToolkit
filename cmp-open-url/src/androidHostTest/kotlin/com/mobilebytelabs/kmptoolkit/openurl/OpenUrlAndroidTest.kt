package com.mobilebytelabs.kmptoolkit.openurl

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Android-hosted (JVM-based) tests for the Android actual implementation.
 * These run on the JVM using Robolectric or a real device/emulator.
 */
class OpenUrlAndroidTest {

    @Test
    fun openUrl_withHttpsUrl_doesNotThrow() {
        // Smoke test: calling openUrl on a valid HTTPS URL must not throw.
        // In a host-test environment without a real Android context the call
        // returns false — that's acceptable.
        val result = runCatching { openUrl("https://github.com/MobileByteLabs") }
        assertTrue(result.isSuccess, "openUrl must not throw on Android: ${result.exceptionOrNull()}")
    }

    @Test
    fun openWithApp_emailHint_doesNotThrow() {
        val result = runCatching { openWithApp("mailto:hello@example.com", AppHint.EMAIL) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun openWithApp_customHint_fallsBackWithoutThrow() {
        val result = runCatching {
            openWithApp("https://example.com", AppHint.Custom("com.nonexistent.app"))
        }
        assertTrue(result.isSuccess)
        // Result is either Success (if Android context available) or NoHandler
        val value = result.getOrNull()
        assertTrue(
            value == null || value is OpenUrlResult.Success || value is OpenUrlResult.NoHandler,
            "Unexpected result: $value",
        )
    }

    @Test
    fun canOpen_withValidUrl_doesNotThrow() {
        val result = runCatching { canOpen("https://example.com") }
        assertTrue(result.isSuccess)
    }
}
