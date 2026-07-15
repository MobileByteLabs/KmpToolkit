/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    ExperimentalPdfGeneratorApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AppKit.NSApplication
import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSSavePanel
import platform.AppKit.NSSharingServicePicker
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKPDFConfiguration
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * macOS implementation. HTML route: `WKWebView` + `didFinishNavigation` → `createPDF` (macOS 11+).
 * Output flows: NSSavePanel (B1/B6), NSSharingServicePicker (B4), file URL (B3).
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {
    private val progress = MutableSharedFlow<PdfProgressEvent>(extraBufferCapacity = 32)

    public actual suspend fun generateAndSharePdf(htmlContent: String, fileName: String, pageConfig: PageConfig) {
        val data = renderHtmlToData(htmlContent.injectPageConfigCss(pageConfig))
        saveViaPanel(data, fileName)
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
            val data = renderHtmlToData(html)
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(data, output, ensurePdfFileName(fileName))
            progress.tryEmit(PdfProgressEvent.Complete(data.length.toInt()))
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
        fileName: String,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val data = renderHtmlToData(html.injectPageConfigCss(pageConfig))
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(data, output, ensurePdfFileName(fileName))
            progress.tryEmit(PdfProgressEvent.Complete(data.length.toInt()))
            result
        } catch (e: Throwable) {
            val err = e.toPdfError()
            progress.tryEmit(PdfProgressEvent.Failed(err))
            PdfResult.Failure(err)
        }
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private suspend fun renderHtmlToData(html: String): NSData {
        val pdfReady = CompletableDeferred<NSData>()
        val webView =
            WKWebView(
                frame = CGRectMake(0.0, 0.0, 612.0, 792.0),
                configuration = WKWebViewConfiguration(),
            )

        val navDelegate =
            object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    webView.createPDFWithConfiguration(WKPDFConfiguration()) { data: NSData?, error: NSError? ->
                        when {
                            error != null -> {
                                pdfReady.completeExceptionally(
                                    PdfError.EngineFailure(
                                        IllegalStateException("WKWebView.createPDF: ${error.localizedDescription}"),
                                    ),
                                )
                            }

                            data == null -> {
                                pdfReady.completeExceptionally(
                                    PdfError.EngineFailure(IllegalStateException("WKWebView.createPDF returned null")),
                                )
                            }

                            else -> {
                                pdfReady.complete(data)
                            }
                        }
                    }
                }

                @kotlinx.cinterop.ObjCSignatureOverride
                override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
                    pdfReady.completeExceptionally(
                        PdfError.EngineFailure(IllegalStateException(withError.localizedDescription)),
                    )
                }

                @kotlinx.cinterop.ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailProvisionalNavigation: WKNavigation?,
                    withError: NSError,
                ) {
                    pdfReady.completeExceptionally(
                        PdfError.EngineFailure(IllegalStateException(withError.localizedDescription)),
                    )
                }
            }
        webView.navigationDelegate = navDelegate
        webView.loadHTMLString(html, baseURL = null)
        return pdfReady.await()
    }

    private fun dispatchOutput(data: NSData, output: PdfOutput, fileName: String): PdfResult = when (output) {
        is PdfOutput.File -> {
            val url = NSURL.fileURLWithPath(output.path)
            data.writeToURL(url, true)
            PdfResult.Success(byteCount = data.length.toInt())
        }

        PdfOutput.ByteArrayOutput -> {
            PdfResult.Success(bytes = data.toByteArray(), byteCount = data.length.toInt())
        }

        is PdfOutput.Uri -> {
            val url = writeToTemp(data, fileName)
            output.callback(url.absoluteString ?: "")
            PdfResult.Success(uri = url.absoluteString, byteCount = data.length.toInt())
        }

        PdfOutput.Share -> {
            val url = writeToTemp(data, fileName)
            presentSharePicker(url)
            PdfResult.Success(byteCount = data.length.toInt())
        }

        PdfOutput.Save -> {
            saveViaPanel(data, fileName)
            PdfResult.Success(byteCount = data.length.toInt())
        }

        PdfOutput.Print -> {
            throw PdfError.UnsupportedFeature(
                "macOS Print — consumer must wire NSPrintOperation in app code (requires NSView)",
            )
        }
    }

    private fun writeToTemp(data: NSData, fileName: String): NSURL {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        data.writeToURL(url, true)
        return url
    }

    private fun presentSharePicker(url: NSURL) {
        val picker = NSSharingServicePicker(items = listOf(url))
        NSApplication.sharedApplication().keyWindow?.contentView?.let { view ->
            // NSMinYEdge = 3 (CGRectEdge.NSRectEdgeMinY). Hardcoding the value to avoid
            // K/Native binding-name drift across AppKit versions.
            picker.showRelativeToRect(view.bounds, ofView = view, preferredEdge = 3uL)
        }
    }

    private fun saveViaPanel(data: NSData, fileName: String) {
        val panel = NSSavePanel.savePanel()
        panel.nameFieldStringValue = fileName
        if (panel.runModal() == NSModalResponseOK) {
            panel.URL?.let { data.writeToURL(it, true) }
        }
    }
}

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()
