/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Entry point consumed by the iosApp Xcode project:
 *
 * ```swift
 * import ComposeApp
 * struct ContentView: View {
 *   var body: some View { ComposeView() }
 * }
 * struct ComposeView: UIViewControllerRepresentable {
 *   func makeUIViewController(context: Context) -> UIViewController {
 *     MainViewControllerKt.MainViewController()
 *   }
 *   func updateUIViewController(_ uiVC: UIViewController, context: Context) {}
 * }
 * ```
 */
fun MainViewController(): UIViewController = ComposeUIViewController { SampleToolkitApp() }
