/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: partial

package com.mobilebytelabs.kmptoolkit.share

import platform.WatchConnectivity.WCSession

/**
 * watchOS `Share` — v0.4 status: Text/Url via `WCSession.transferUserInfo` handoff.
 * Binary payloads (Image / File / Multi-with-binary) → `UnsupportedPlatform`.
 *
 * **Why not full binary handoff via `transferFile`**: writing the temp file requires
 * either `NSData.dataWithBytes(bytes, length)` or POSIX `fwrite` — both have
 * `NSUInteger` / `size_t` parameter shapes that resolve to different bit widths on
 * `watchosArm32` (32-bit) vs `watchosArm64` / `watchosSimulatorArm64` / `watchosDeviceArm64`
 * (64-bit). K/N's strict expect/actual matching refuses to compile when a single
 * watchOS source set targets both bit widths. Consolidating to a single watchOS
 * arch (drop `watchosArm32` from build.gradle.kts targets) would unblock the binary
 * path; that's a build-config decision deferred to a v0.5 review of legacy-Apple-Watch
 * support.
 *
 * Text + Url paths use `WCSession.transferUserInfo(dict)` which only takes Kotlin
 * primitive types → no bit-width interop. Companion iOS app handles the userInfo
 * dictionary `{kind: "share", type, value}` and presents `UIActivityViewController`.
 *
 * Behavior when WCSession unavailable: returns `ShareResult.Failed(ShareError.NoHandler)`.
 *
 * ADR-09 #2 audit refresh: closed for Text/Url; binary deferred to v0.5
 * (Q: drop watchosArm32 or wrap binary calls behind per-arch source sets?).
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Text -> handoffUserInfo(
            mapOf("kind" to "share", "type" to "text", "value" to payload.content),
        )

        is SharePayload.Url -> handoffUserInfo(
            mapOf("kind" to "share", "type" to "url", "value" to payload.href),
        )

        is SharePayload.Image,
        is SharePayload.File,
        -> ShareResult.Failed(ShareError.UnsupportedPlatform)

        is SharePayload.Multi -> multiShareTextOnly(payload)
    }

    /** Send a dict-shaped userInfo to paired iPhone via WCSession.transferUserInfo. */
    private fun handoffUserInfo(userInfo: Map<String, Any?>): ShareResult {
        val session = WCSession.defaultSession
        if (!session.isReachable()) return ShareResult.Failed(ShareError.NoHandler)
        @Suppress("UNCHECKED_CAST")
        session.transferUserInfo(userInfo as Map<Any?, *>)
        return ShareResult.Completed
    }

    /**
     * Multi-payload handler — text/url items dispatch normally; binary items inside
     * Multi return UnsupportedPlatform (same rationale as the top-level Image/File case).
     * First-success strategy across text/url items.
     */
    private fun multiShareTextOnly(multi: SharePayload.Multi): ShareResult {
        var sawText = false
        for (item in multi.items) {
            when (item) {
                is SharePayload.Text -> {
                    sawText = true
                    val r = handoffUserInfo(
                        mapOf("kind" to "share", "type" to "text", "value" to item.content),
                    )
                    if (r is ShareResult.Completed) return r
                }

                is SharePayload.Url -> {
                    sawText = true
                    val r = handoffUserInfo(
                        mapOf("kind" to "share", "type" to "url", "value" to item.href),
                    )
                    if (r is ShareResult.Completed) return r
                }

                is SharePayload.Image,
                is SharePayload.File,
                is SharePayload.Multi,
                -> { /* skip — binary or nested multi not supported on watchOS at v0.4 */ }
            }
        }
        return if (sawText) {
            ShareResult.Failed(
                ShareError.NoHandler,
            )
        } else {
            ShareResult.Failed(ShareError.UnsupportedPlatform)
        }
    }
}
