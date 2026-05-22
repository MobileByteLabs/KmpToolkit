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
 * wasmJs (browser) implementation. Mirror of the JS impl but using wasmJs-compatible APIs.
 * HTML route: iframe + window.print(). Bytes/share routes deferred — pdf-lib JS interop
 * planned for v0.2.
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
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val html = document.toHtml().injectPageConfigCss(document.config)
            handleOutput(html, output, "document.pdf").also {
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
    ): PdfResult = try {
        handleOutput(html.injectPageConfigCss(pageConfig), output, "document.pdf").also {
            progress.tryEmit(PdfProgressEvent.Complete(html.length))
        }
    } catch (e: Throwable) {
        PdfResult.Failure(e.toPdfError())
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private suspend fun handleOutput(html: String, output: PdfOutput, fileName: String): PdfResult = when (output) {
        PdfOutput.Print -> { printViaIframe(html); PdfResult.Success() }
        PdfOutput.Share, PdfOutput.Save -> { printViaIframe(html); PdfResult.Success() }
        is PdfOutput.File -> throw PdfError.UnsupportedFeature("File output on wasmJs — use Print")
        PdfOutput.ByteArrayOutput -> throw PdfError.UnsupportedFeature(
            "ByteArray output on wasmJs requires pdf-lib JS interop (deferred to v0.2).",
        )
        is PdfOutput.Uri -> throw PdfError.UnsupportedFeature("Uri output on wasmJs deferred to v0.2.")
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
            window.setTimeout({
                try {
                    iframe.contentWindow?.print()
                    completion.complete(Unit)
                } catch (e: Throwable) {
                    completion.completeExceptionally(e)
                }
            }, 0)
        }
        frameDoc.close()
        completion.await()
        window.setTimeout({
            if (docBody.contains(iframe)) docBody.removeChild(iframe)
        }, 2000)
    }
}

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()
