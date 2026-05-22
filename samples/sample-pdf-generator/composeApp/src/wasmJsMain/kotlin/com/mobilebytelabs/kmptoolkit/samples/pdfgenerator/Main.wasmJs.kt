/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import androidx.compose.ui.window.ComposeViewport
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGenerator
import kotlinx.browser.document

fun main() {
    val generator = PdfGenerator()
    ComposeViewport(document.body!!) {
        SamplePdfGeneratorApp(generator)
    }
}
