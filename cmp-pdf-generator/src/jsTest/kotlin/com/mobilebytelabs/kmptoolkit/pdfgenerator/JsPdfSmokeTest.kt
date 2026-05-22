/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * JS smoke test — runs via pdf-lib DSL route. Headless Node mode.
 *
 * Note: the iframe + window.print() path can't be exercised in Node (no DOM); we test the
 * pdf-lib ByteArray path.
 */
class JsPdfSmokeTest {
    @Test
    fun pdfLibDslRouteProducesValidBytes() = runTest {
        val gen = PdfGenerator()
        val doc =
            pdf {
                page {
                    heading(1, "JS Smoke")
                    text("Hello from pdf-lib.")
                }
            }
        val result = gen.generate(doc, PdfOutput.ByteArrayOutput)
        assertIs<PdfResult.Success>(result)
        val bytes = result.bytes!!
        assertTrue(bytes.size > 100, "Expected non-trivial bytes")
        assertTrue(bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte())
    }
}
