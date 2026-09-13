/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Injectable PDF generation — the type to depend on from a ViewModel or repository.
 *
 * ## Why an interface when [PdfGenerator] already exists
 * [PdfGenerator] is an `expect class`, so code calling it directly cannot be substituted. That
 * matters more here than almost anywhere else in the toolkit: generating a PDF means invoking a
 * real renderer, and on several targets it also raises a share sheet, a print dialog or a save
 * panel. A test that exercises an export flow must not do any of that.
 *
 * [PdfManager] is the same capability behind an injectable type; `FakePdfManager` gives tests a
 * scripted result with no renderer involved. [PdfGenerator] stays public and unchanged.
 *
 * ## Using
 * ```kotlin
 * class InvoiceViewModel(private val pdf: PdfManager) : ViewModel() {
 *     fun export(invoice: Invoice) = viewModelScope.launch {
 *         when (val result = pdf.generate(invoice.toPdfDocument(), PdfOutput.Share)) {
 *             is PdfResult.Success -> Unit
 *             is PdfResult.Failure -> showError(result.error)
 *         }
 *     }
 * }
 * ```
 */
public interface PdfManager {

    /** Render [document] to [output]. */
    public suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
        fileName: String = DEFAULT_PDF_FILE_NAME,
    ): PdfResult

    /**
     * Render raw [html] to [output].
     *
     * On the five targets with no HTML renderer — tvOS, watchOS, Linux, Windows, WASI — the markup
     * is stripped to text rather than laid out. Check [supportsHtmlLayout] if the difference
     * matters to you.
     */
    public suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig = PageConfig(),
        branding: PdfBranding = PdfBranding.none(),
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
        fileName: String = DEFAULT_PDF_FILE_NAME,
    ): PdfResult

    /** Convenience for the commonest case: a document straight to bytes. */
    public suspend fun toBytes(
        document: PdfDocument,
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
    ): ByteArray? = (generate(document, PdfOutput.ByteArrayOutput, options) as? PdfResult.Success)?.bytes

    /**
     * Whether this target lays out HTML, or only renders text.
     *
     * `false` on tvOS, watchOS, Linux, Windows and WASI, where generation goes through the
     * dependency-free [TextPdfWriter]. Read it before offering an HTML-templated export, rather
     * than shipping a document whose styling silently vanished.
     */
    public val supportsHtmlLayout: Boolean

    /** Render progress. Tier-1 targets emit Started / PageRendered / Finalizing / Complete. */
    public fun progressFlow(): Flow<PdfProgressEvent>
}

/**
 * The one [PdfManager] — for every target.
 *
 * Wraps a [PdfGenerator]; all per-target behaviour lives there.
 */
public class PdfManagerImpl(private val generator: PdfGenerator = PdfGenerator()) : PdfManager {

    override val supportsHtmlLayout: Boolean get() = platformSupportsHtmlLayout

    override suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult = generator.generate(document, output, options, fileName)
        .also { report("pdf_generated", it, pages = document.pages.size, html = false) }

    override suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig,
        branding: PdfBranding,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult = generator.generateFromHtml(html, output, pageConfig, branding, options, fileName)
        .also { report("pdf_generated", it, pages = 0, html = true) }

    override fun progressFlow(): Flow<PdfProgressEvent> = generator.progressFlow()

    /**
     * Reports the result CLASS and document SIZE, never the content.
     *
     * A PDF body is the most sensitive thing this library touches — statements, reports, invoices.
     * Nothing derived from [PdfDocument] content or the file name leaves here.
     */
    private fun report(event: String, result: PdfResult, pages: Int, html: Boolean) {
        observeLifecycle(
            cmpMetadata(),
            event,
            mapOf(
                "result" to result::class.simpleName,
                "pages" to pages,
                "source" to if (html) "html" else "builder",
            ),
        )
    }
}

/**
 * Whether THIS target lays out HTML.
 *
 * `true` wherever a real renderer exists (Android, iOS, macOS, JVM, JS, wasmJs); `false` on the
 * five that fall back to [TextPdfWriter].
 */
public expect val platformSupportsHtmlLayout: Boolean
