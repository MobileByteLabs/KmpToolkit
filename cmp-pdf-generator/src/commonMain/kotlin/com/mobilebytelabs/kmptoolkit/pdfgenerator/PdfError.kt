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

import kotlinx.coroutines.CancellationException

/**
 * Sealed hierarchy of PDF generation failures.
 *
 * Use with exhaustive `when`:
 * ```
 * when (val r = generator.generate(doc, output)) {
 *     is PdfResult.Success -> publish(r.bytes)
 *     is PdfResult.Failure -> when (val e = r.error) {
 *         is PdfError.PermissionDenied -> requestPermission()
 *         else -> showError(e.message)
 *     }
 * }
 * ```
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    /** Underlying engine (WebView, PDFBox, pdf-lib, …) failed. */
    public class EngineFailure(cause: Throwable) :
        PdfError("PDF engine failed: ${cause.message ?: cause::class.simpleName}", cause)

    /** Coroutine was cancelled mid-render. */
    public object CancellationError : PdfError("PDF generation cancelled")

    /** Host-app permission denied (e.g. Android FileProvider authority missing). */
    public class PermissionDenied(message: String) : PdfError(message)

    /** File I/O failure during write / read. */
    public class IoError(cause: Throwable) :
        PdfError("PDF I/O failed: ${cause.message ?: cause::class.simpleName}", cause)

    /** Requested feature exists in the API but is not supported by this platform's engine. */
    public class UnsupportedFeature(public val feature: String) :
        PdfError("Unsupported feature on this platform: $feature")

    /** Input failed validation (negative margin, empty doc, malformed HTML, …). */
    public class InvalidInput(public val reason: String) : PdfError("Invalid input: $reason")
}

/**
 * Wrap an arbitrary throwable as a [PdfError]. Cancellation is preserved.
 */
@ExperimentalPdfGeneratorApi
public fun Throwable.toPdfError(): PdfError = when (this) {
    is PdfError -> this
    is CancellationException -> PdfError.CancellationError
    else -> PdfError.EngineFailure(this)
}
