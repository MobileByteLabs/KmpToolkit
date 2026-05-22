/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * JVM (Desktop) implementation. Uses OpenHTMLToPDF for HTML route + JvmNativePdfRenderer
 * (Apache PDFBox direct) for DSL route.
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {

    private val progress = MutableSharedFlow<PdfProgressEvent>(extraBufferCapacity = 32)

    public actual suspend fun generateAndSharePdf(
        htmlContent: String,
        fileName: String,
        pageConfig: PageConfig,
    ) {
        val outputFile = pickSaveFile("$fileName.pdf") ?: return
        val finalHtml = htmlContent.injectPageConfigCss(pageConfig)
        withContext(Dispatchers.IO) {
            outputFile.outputStream().use { out ->
                PdfRendererBuilder()
                    .useSVGDrawer(BatikSVGDrawer())
                    .useFastMode()
                    .withHtmlContent(finalHtml, null)
                    .toStream(out)
                    .run()
            }
            openWithDefaultApp(outputFile)
        }
    }

    public actual suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val needsHtmlEngine = document.pages.any { p -> p.elements.any { it is PdfElement.Html } }
            val bytes: ByteArray = if (needsHtmlEngine) {
                val html = document.toHtml()
                renderHtmlToBytes(html, document.config)
            } else {
                JvmNativePdfRenderer(document, options, progress).render()
            }
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(bytes, output)
            progress.tryEmit(PdfProgressEvent.Complete(bytes.size))
            result
        } catch (e: Throwable) {
            val err = e.toPdfError()
            progress.tryEmit(PdfProgressEvent.Failed(err))
            PdfResult.Failure(err)
        }
    }

    public actual suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig,
        branding: PdfBranding,
        options: PdfGeneratorOptions,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val finalHtml = html.injectPageConfigCss(pageConfig)
            val bytes = renderHtmlToBytes(finalHtml, pageConfig)
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(bytes, output)
            progress.tryEmit(PdfProgressEvent.Complete(bytes.size))
            result
        } catch (e: Throwable) {
            val err = e.toPdfError()
            progress.tryEmit(PdfProgressEvent.Failed(err))
            PdfResult.Failure(err)
        }
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private suspend fun renderHtmlToBytes(html: String, pageConfig: PageConfig): ByteArray =
        withContext(Dispatchers.IO) {
            val bao = ByteArrayOutputStream()
            PdfRendererBuilder()
                .useSVGDrawer(BatikSVGDrawer())
                .useFastMode()
                .withHtmlContent(html, null)
                .toStream(bao)
                .run()
            bao.toByteArray()
        }

    private suspend fun dispatchOutput(bytes: ByteArray, output: PdfOutput): PdfResult {
        return when (output) {
            is PdfOutput.File -> {
                File(output.path).writeBytes(bytes)
                PdfResult.Success(byteCount = bytes.size)
            }
            PdfOutput.ByteArrayOutput -> PdfResult.Success(bytes = bytes, byteCount = bytes.size)
            is PdfOutput.Uri -> {
                val tmp = File.createTempFile("pdf-", ".pdf").apply { writeBytes(bytes); deleteOnExit() }
                val uri = tmp.toURI().toString()
                output.callback(uri)
                PdfResult.Success(uri = uri, byteCount = bytes.size)
            }
            PdfOutput.Share -> {
                val tmp = File.createTempFile("pdf-", ".pdf").apply { writeBytes(bytes); deleteOnExit() }
                openWithDefaultApp(tmp)
                PdfResult.Success(byteCount = bytes.size)
            }
            PdfOutput.Print -> throw PdfError.UnsupportedFeature(
                "JVM PdfOutput.Print — use PdfOutput.Save and let user print from viewer",
            )
            PdfOutput.Save -> {
                val out = pickSaveFile("document.pdf") ?: return PdfResult.Failure(PdfError.CancellationError)
                out.writeBytes(bytes)
                openWithDefaultApp(out)
                PdfResult.Success(byteCount = bytes.size)
            }
        }
    }

    private suspend fun pickSaveFile(defaultName: String): File? = withContext(Dispatchers.Swing) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save PDF"
            fileFilter = FileNameExtensionFilter("PDF Documents", "pdf")
            selectedFile = File(defaultName)
        }
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var f = chooser.selectedFile
            if (!f.name.lowercase().endsWith(".pdf")) f = File(f.absolutePath + ".pdf")
            f
        } else null
    }

    private fun openWithDefaultApp(file: File) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try { Desktop.getDesktop().open(file) } catch (_: Throwable) {}
        }
    }
}
