package com.mobilebytelabs.kmptoolkit.openurl

import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Android-hosted (JVM-based) tests for the Android actual implementation.
 *
 * Now actually running under Robolectric, as this file always claimed. Without it there is no
 * init ContentProvider, so `OpenUrlContext.context` threw
 * "OpenUrlInitProvider was not initialised" and `openWithApp` returned Error instead of the
 * documented Success/NoHandler. Robolectric supplies a real Application, injected below through
 * the same seam the ContentProvider uses on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class OpenUrlAndroidTest {

    @Before
    fun installApplicationContext() {
        OpenUrlContext.init(RuntimeEnvironment.getApplication())
    }

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
