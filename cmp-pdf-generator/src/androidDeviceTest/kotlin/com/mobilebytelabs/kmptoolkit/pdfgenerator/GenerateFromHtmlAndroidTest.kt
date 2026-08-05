/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device regression for issue #152.
 *
 * Before the fix, `generateFromHtml` on Android routed EVERY output through a `renderHtmlToBytes`
 * stub that unconditionally threw `UnsupportedFeature` — so Share/Save/File/Uri/Print/ByteArray all
 * failed. This test runs the real WebView → `android.graphics.pdf.PdfDocument` path on the connected
 * device/emulator and asserts a genuine PDF comes out (valid `%PDF` header, non-empty). It is the
 * only layer that exercises Android's WebView/PdfDocument — JVM/common tests cannot.
 */
@OptIn(ExperimentalPdfGeneratorApi::class)
@RunWith(AndroidJUnit4::class)
class GenerateFromHtmlAndroidTest {

    private val html =
        """
        <html><head><meta charset="utf-8"></head>
        <body><h1>Issue #152</h1><p>HTML → PDF on Android must work.</p></body></html>
        """.trimIndent()

    private fun newGenerator(): PdfGenerator =
        PdfGenerator().apply { setContext(ApplicationProvider.getApplicationContext<Context>()) }

    private fun ByteArray.hasPdfHeader(): Boolean = size >= 5 && copyOfRange(0, 5).decodeToString() == "%PDF-"

    @Test
    fun htmlToByteArrayProducesValidPdf() = runBlocking {
        val result = newGenerator().generateFromHtml(
            html = html,
            output = PdfOutput.ByteArrayOutput,
            fileName = "issue152",
        )
        assertTrue("expected Success, got $result", result is PdfResult.Success)
        val bytes = (result as PdfResult.Success).bytes
        assertTrue("no bytes returned", bytes != null && bytes.isNotEmpty())
        assertTrue("output is not a PDF (missing %PDF- header)", bytes!!.hasPdfHeader())
    }

    @Test
    fun htmlToFileWritesValidPdf() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val outFile = File(ctx.cacheDir, "issue152-file.pdf").also { it.delete() }
        val result = newGenerator().generateFromHtml(
            html = html,
            output = PdfOutput.File(outFile.path),
            fileName = "issue152-file",
        )
        assertTrue("expected Success, got $result", result is PdfResult.Success)
        assertTrue("PDF file was not written", outFile.exists() && outFile.length() > 0)
        assertTrue("written file is not a PDF (missing %PDF- header)", outFile.readBytes().hasPdfHeader())
    }
}
