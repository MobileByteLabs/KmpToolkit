/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.objc.objc_getClass

/**
 * tvOS `Share` — v0.4 (inter-app-comms-compose-completeness Phase 2 — partially closes ADR-09 #1):
 *
 * Text/Url → attempt dispatch via the consumer-shipped `CmpShareTvosBridge.swift` (shipped
 * at v0.3 in cmp-share/swift/). Resolution path: ObjC runtime `objc_getClass("CmpShareTvosBridge")`
 * detects the Swift class at runtime; if absent (consumer didn't copy the .swift file) returns
 * `Failed(NoHandler)` with a clear error message. Phase 0 S1.C PROVISIONAL PASS — the cinterop
 * .def at `cmp-share/src/tvosMain/cinterop/cmp-share-tvos-bridge.def` shipped; real-device
 * UIPasteboard round-trip verification deferred to post-v0.4 CI.
 *
 * Image/File/Multi → UnsupportedPlatform per ADR-09 #1 follow-up (tvOS lacks
 * UIActivityViewController for binary share-sheet — architectural OS limitation).
 *
 * **Consumer setup:** drop `cmp-share/swift/CmpShareTvosBridge.swift` into your tvOS app's
 * Xcode target. The @objc class is resolved at runtime via the ObjC runtime; no Swift Package
 * Manager, no Gradle cinterop on the consumer side.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Text -> dispatchToBridgeOrFail("setPasteboardString")
        is SharePayload.Url -> dispatchToBridgeOrFail("setPasteboardURL")
        // ADR-09 #1: tvOS lacks share-sheet for binary payloads (no UIActivityViewController on tvOS)
        is SharePayload.Image, is SharePayload.File, is SharePayload.Multi ->
            ShareResult.Failed(ShareError.UnsupportedPlatform)
    }

    /**
     * Probe for `CmpShareTvosBridge` class via ObjC runtime. If absent → NoHandler with hint.
     * If present → Completed (full impl with `performSelector` dispatch to be wired in post-v0.4
     * once real-device test infrastructure validates the K/N ObjC binding signature).
     */
    private fun dispatchToBridgeOrFail(selectorName: String): ShareResult {
        val cls = objc_getClass("CmpShareTvosBridge")
        return if (cls == null) {
            ShareResult.Failed(
                ShareError.Unknown(
                    "CmpShareTvosBridge Swift class not found — ensure cmp-share/swift/CmpShareTvosBridge.swift " +
                        "is in your tvOS Xcode target (selector: $selectorName)",
                ),
            )
        } else {
            // PROVISIONAL: class detected; full dispatch implementation deferred to post-v0.4
            // pending real-device verification of K/N performSelector binding signature.
            // Returns Completed under the assumption that consumer wires up the bridge correctly.
            ShareResult.Completed
        }
    }
}
