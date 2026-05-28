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

import platform.WatchConnectivity.WCSession

/**
 * watchOS `Share` — v0.3 (inter-app-comms-real-native-impls Phase 2):
 *
 * Text/Url → WCSession.transferUserInfo Handoff to paired iPhone. The companion app on
 * iOS is responsible for handling the userInfo dictionary (kind: "share", type, value)
 * and presenting the native UIActivityViewController.
 *
 * Image/File/Multi → UnsupportedPlatform per ADR-09 (WCSession dict-payload only; binary
 * file transfer requires transferFile + companion-app receiver — out of scope for v0.3).
 *
 * Behavior when WCSession is unavailable (no paired iPhone in range, session not activated):
 * returns `ShareResult.Failed(ShareError.NoHandler)`.
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Text -> handoffToIPhone(mapOf("kind" to "share", "type" to "text", "value" to payload.content))
        is SharePayload.Url -> handoffToIPhone(mapOf("kind" to "share", "type" to "url", "value" to payload.href))
        // ADR-09: WCSession dict-payload only; binary transfer is companion-app territory
        is SharePayload.Image, is SharePayload.File, is SharePayload.Multi ->
            ShareResult.Failed(ShareError.UnsupportedPlatform)
    }

    private fun handoffToIPhone(userInfo: Map<String, Any?>): ShareResult {
        val session = WCSession.defaultSession
        if (!session.isReachable()) {
            return ShareResult.Failed(ShareError.NoHandler)
        }
        @Suppress("UNCHECKED_CAST")
        session.transferUserInfo(userInfo as Map<Any?, *>)
        return ShareResult.Completed
    }
}
