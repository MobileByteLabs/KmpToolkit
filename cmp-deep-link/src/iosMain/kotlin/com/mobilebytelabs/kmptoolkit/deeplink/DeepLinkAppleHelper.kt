package com.mobilebytelabs.kmptoolkit.deeplink

import platform.Foundation.NSURL
import kotlin.experimental.ExperimentalObjCName

/**
 * iOS entry point for deep link handling. Call from AppDelegate or SwiftUI `.onOpenURL`.
 *
 * ## AppDelegate (UIKit)
 *
 * ```swift
 * func application(_ app: UIApplication, open url: URL,
 *                  options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
 *     DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString ?? "")
 *     return true
 * }
 * ```
 *
 * ## SwiftUI
 *
 * ```swift
 * @main struct MyApp: App {
 *     var body: some Scene {
 *         WindowGroup {
 *             ContentView()
 *                 .onOpenURL { url in
 *                     DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString)
 *                 }
 *         }
 *     }
 * }
 * ```
 *
 * ## Universal Links (NSUserActivity)
 *
 * ```swift
 * func application(_ application: UIApplication,
 *                  continue userActivity: NSUserActivity,
 *                  restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
 *     if let url = userActivity.webpageURL {
 *         DeepLinkAppleHelper.shared.handleUserActivity(webpageUrl: url.absoluteString)
 *     }
 *     return true
 * }
 * ```
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("DeepLinkAppleHelper")
object DeepLinkAppleHelper {

    /**
     * Handle a URL scheme or Universal Link received from the OS.
     *
     * @param url Absolute URL string (e.g. `myapp://product/42` or `https://example.com/product/42`).
     */
    @ObjCName("handleUrl")
    fun handle(url: String) {
        if (url.isNotBlank()) DeepLinkHandler.handle(url)
    }

    /**
     * Handle a Universal Link delivered via [NSUserActivity].
     *
     * @param webpageUrl The `webpageURL` from the NSUserActivity, or `null` if absent.
     */
    @ObjCName("handleUserActivity")
    fun handleUserActivity(webpageUrl: String?) {
        webpageUrl?.let { if (it.isNotBlank()) DeepLinkHandler.handle(it) }
    }
}
