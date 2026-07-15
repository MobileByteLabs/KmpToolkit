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
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

/**
 * JS (browser + Node) implementation.
 *
 * Two routes:
 *  - HTML route — iframe + `window.print()`. Used by `generateAndSharePdf` and by `generate()`
 *    when output is `Print`/`Share`/`Save` and the document contains Html elements.
 *  - DSL route via **pdf-lib** (npm) — used for `ByteArrayOutput`, `Uri`, `File` outputs.
 *    Yields a real Uint8Array of PDF bytes. Consumers must have `pdf-lib` resolvable in npm
 *    (Kotlin Gradle plugin handles this when `kotlin.js` plugin is configured).
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
            when (output) {
                PdfOutput.Print -> {
                    val html = document.toHtml().injectPageConfigCss(document.config)
                    printViaIframe(html)
                    progress.tryEmit(PdfProgressEvent.Complete(html.length))
                    PdfResult.Success()
                }

                PdfOutput.ByteArrayOutput, is PdfOutput.File, is PdfOutput.Uri, PdfOutput.Share, PdfOutput.Save -> {
                    val bytes = JsPdfLibRenderer(document, progress).render()
                    progress.tryEmit(PdfProgressEvent.Finalizing)
                    val result = dispatchOutput(bytes, output, ensurePdfFileName(fileName))
                    progress.tryEmit(PdfProgressEvent.Complete(bytes.size))
                    result
                }
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
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val finalHtml = html.injectPageConfigCss(pageConfig)
            when (output) {
                PdfOutput.Print, PdfOutput.Share, PdfOutput.Save -> {
                    printViaIframe(finalHtml)
                    PdfResult.Success()
                }

                is PdfOutput.Uri -> {
                    val blob = Blob(arrayOf(finalHtml), BlobPropertyBag(type = "text/html"))
                    val url = URL.createObjectURL(blob)
                    output.callback(url)
                    PdfResult.Success(uri = url)
                }

                else -> {
                    throw PdfError.UnsupportedFeature(
                        "JS generateFromHtml only supports Print/Share/Save/Uri. " +
                            "For ByteArray/File output use the DSL route via generate(PdfDocument, ...).",
                    )
                }
            }
        } catch (e: Throwable) {
            PdfResult.Failure(e.toPdfError())
        }
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private fun dispatchOutput(bytes: ByteArray, output: PdfOutput, fileName: String): PdfResult = when (output) {
        is PdfOutput.File -> {
            triggerBytesDownload(bytes, output.path.substringAfterLast('/'))
            PdfResult.Success(byteCount = bytes.size)
        }

        PdfOutput.ByteArrayOutput -> {
            PdfResult.Success(bytes = bytes, byteCount = bytes.size)
        }

        is PdfOutput.Uri -> {
            val url = createBlobUrl(bytes)
            output.callback(url)
            PdfResult.Success(uri = url, byteCount = bytes.size)
        }

        PdfOutput.Share -> {
            if (js("typeof navigator !== 'undefined' && navigator.share") != null) {
                sharePdfNative(bytes, fileName)
            } else {
                triggerBytesDownload(bytes, fileName)
            }
            PdfResult.Success(byteCount = bytes.size)
        }

        PdfOutput.Save -> {
            triggerBytesDownload(bytes, fileName)
            PdfResult.Success(byteCount = bytes.size)
        }

        PdfOutput.Print -> {
            throw PdfError.UnsupportedFeature(
                "Print of DSL bytes not supported on JS — pass HTML via generateAndSharePdf",
            )
        }
    }

    private fun createBlobUrl(bytes: ByteArray): String {
        val u8 = byteArrayToUint8Array(bytes)
        val blob = Blob(arrayOf(u8), BlobPropertyBag(type = "application/pdf"))
        return URL.createObjectURL(blob)
    }

    private fun triggerBytesDownload(bytes: ByteArray, fileName: String) {
        val url = createBlobUrl(bytes)
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = url
        a.download = fileName
        document.body?.appendChild(a)
        a.click()
        document.body?.removeChild(a)
        window.setTimeout({
            URL.revokeObjectURL(url)
            null
        }, 5000)
    }

    private fun sharePdfNative(bytes: ByteArray, fileName: String) {
        val u8 = byteArrayToUint8Array(bytes)
        val blob = Blob(arrayOf(u8), BlobPropertyBag(type = "application/pdf"))
        val file = js("new File([blob], fileName, { type: 'application/pdf' })")
        val data = js("({})")
        data.files = arrayOf(file)
        data.title = fileName
        js("navigator.share")(data)
    }

    private suspend fun printViaIframe(html: String) {
        val docBody = document.body ?: throw PdfError.EngineFailure(IllegalStateException("document.body is null"))
        val completion = CompletableDeferred<Unit>()
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.setAttribute("style", "position:fixed;visibility:hidden;width:0;height:0;border:none;")
        docBody.appendChild(iframe)
        val frameDoc =
            iframe.contentWindow?.document
                ?: throw PdfError.EngineFailure(IllegalStateException("iframe content window inaccessible"))
        frameDoc.open()
        frameDoc.write(html)
        iframe.onload = {
            window.setTimeout({
                try {
                    iframe.contentWindow?.print()
                    completion.complete(Unit)
                } catch (e: Throwable) {
                    completion.completeExceptionally(e)
                }
                null
            }, 0)
        }
        frameDoc.close()
        completion.await()
        window.setTimeout({
            if (docBody.contains(iframe)) docBody.removeChild(iframe)
            null
        }, 2000)
    }
}

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()

// injectPageConfigCss moved to commonMain (PageConfigCssInjection.kt)

@ExperimentalPdfGeneratorApi
internal fun byteArrayToUint8Array(bytes: ByteArray): Uint8Array {
    val u8 = Uint8Array(bytes.size)
    for (i in bytes.indices) u8.asDynamic()[i] = bytes[i]
    return u8
}
