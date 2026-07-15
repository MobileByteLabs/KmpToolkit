/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.w3c.dom.HTMLIFrameElement

/**
 * wasmJs (browser) implementation. Mirror of the JS impl for the HTML route — uses iframe +
 * `window.print()` for Print/Share/Save outputs. DSL byte route via `pdf-lib` is deferred
 * because the npm interop surface for wasmJs is still maturing.
 *
 * Note: browsers do not expose a reliable way to control the file name used by the print/save
 * dialog, so the `fileName` argument is accepted for cross-platform API parity but is ignored
 * here — the user picks the name in the browser's own dialog.
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {
    private val progress = MutableSharedFlow<PdfProgressEvent>(extraBufferCapacity = 32)

    public actual suspend fun generateAndSharePdf(htmlContent: String, fileName: String, pageConfig: PageConfig) {
        printViaIframe(htmlContent.injectPageConfigCss(pageConfig))
    }

    public actual suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val html = document.toHtml().injectPageConfigCss(document.config)
            handleOutput(html, output, ensurePdfFileName(fileName)).also {
                progress.tryEmit(PdfProgressEvent.Complete(html.length))
            }
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
        fileName: String,
    ): PdfResult = try {
        handleOutput(html.injectPageConfigCss(pageConfig), output, ensurePdfFileName(fileName)).also {
            progress.tryEmit(PdfProgressEvent.Complete(html.length))
        }
    } catch (e: Throwable) {
        PdfResult.Failure(e.toPdfError())
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private suspend fun handleOutput(html: String, output: PdfOutput, fileName: String): PdfResult = when (output) {
        PdfOutput.Print, PdfOutput.Share, PdfOutput.Save -> {
            printViaIframe(html)
            PdfResult.Success()
        }

        is PdfOutput.File -> throw PdfError.UnsupportedFeature("File output on wasmJs — use Print/Share/Save")

        PdfOutput.ByteArrayOutput -> throw PdfError.UnsupportedFeature(
            "ByteArray output on wasmJs deferred — pdf-lib wasmJs interop pending.",
        )

        is PdfOutput.Uri -> throw PdfError.UnsupportedFeature("Uri output on wasmJs deferred.")
    }

    private suspend fun printViaIframe(html: String) {
        val docBody = document.body ?: throw PdfError.EngineFailure(IllegalStateException("document.body is null"))
        val completion = CompletableDeferred<Unit>()
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.setAttribute("style", "position:fixed;visibility:hidden;width:0;height:0;border:none;")
        docBody.appendChild(iframe)
        val frameDoc = iframe.contentWindow?.document
            ?: throw PdfError.EngineFailure(IllegalStateException("iframe content window inaccessible"))
        frameDoc.open()
        frameDoc.write(html)
        iframe.onload = { _ ->
            window.setTimeout(
                {
                    try {
                        iframe.contentWindow?.print()
                        completion.complete(Unit)
                    } catch (e: Throwable) {
                        completion.completeExceptionally(e)
                    }
                    null
                },
                0,
            )
        }
        frameDoc.close()
        completion.await()
        window.setTimeout(
            {
                if (docBody.contains(iframe)) docBody.removeChild(iframe)
                null
            },
            2000,
        )
    }
}

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()
