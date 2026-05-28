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

/**
 * tvOS `Share` — v0.3 status (inter-app-comms-real-native-impls Phase 2):
 *
 * The `cmp-share/swift/CmpShareTvosBridge.swift` ship-source-file is provided for consumer
 * apps that want to bridge `UIPasteboard.general` themselves (K/N tvOS bindings don't expose
 * it directly). However, dynamic ObjC dispatch from Kotlin/Native to a consumer-shipped
 * Swift class requires a `@ExportObjCClass` / cinterop bridge declaration that wasn't
 * feasible in v0.3 timeline (objc_msgSend K/N binding is non-variadic; needs custom interop
 * .def per ADR-09).
 *
 * Therefore tvOS continues to return `UnsupportedPlatform` for all payloads at v0.3. The
 * .swift file is shipped as a v0.4 candidate-API artifact — consumers wanting tvOS share
 * today can call it directly from their app code.
 *
 * ADR-09 row: cmp-share / Share.tvos.kt / SharePayload.* / UnsupportedPlatform / "K/N
 * tvOS lacks UIPasteboard binding; dynamic Swift dispatch deferred to v0.4 cinterop work"
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        // ADR-09: tvOS K/N bindings lack UIPasteboard; Swift bridge dynamic dispatch deferred
        ShareResult.Failed(ShareError.UnsupportedPlatform)
}
