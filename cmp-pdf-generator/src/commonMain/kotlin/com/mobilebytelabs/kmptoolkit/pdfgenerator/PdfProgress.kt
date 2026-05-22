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

/**
 * Progress events emitted by [PdfGenerator.progressFlow] during a render.
 *
 * Lifecycle:
 *   Started → (PageRendered)* → Finalizing → Complete
 *   Started → … → Failed
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfProgressEvent {
    /** Render started. Emitted once at the beginning. */
    public object Started : PdfProgressEvent()

    /**
     * A page was rendered. [total] is null when the renderer can't predict total pages
     * (HTML-based engines pre-pagination).
     */
    public data class PageRendered(public val pageNum: Int, public val total: Int?) : PdfProgressEvent()

    /** Last pass before producing output bytes (metadata, ToC, signature). */
    public object Finalizing : PdfProgressEvent()

    /** Successful completion. [byteCount] is the size of the final PDF. */
    public data class Complete(public val byteCount: Int) : PdfProgressEvent()

    /** Render failed. The error is also returned by the generator call as [PdfResult.Failure]. */
    public data class Failed(public val error: PdfError) : PdfProgressEvent()
}
