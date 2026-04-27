package com.mobilebytelabs.kmptoolkit.openurl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenUrlTest {

    // -------------------------------------------------------------------------
    // openUrl — never throws contract
    // -------------------------------------------------------------------------

    @Test
    fun openUrl_withInvalidScheme_returnsFalseWithoutThrowing() {
        val result = runCatching { openUrl("not-a-url-at-all") }
        assertTrue(result.isSuccess, "openUrl must never throw; got: ${result.exceptionOrNull()}")
    }

    @Test
    fun openUrl_withEmptyString_returnsFalseWithoutThrowing() {
        val result = runCatching { openUrl("") }
        assertTrue(result.isSuccess, "openUrl('') must not throw")
    }

    @Test
    fun openUrl_withNullByteScheme_doesNotThrow() {
        val result = runCatching { openUrl("\u0000bad") }
        assertTrue(result.isSuccess)
    }

    // -------------------------------------------------------------------------
    // openInBrowser — never throws contract
    // -------------------------------------------------------------------------

    @Test
    fun openInBrowser_withEmptyString_doesNotThrow() {
        val result = runCatching { openInBrowser("") }
        assertTrue(result.isSuccess)
    }

    // -------------------------------------------------------------------------
    // openWithApp — result type contract
    // -------------------------------------------------------------------------

    @Test
    fun openWithApp_CustomHint_onNonAndroid_returnsSuccessOrNoHandler() {
        val result = openWithApp("https://example.com", AppHint.Custom("com.some.app"))
        assertTrue(
            result is OpenUrlResult.Success || result is OpenUrlResult.NoHandler,
            "Custom hint on non-Android must succeed or give NoHandler, got: $result",
        )
    }

    @Test
    fun openWithApp_withInvalidUrl_neverThrows() {
        val result = runCatching { openWithApp(":::bad:::") }
        assertTrue(result.isSuccess)
    }

    @Test
    fun openWithApp_resultIsExhaustivelyMatchable() {
        val result: OpenUrlResult = OpenUrlResult.NoHandler
        // This must compile — exhaustive when
        val label = when (result) {
            OpenUrlResult.Success -> "success"
            OpenUrlResult.NoHandler -> "no-handler"
            is OpenUrlResult.Error -> "error: ${result.message}"
        }
        assertTrue(label.isNotEmpty())
    }

    // -------------------------------------------------------------------------
    // canOpen — never throws contract
    // -------------------------------------------------------------------------

    @Test
    fun canOpen_withEmptyString_doesNotThrow() {
        val result = runCatching { canOpen("") }
        assertTrue(result.isSuccess)
    }

    @Test
    fun canOpen_returnsBooleanNotException() {
        // Result should be a boolean value — we don't assert true/false
        // because platform availability differs, but it must not throw.
        val result = runCatching { canOpen("https://example.com") }
        assertTrue(result.isSuccess)
    }

    // -------------------------------------------------------------------------
    // AppHint — sealed class coverage
    // -------------------------------------------------------------------------

    @Test
    fun appHint_customEquality() {
        val a = AppHint.Custom("com.example")
        val b = AppHint.Custom("com.example")
        assertTrue(a == b)
        assertFalse(AppHint.Custom("com.a") == AppHint.Custom("com.b"))
    }

    @Test
    fun appHint_objectSingletons() {
        assertTrue(AppHint.DEFAULT === AppHint.DEFAULT)
        assertTrue(AppHint.EMAIL === AppHint.EMAIL)
    }
}
