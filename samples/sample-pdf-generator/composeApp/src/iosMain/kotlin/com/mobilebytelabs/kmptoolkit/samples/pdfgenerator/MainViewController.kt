/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import androidx.compose.ui.window.ComposeUIViewController
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGenerator
import platform.UIKit.UIViewController

/**
 * Exposed to the iOSApp Xcode project. Wire from SwiftUI/UIKit via:
 *
 * ```swift
 * import ComposeApp
 * struct ContentView: View {
 *   var body: some View {
 *     ComposeView()
 *   }
 * }
 * struct ComposeView: UIViewControllerRepresentable {
 *   func makeUIViewController(context: Context) -> UIViewController {
 *     MainViewControllerKt.MainViewController()
 *   }
 *   func updateUIViewController(_ uiVC: UIViewController, context: Context) {}
 * }
 * ```
 */
fun MainViewController(): UIViewController {
    val generator = PdfGenerator()
    return ComposeUIViewController { SamplePdfGeneratorApp(generator) }
}
