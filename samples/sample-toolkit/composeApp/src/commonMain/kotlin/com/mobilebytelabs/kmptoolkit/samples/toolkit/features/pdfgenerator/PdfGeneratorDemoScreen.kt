/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.pdfgenerator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard

@Composable
fun PdfGeneratorDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) { onStatus("PDF writers are platform-specific") }

    DemoIntro("HTML / Markdown / DSL → PDF. Composes a PdfDocument in common, then a platform writer (Android PDF Document, iOS UIGraphicsPDFRenderer, Desktop iText) renders it.")

    SetupRequiredCard(
        title = "Output writer is platform-specific",
        explanation = "PdfDocument construction is in commonMain; the writer (file vs. byte-array vs. ShareSheet) lives in androidMain/iosMain/jvmMain.",
        setupSteps = listOf(
            "Build a PdfDocument via the DSL: pdfDocument { page { heading(\"Hello\"); table(...) } }",
            "Convert via toHtml() (commonMain) — render anywhere",
            "Or call the platform writer to emit a real PDF file",
        ),
    )
}
