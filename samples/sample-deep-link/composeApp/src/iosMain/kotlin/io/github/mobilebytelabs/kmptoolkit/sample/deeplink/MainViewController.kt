package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.ui.window.ComposeUIViewController
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkAppleHelper

fun MainViewController() = ComposeUIViewController { App() }

/**
 * Called from Swift AppDelegate:
 *
 * ```swift
 * func application(_ app: UIApplication, open url: URL,
 *                  options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
 *     MainViewControllerKt.handleDeepLink(url: url.absoluteString)
 *     return true
 * }
 * ```
 */
fun handleDeepLink(url: String) = DeepLinkAppleHelper.handle(url)
