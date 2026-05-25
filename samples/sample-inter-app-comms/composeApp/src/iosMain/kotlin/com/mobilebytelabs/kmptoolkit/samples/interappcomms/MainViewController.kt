/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.interappcomms

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Exposed to the iOSApp Xcode project (when one is added). Wire from SwiftUI via:
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
 *
 * No iosApp xcodeproj shipped in v0.1 sample — see sub-plan 07 follow-up.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { SampleInterAppCommsApp() }
