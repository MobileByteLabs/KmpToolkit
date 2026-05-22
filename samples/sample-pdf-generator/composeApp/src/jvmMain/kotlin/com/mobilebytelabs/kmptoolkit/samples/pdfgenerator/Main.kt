/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGenerator

fun main() =
    application {
        val generator = remember { PdfGenerator() }
        Window(
            onCloseRequest = ::exitApplication,
            title = "PDF Generator Sample",
            state = rememberWindowState(width = 600.dp, height = 800.dp),
        ) {
            SamplePdfGeneratorApp(generator)
        }
    }

// `remember` reference outside Composable scope — JVM main creates instance once.
private fun <T> remember(producer: () -> T): T = producer()
