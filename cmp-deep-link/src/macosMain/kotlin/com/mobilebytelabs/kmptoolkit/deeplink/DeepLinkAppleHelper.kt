package com.mobilebytelabs.kmptoolkit.deeplink

import kotlin.experimental.ExperimentalObjCName

/**
 * macOS entry point for deep link handling.
 *
 * Uses the same NSURL / NSAppleEventManager surface as iOS (separate file
 * intentional — macOS and iOS AppDelegate hooks diverge in future versions).
 *
 * ## AppKit AppDelegate
 *
 * ```swift
 * func applicationWillFinishLaunching(_ notification: Notification) {
 *     NSAppleEventManager.shared().setEventHandler(
 *         self,
 *         andSelector: #selector(handleGetURLEvent(_:withReplyEvent:)),
 *         forEventClass: AEEventClass(kInternetEventClass),
 *         andEventID: AEEventID(kAEGetURL)
 *     )
 * }
 *
 * @objc func handleGetURLEvent(_ event: NSAppleEventDescriptor,
 *                              withReplyEvent: NSAppleEventDescriptor) {
 *     if let urlString = event.paramDescriptor(forKeyword: AEKeyword(keyDirectObject))?.stringValue {
 *         DeepLinkAppleHelper.shared.handleUrl(url: urlString)
 *     }
 * }
 * ```
 *
 * ## SwiftUI
 *
 * ```swift
 * WindowGroup {
 *     ContentView()
 *         .onOpenURL { url in
 *             DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString)
 *         }
 * }
 * ```
 *
 * See `docs/MACOS.md` for `Info.plist` CFBundleURLTypes configuration.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("DeepLinkAppleHelper")
object DeepLinkAppleHelper {

    @ObjCName("handleUrl")
    fun handle(url: String) {
        if (url.isNotBlank()) DeepLinkHandler.handle(url)
    }

    @ObjCName("handleUserActivity")
    fun handleUserActivity(webpageUrl: String?) {
        webpageUrl?.let { if (it.isNotBlank()) DeepLinkHandler.handle(it) }
    }
}
