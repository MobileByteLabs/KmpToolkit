// DeepLinkPlugin.swift — cmp-deep-link SwiftUI adapter
//
// Drop this file into your Xcode project to reduce deep link wiring to a single line.
// Requires: iOS 14+ / macOS 11+ / tvOS 14+
//
// USAGE (SwiftUI):
//   @main struct MyApp: App {
//       var body: some Scene {
//           WindowGroup {
//               ContentView()
//                   .deepLinkAutoHandle()   // ← only line needed
//           }
//       }
//   }
//
// The OS delivers URLs through UIKit/AppKit callbacks that Kotlin/Native cannot intercept,
// so this one-line wiring is the irreducible minimum for SwiftUI apps.
//
// UIKit (AppDelegate) consumers use DeepLinkAppleHelper.shared.handleUrl(url:) directly —
// see docs/IOS.md for the 3-line AppDelegate snippet.

import SwiftUI

public extension View {
    /// Attach to your root SwiftUI view to enable automatic deep link handling.
    ///
    /// Handles both custom URL schemes (`myapp://`) and Universal Links (`https://`).
    /// Internally calls `DeepLinkAppleHelper.shared.handleUrl(url:)` which forwards
    /// the URI to the shared `DeepLinkHandler` singleton in Kotlin.
    ///
    /// - Returns: A view that reacts to deep link URLs delivered by the OS.
    func deepLinkAutoHandle() -> some View {
        self.onOpenURL { url in
            DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString)
        }
    }
}
