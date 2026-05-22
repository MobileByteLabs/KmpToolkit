/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IosPdfSmokeTest {

    @Test
    fun htmlRouteProducesValidPdfBytes() = runTest {
        val gen = PdfGenerator()
        val html = """
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/><title>iOS Smoke</title>
            <style>/* PAGE_CONFIG_PLACEHOLDER */</style></head>
            <body><h1>Hello iOS</h1></body></html>
        """.trimIndent()
        val result = gen.generateFromHtml(html, PdfOutput.ByteArrayOutput, PageConfig())
        assertIs<PdfResult.Success>(result)
        val bytes = result.bytes!!
        assertTrue(bytes.size > 100)
        assertTrue(bytes[0] == '%'.code.toByte())
    }
}
