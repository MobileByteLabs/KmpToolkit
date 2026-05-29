// =============================================================================
// CmpShareTvosBridge.swift
// =============================================================================
//
// Ship-source-file Swift bridge for cmp-share on tvOS (per ADR-04 mechanism).
//
// Drop this file into your tvOS app's Xcode target. Kotlin/Native bindings on
// tvOS do not expose `UIPasteboard` (as of Kotlin 2.3.10), even though the
// Objective-C API exists on tvOS 9+. This Swift adapter bridges UIPasteboard to
// the Kotlin layer via @objc-exposed methods.
//
// Wiring (consumer side):
//   1. Copy this file into your tvOS app's Xcode target alongside other Swift sources.
//   2. No further setup needed — Kotlin's CmpShareTvosBridgeCallback.shared.handler
//      resolves to this class at runtime via the ObjC runtime.
//
// Authored: 2026-05-28 by inter-app-comms-real-native-impls Phase 2 T1.
// =============================================================================

import Foundation
import UIKit  // tvOS UIKit provides UIPasteboard

@objc public class CmpShareTvosBridge: NSObject {

    @objc public static let shared = CmpShareTvosBridge()

    /// Write a text payload to the system pasteboard.
    /// Returns `true` on success, `false` if UIPasteboard.general is unavailable.
    @objc public func setPasteboardString(_ value: String) -> Bool {
        UIPasteboard.general.string = value
        return true
    }

    /// Write a URL payload to the system pasteboard.
    /// Returns `true` if the URL parses; `false` if malformed.
    @objc public func setPasteboardURL(_ url: String) -> Bool {
        guard let parsed = URL(string: url) else { return false }
        UIPasteboard.general.url = parsed
        return true
    }
}
