/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    ExperimentalPdfGeneratorApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UIPrintInteractionController
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKPDFConfiguration
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * iOS implementation. HTML route uses `WKWebView.loadHTMLString` + waits for
 * `didFinishNavigation` callback, then calls `createPDF`.
 *
 * Cleanup: `WKWebView` is retained by an internal field for the duration of the suspend call,
 * released on completion (ARC).
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {
    private val progress = MutableSharedFlow<PdfProgressEvent>(extraBufferCapacity = 32)
    private var presentingRootViewController: platform.UIKit.UIViewController? = null

    /**
     * Optional — supply the root view controller used to present Share/Save sheets.
     * Defaults to `UIApplication.sharedApplication.keyWindow.rootViewController`.
     */
    public fun setRootViewController(vc: platform.UIKit.UIViewController) {
        presentingRootViewController = vc
    }

    public actual suspend fun generateAndSharePdf(htmlContent: String, fileName: String, pageConfig: PageConfig) {
        val data = renderHtmlToData(htmlContent.injectPageConfigCss(pageConfig))
        presentShareSheet(data, fileName)
    }

    public actual suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val html = document.toHtml()
            val data = renderHtmlToData(html.injectPageConfigCss(document.config))
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(data, output, fileName)
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
            val result = dispatchOutput(data, output, fileName)
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
        val config = WKWebViewConfiguration()
        val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 612.0, 792.0), configuration = config)

        val navDelegate =
            object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    val pdfConfig = WKPDFConfiguration()
                    webView.createPDFWithConfiguration(pdfConfig) { data: NSData?, error: NSError? ->
                        when {
                            error != null -> {
                                pdfReady.completeExceptionally(
                                    PdfError.EngineFailure(
                                        IllegalStateException(
                                            "WKWebView.createPDF failed: " +
                                                error.localizedDescription,
                                        ),
                                    ),
                                )
                            }

                            data == null -> {
                                pdfReady.completeExceptionally(
                                    PdfError.EngineFailure(
                                        IllegalStateException("WKWebView.createPDF returned null"),
                                    ),
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
                        PdfError.EngineFailure(
                            IllegalStateException("WKWebView navigation failed: ${withError.localizedDescription}"),
                        ),
                    )
                }

                @kotlinx.cinterop.ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailProvisionalNavigation: WKNavigation?,
                    withError: NSError,
                ) {
                    pdfReady.completeExceptionally(
                        PdfError.EngineFailure(
                            IllegalStateException(
                                "WKWebView provisional navigation failed: ${withError.localizedDescription}",
                            ),
                        ),
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
            presentShareSheet(data, fileName)
            PdfResult.Success(byteCount = data.length.toInt())
        }

        PdfOutput.Print -> {
            val printer = UIPrintInteractionController.sharedPrintController()
            printer.printingItem = data
            printer.presentAnimated(true, completionHandler = null)
            PdfResult.Success(byteCount = data.length.toInt())
        }

        PdfOutput.Save -> {
            val url = writeToTemp(data, fileName)
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url))
            picker.modalPresentationStyle = UIModalPresentationFormSheet
            rootViewController()?.presentViewController(picker, animated = true, completion = null)
            PdfResult.Success(uri = url.absoluteString, byteCount = data.length.toInt())
        }
    }

    private fun writeToTemp(data: NSData, fileName: String): NSURL {
        val tmpPath = NSTemporaryDirectory() + fileName
        val url = NSURL.fileURLWithPath(tmpPath)
        data.writeToURL(url, true)
        return url
    }

    private fun presentShareSheet(data: NSData, fileName: String) {
        val url = writeToTemp(data, fileName)
        val vc = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        rootViewController()?.presentViewController(vc, animated = true, completion = null)
    }

    private fun rootViewController(): platform.UIKit.UIViewController? =
        presentingRootViewController ?: UIApplication.sharedApplication.keyWindow?.rootViewController
}

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()
