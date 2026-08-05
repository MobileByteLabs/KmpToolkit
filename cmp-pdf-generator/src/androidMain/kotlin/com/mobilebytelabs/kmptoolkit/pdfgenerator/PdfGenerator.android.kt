/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Android implementation of [PdfGenerator].
 *
 * HTML route: WebView's `createPrintDocumentAdapter` → write to a temp `PdfPrintListener` file
 * (or PrintManager when [PdfOutput.Print] requested).
 * DSL route: `AndroidNativePdfRenderer` (Canvas-based, see file).
 *
 * Context must be supplied before generation. The Compose helper [rememberPdfGenerator] handles
 * this via `LocalContext`. Direct (non-Compose) consumers call [setContext] first.
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {
    private var context: Context? = null
    private val progress = MutableSharedFlow<PdfProgressEvent>(extraBufferCapacity = 32)

    /** Required before any generation call. Compose helper does this for you. */
    public fun setContext(context: Context) {
        this.context = context
    }

    public actual suspend fun generateAndSharePdf(htmlContent: String, fileName: String, pageConfig: PageConfig) {
        val ctx = requireContext()
        withContext(Dispatchers.Main) {
            val finalHtml = htmlContent.injectPageConfigCss(pageConfig)
            val pm = ctx.getSystemService<PrintManager>() ?: throw PdfError.PermissionDenied("PrintManager unavailable")
            val attrs = pageConfig.toPrintAttributes()
            renderWebViewToPrintAdapter(ctx, finalHtml, fileName) { adapter ->
                pm.print(fileName, adapter, attrs)
            }
        }
    }

    public actual suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val needsHtmlEngine =
                document.pages.any { page ->
                    page.elements.any { it is PdfElement.Html } || hasComplexContent(page)
                }
            val bytes: ByteArray =
                if (needsHtmlEngine) {
                    val html = document.toHtml()
                    renderHtmlToBytes(html, document.config)
                } else {
                    AndroidNativePdfRenderer(document, options, progress).render()
                }
            progress.tryEmit(PdfProgressEvent.Finalizing)
            val result = dispatchOutput(bytes, output, suggestedFileName = ensurePdfFileName(fileName))
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
        fileName: String,
    ): PdfResult {
        progress.tryEmit(PdfProgressEvent.Started)
        return try {
            val finalHtml = html.injectPageConfigCss(pageConfig)
            val bytes = renderHtmlToBytes(finalHtml, pageConfig)
            val result = dispatchOutput(bytes, output, suggestedFileName = ensurePdfFileName(fileName))
            progress.tryEmit(PdfProgressEvent.Complete(bytes.size))
            result
        } catch (e: Throwable) {
            val err = e.toPdfError()
            progress.tryEmit(PdfProgressEvent.Failed(err))
            PdfResult.Failure(err)
        }
    }

    public actual fun progressFlow(): Flow<PdfProgressEvent> = progress.asSharedFlow()

    private fun requireContext(): Context = context ?: throw PdfError.InvalidInput(
        "PdfGenerator requires a Context — call setContext() or use createPdfGenerator(context)",
    )

    /**
     * Render [html] to a real, multi-page PDF byte array.
     *
     * Loads the HTML into an offscreen [WebView], lays it out at the page's point-width, then
     * paints vertical page-height slices onto [android.graphics.pdf.PdfDocument] pages (the same
     * engine the DSL path uses). This is the reflection-free route: it sidesteps
     * `PrintDocumentAdapter`'s package-private `LayoutResultCallback`/`WriteResultCallback` (the
     * reason HTML→bytes was previously unavailable). Runs on the main thread — WebView requires it.
     *
     * With real bytes in hand, every [PdfOutput] flows through [dispatchOutput], so Share / File /
     * Uri / Print / ByteArray all work (fixes #152).
     */
    private suspend fun renderHtmlToBytes(html: String, pageConfig: PageConfig): ByteArray =
        withContext(Dispatchers.Main) {
            requireContext() // fail fast with a clear error before spinning up a WebView
            val (pageWidthPt, pageHeightPt) = pageConfig.toPointDimensions()
            var webView: WebView? = WebView(requireContext())
            val wv = webView ?: throw PdfError.EngineFailure(IllegalStateException("WebView unavailable"))
            try {
                wv.settings.javaScriptEnabled = false
                wv.settings.textZoom = 100 // no auto font scaling — 1 CSS px maps to 1 PDF point
                awaitHtmlLoaded(wv, html)
                wv.measure(
                    View.MeasureSpec.makeMeasureSpec(pageWidthPt, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                wv.layout(0, 0, wv.measuredWidth, wv.measuredHeight)
                val contentHeightPt = wv.measuredHeight.coerceAtLeast(pageHeightPt)
                val pageCount = ceil(contentHeightPt.toDouble() / pageHeightPt).toInt().coerceAtLeast(1)

                val doc = android.graphics.pdf.PdfDocument()
                try {
                    for (i in 0 until pageCount) {
                        val info =
                            android.graphics.pdf.PdfDocument.PageInfo
                                .Builder(pageWidthPt, pageHeightPt, i + 1)
                                .create()
                        val page = doc.startPage(info)
                        page.canvas.apply {
                            save()
                            clipRect(0, 0, pageWidthPt, pageHeightPt)
                            translate(0f, -(i.toFloat() * pageHeightPt))
                            wv.draw(this)
                            restore()
                        }
                        doc.finishPage(page)
                    }
                    ByteArrayOutputStream().use { out ->
                        doc.writeTo(out)
                        out.toByteArray()
                    }
                } finally {
                    doc.close()
                }
            } finally {
                wv.stopLoading()
                wv.destroy()
                webView = null
            }
        }

    /** Load [html] into [wv] and suspend until `onPageFinished` (or a WebView error). */
    private suspend fun awaitHtmlLoaded(wv: WebView, html: String): Unit = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { wv.stopLoading() }
        wv.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?,
                ) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            PdfError.EngineFailure(IllegalStateException("WebView: $description")),
                        )
                    }
                }
            }
        wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private suspend fun renderWebViewToPrintAdapter(
        ctx: Context,
        html: String,
        documentName: String,
        block: (PrintDocumentAdapter) -> Unit,
    ): Unit = suspendCancellableCoroutine { cont ->
        var webView: WebView? = WebView(ctx)
        cont.invokeOnCancellation {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        val wv = webView ?: return@suspendCancellableCoroutine
        wv.settings.javaScriptEnabled = false
        wv.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val adapter =
                            view?.createPrintDocumentAdapter(documentName)
                                ?: throw PdfError.EngineFailure(IllegalStateException("Adapter null"))
                        block(adapter)
                        if (cont.isActive) cont.resume(Unit)
                    } catch (e: Throwable) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?,
                ) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            PdfError.EngineFailure(IllegalStateException("WebView: $description")),
                        )
                    }
                }
            }
        wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun dispatchOutput(bytes: ByteArray, output: PdfOutput, suggestedFileName: String): PdfResult {
        val ctx = requireContext()
        return when (output) {
            is PdfOutput.File -> {
                File(output.path).writeBytes(bytes)
                PdfResult.Success(byteCount = bytes.size)
            }

            PdfOutput.ByteArrayOutput -> {
                PdfResult.Success(bytes = bytes, byteCount = bytes.size)
            }

            is PdfOutput.Uri -> {
                val file = File(ctx.cacheDir, suggestedFileName).apply { writeBytes(bytes) }
                val uri =
                    try {
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    } catch (e: IllegalArgumentException) {
                        throw PdfError.PermissionDenied(
                            "FileProvider authority '${ctx.packageName}.fileprovider' is not declared. " +
                                "Declare it in your app's AndroidManifest.xml.",
                        )
                    }
                output.callback(uri.toString())
                PdfResult.Success(uri = uri.toString(), byteCount = bytes.size)
            }

            PdfOutput.Share -> {
                val file = File(ctx.cacheDir, suggestedFileName).apply { writeBytes(bytes) }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                ctx.startActivity(
                    Intent
                        .createChooser(intent, "Share PDF")
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                )
                PdfResult.Success(uri = uri.toString(), byteCount = bytes.size)
            }

            PdfOutput.Print -> {
                val pm = ctx.getSystemService<PrintManager>()!!
                val tmpFile = File(ctx.cacheDir, suggestedFileName).apply { writeBytes(bytes) }
                pm.print(
                    suggestedFileName,
                    BytesPrintAdapter(tmpFile),
                    PrintAttributes.Builder().build(),
                )
                PdfResult.Success(byteCount = bytes.size)
            }

            PdfOutput.Save -> {
                throw PdfError.UnsupportedFeature(
                    "PdfOutput.Save on Android requires hosting Activity wiring via " +
                        "ActivityResultLauncher<Intent>. Use PdfOutput.File(path) or PdfOutput.Share instead.",
                )
            }
        }
    }

    private fun hasComplexContent(page: PdfPage): Boolean = page.elements.any { it is PdfElement.Html }
}

