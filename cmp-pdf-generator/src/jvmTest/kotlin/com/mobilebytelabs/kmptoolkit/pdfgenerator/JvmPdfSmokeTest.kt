/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * JVM smoke test — generates a real PDF byte stream and asserts the magic prefix.
 * Runs against the OpenHTMLToPDF stack and the PDFBox DSL renderer.
 */
class JvmPdfSmokeTest {
    @Test
    fun dslPathProducesValidPdfMagic() = runTest {
        val gen = PdfGenerator()
        val doc =
            pdf {
                branding(PdfBranding.none())
                page {
                    heading(1, "Smoke Test")
                    text("This is a smoke-test PDF generated via the DSL route.")
                    divider()
                    table {
                        header {
                            cell("Col A")
                            cell("Col B")
                        }
                        row {
                            cell("1")
                            cell("2")
                        }
                        row {
                            cell("3")
                            cell("4")
                        }
                    }
                }
            }
        val result = gen.generate(doc, PdfOutput.ByteArrayOutput)
        assertIs<PdfResult.Success>(result)
        val bytes = result.bytes
        assertTrue(bytes != null && bytes.size > 100, "Expected non-trivial PDF byte stream")
        assertPdfMagic(bytes!!)
    }

    @Test
    fun htmlPathProducesValidPdfMagic() = runTest {
        val gen = PdfGenerator()
        val doctype =
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
        val html =
            """
                $doctype
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/><title>T</title>
                <style>body { font-family: Helvetica, Arial, sans-serif; }
                /* PAGE_CONFIG_PLACEHOLDER */</style></head>
                <body><h1>Smoke</h1><p>Hello</p></body></html>
            """.trimIndent()
        val result = gen.generateFromHtml(html, PdfOutput.ByteArrayOutput, PageConfig())
        assertIs<PdfResult.Success>(result)
        val bytes = result.bytes
        assertTrue(bytes != null && bytes.size > 100)
        assertTrue(bytes!![0] == '%'.code.toByte())
    }

    @Test
    fun deterministicModeFlagDoesNotThrow() = runTest {
        val gen = PdfGenerator()
        val doc = pdf { page { text("Det") } }
        val opts = PdfGeneratorOptions(deterministic = true)
        val result = gen.generate(doc, PdfOutput.ByteArrayOutput, opts)
        assertIs<PdfResult.Success>(result)
    }

    private fun assertPdfMagic(bytes: ByteArray) {
        require(bytes.size >= 4) { "PDF must have at least 4 bytes" }
        assertTrue(bytes[0] == '%'.code.toByte(), "byte 0 should be '%'")
        assertTrue(bytes[1] == 'P'.code.toByte(), "byte 1 should be 'P'")
        assertTrue(bytes[2] == 'D'.code.toByte(), "byte 2 should be 'D'")
        assertTrue(bytes[3] == 'F'.code.toByte(), "byte 3 should be 'F'")
    }
}
