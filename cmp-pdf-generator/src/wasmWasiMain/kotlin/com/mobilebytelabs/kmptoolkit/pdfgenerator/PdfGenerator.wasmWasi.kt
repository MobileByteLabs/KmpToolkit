/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Tier-3 platform stub for wasmWasi. PDF generation is not implemented in v0.x. Every method
 * throws [PdfError.UnsupportedPlatform("wasmWasi")].
 */
@ExperimentalPdfGeneratorApi
public actual class PdfGenerator public actual constructor() {

    public actual suspend fun generateAndSharePdf(
        htmlContent: String,
        fileName: String,
        pageConfig: PageConfig,
    ) {
        throw PdfError.UnsupportedPlatform("wasmWasi")
    }

    public actual suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
    ): PdfResult = PdfResult.Failure(PdfError.UnsupportedPlatform("wasmWasi"))

    public actual suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig,
        branding: PdfBranding,
        options: PdfGeneratorOptions,
    ): PdfResult = PdfResult.Failure(PdfError.UnsupportedPlatform("wasmWasi"))

    public actual fun progressFlow(): Flow<PdfProgressEvent> = emptyFlow()
}
