/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobilebytelabs.kmptoolkit.pdfgenerator.createPdfGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val generator = createPdfGenerator(applicationContext)
        setContent {
            SamplePdfGeneratorApp(generator)
        }
    }
}
