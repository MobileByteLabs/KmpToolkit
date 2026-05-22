/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Tier3StubTest {

    @Test
    fun generateAndSharePdfThrowsUnsupportedPlatform() = runTest {
        val gen = PdfGenerator()
        val err = assertFailsWith<PdfError.UnsupportedPlatform> {
            gen.generateAndSharePdf("<p>hi</p>", "test", PageConfig())
        }
        assertTrue(err.message!!.contains("linux"))
    }

    @Test
    fun generateReturnsFailureWithUnsupportedPlatform() = runTest {
        val gen = PdfGenerator()
        val doc = pdf { page { text("hi") } }
        val result = gen.generate(doc, PdfOutput.ByteArrayOutput)
        assertIs<PdfResult.Failure>(result)
        assertIs<PdfError.UnsupportedPlatform>(result.error)
        assertTrue((result.error as PdfError.UnsupportedPlatform).platform == "linux")
    }
}
