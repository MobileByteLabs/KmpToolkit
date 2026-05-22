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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate

/**
 * Render-time options.
 *
 * @param deterministic When true, the renderer freezes timestamps + object IDs for byte-stable
 *   output. Use in snapshot tests.
 * @param fixedDate When [deterministic] is true and a generation date appears in the output,
 *   use this date. Null falls back to a fixed epoch.
 * @param dpi DPI for raster image embedding. Default 300.
 * @param compress Compress PDF object streams. Default true.
 */
@ExperimentalPdfGeneratorApi
public data class PdfGeneratorOptions(
    public val deterministic: Boolean = false,
    public val fixedDate: LocalDate? = null,
    public val dpi: Int = 300,
    public val compress: Boolean = true,
) {
    init {
        require(dpi in 72..600) { "dpi must be 72..600" }
    }
}

/**
 * Where the rendered PDF should land.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfOutput {
    /** Write to a specific filesystem path. */
    public data class File(
        public val path: String,
    ) : PdfOutput()

    /** Return bytes in-memory via [PdfResult.Success.bytes]. */
    public object ByteArrayOutput : PdfOutput()

    /** Write to platform-appropriate storage and pass a content URI to [callback]. */
    public data class Uri(
        public val callback: (String) -> Unit,
    ) : PdfOutput()

    /** Launch native share intent (Android `ACTION_SEND`, iOS `UIActivityViewController`, …). */
    public object Share : PdfOutput()

    /** Launch native print dialog. */
    public object Print : PdfOutput()

    /** Launch native save-as dialog (Android SAF, iOS document picker, NSSavePanel, browser download). */
    public object Save : PdfOutput()
}

/**
 * Result of a [PdfGenerator.generate] call.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfResult {
    /**
     * @param bytes Non-null when [PdfOutput.ByteArrayOutput] was used; null otherwise.
     * @param uri Non-null when [PdfOutput.Uri] was used; null otherwise.
     * @param byteCount Always populated when bytes were available.
     */
    public data class Success(
        public val bytes: ByteArray? = null,
        public val uri: String? = null,
        public val byteCount: Int = 0,
    ) : PdfResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return uri == other.uri && byteCount == other.byteCount &&
                (bytes?.contentEquals(other.bytes) ?: (other.bytes == null))
        }

        override fun hashCode(): Int {
            var r = bytes?.contentHashCode() ?: 0
            r = 31 * r + (uri?.hashCode() ?: 0)
            r = 31 * r + byteCount
            return r
        }
    }

    public data class Failure(
        public val error: PdfError,
    ) : PdfResult()
}

/**
 * Cross-platform PDF generator. Each platform supplies an `actual class`.
 *
 * Two entry points:
 *
 *  - [generateAndSharePdf] — mifos-x back-compatible. HTML in, share intent / file save out.
 *  - [generate] — generic v0.1 entry. Takes a [PdfDocument] (DSL or HTML-via-template), routes
 *    to the requested [PdfOutput].
 *
 * The module declares the targets supported by `kotlinx-html` + Kotlin/Native Tier-1:
 * Android, iOS (3 archs), macOS (2 archs), JVM, JS, wasmJs. Adding more targets
 * requires verifying upstream library compatibility first.
 */
@ExperimentalPdfGeneratorApi
public expect class PdfGenerator() {
    /**
     * mifos-x back-compat — HTML in, share/print/save out. Behavior is platform-defined.
     */
    public suspend fun generateAndSharePdf(
        htmlContent: String,
        fileName: String,
        pageConfig: PageConfig,
    )

    /**
     * Render a [PdfDocument] to the chosen [output].
     */
    public suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
    ): PdfResult

    /**
     * Render raw [html] to the chosen [output], with optional branding overrides.
     */
    public suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig = PageConfig(),
        branding: PdfBranding = PdfBranding.none(),
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
    ): PdfResult

    /**
     * Hot flow of render progress events. Default implementation emits nothing.
     * Tier-1 platforms emit Started / PageRendered / Finalizing / Complete.
     */
    public fun progressFlow(): Flow<PdfProgressEvent>
}

/**
 * Default no-op progress flow — referenced by platform impls that don't yet emit events.
 */
@ExperimentalPdfGeneratorApi
internal fun emptyProgressFlow(): Flow<PdfProgressEvent> = emptyFlow()
