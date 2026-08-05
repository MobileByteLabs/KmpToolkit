/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopRunInMode
import platform.CoreFoundation.kCFRunLoopDefaultMode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosPdfSmokeTest {
    /**
     * iOS HTML→PDF drives a real `WKWebView` (`loadHTMLString` → `didFinishNavigation` →
     * `createPDF`). `WKWebView` only navigates inside a live `UIApplication`/window context, which
     * the headless Kotlin/Native `iosSimulatorArm64Test` binary does NOT provide — so navigation
     * never completes and the call times out. (The pre-existing version used `runTest`, which
     * instead failed with `UncompletedCoroutinesError`.) There is no non-WebView PDF path on iOS to
     * fall back to, so this cannot be exercised in the headless test harness.
     *
     * The body below is the CORRECT way to drive it (run the suspend call + pump the run loop) and
     * passes in a real app / XCUITest context; it is [Ignore]d here until such a host exists. The
     * iOS `WKWebView.createPDF` path is a standard Apple API used as-is; the analogous HTML→PDF
     * behavior is executed on-device by the Android instrumented test (`GenerateFromHtmlAndroidTest`).
     */
    @Ignore // WKWebView needs a UIApplication/window context unavailable in headless K/N tests — see KDoc.
    @Test
    fun htmlRouteProducesValidPdfBytes() {
        val gen = PdfGenerator()
        val doctype =
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
        val html =
            """
                $doctype
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/><title>iOS Smoke</title>
                <style>/* PAGE_CONFIG_PLACEHOLDER */</style></head>
                <body><h1>Hello iOS</h1></body></html>
            """.trimIndent()

        var result: PdfResult? = null
        var failure: Throwable? = null
        CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                result = gen.generateFromHtml(html, PdfOutput.ByteArrayOutput, PageConfig())
            } catch (e: Throwable) {
                failure = e
            }
        }

        // Pump the run loop (0.05s slices, ≤30s) so WKWebView delegate/createPDF callbacks fire.
        var pumps = 0
        while (result == null && failure == null && pumps < 600) {
            CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.05, true)
            pumps++
        }

        failure?.let { throw it }
        val settled = assertNotNull(result, "generateFromHtml did not complete within 30s")
        assertIs<PdfResult.Success>(settled)
        val bytes = settled.bytes!!
        assertTrue(bytes.size > 100)
        assertTrue(bytes[0] == '%'.code.toByte())
    }
}