/** PageConfig → (widthPt, heightPt) in PostScript points (1/72"), honoring orientation. */
internal fun PageConfig.toPointDimensions(): Pair<Int, Int> {
    val mmToPt = 72.0 / 25.4
    val widthPt = (size.widthMm * mmToPt).roundToInt()
    val heightPt = (size.heightMm * mmToPt).roundToInt()
    return if (orientation == Orientation.LANDSCAPE) heightPt to widthPt else widthPt to heightPt
}

/** Map PageConfig → Android PrintAttributes. */
internal fun PageConfig.toPrintAttributes(): PrintAttributes {
    val landscape = orientation == Orientation.LANDSCAPE

    fun applyLandscape(s: PrintAttributes.MediaSize) = if (landscape) s.asLandscape() else s
    val mediaSize =
        when (size) {
            PageSize.A4 -> applyLandscape(PrintAttributes.MediaSize.ISO_A4)
            PageSize.A3 -> applyLandscape(PrintAttributes.MediaSize.ISO_A3)
            PageSize.A5 -> applyLandscape(PrintAttributes.MediaSize.ISO_A5)
            PageSize.LETTER -> applyLandscape(PrintAttributes.MediaSize.NA_LETTER)
            PageSize.LEGAL -> applyLandscape(PrintAttributes.MediaSize.NA_LEGAL)
            else -> PrintAttributes.MediaSize.ISO_A4
        }
    val marginMils = (margins.top * 39.3701).toInt() // approximate; per-edge would need custom Margins
    return PrintAttributes
        .Builder()
        .setMediaSize(mediaSize)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .setMinMargins(PrintAttributes.Margins(marginMils, marginMils, marginMils, marginMils))
        .build()
}

/** Minimal adapter that streams bytes from a file when PrintManager wants them. */
private class BytesPrintAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) {
        callback?.onLayoutFinished(
            PrintDocumentInfo
                .Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        FileOutputStream(destination?.fileDescriptor).use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
    }
}
