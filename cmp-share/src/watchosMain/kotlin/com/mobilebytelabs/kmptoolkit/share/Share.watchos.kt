/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.WatchConnectivity.WCSession

/**
 * watchOS `Share` — v0.4 (inter-app-comms-compose-completeness Phase 2 — closes ADR-09 #2):
 *
 * - Text/Url → `WCSession.transferUserInfo` Handoff to paired iPhone (dict payload). Companion
 *   app on iOS handles the userInfo dictionary `{kind: "share", type, value}` and presents
 *   the native UIActivityViewController.
 * - **NEW v0.4** Image → write bytes to NSDocumentDirectory tmp file then `WCSession.transferFile()`
 *   with metadata dict (companion-app receiver dispatches based on metadata.kind).
 * - **NEW v0.4** File → directly transfer via `WCSession.transferFile()` (caller-provided URI).
 * - **NEW v0.4** Multi → per-item dispatch via first-success strategy.
 *
 * **Companion-app receiver protocol** (consumer's iOS app must implement):
 *   - WCSessionDelegate.session:didReceiveUserInfo: → handle dict payload (Text/Url)
 *   - WCSessionDelegate.session:didReceiveFile: → handle file payload (Image/File); read
 *     `file.metadata["kind"]` to dispatch to UIActivityViewController.
 *
 * Behavior when WCSession unavailable (no paired iPhone, session not activated):
 * returns `ShareResult.Failed(ShareError.NoHandler)`.
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Text -> handoffUserInfo(mapOf("kind" to "share", "type" to "text", "value" to payload.content))
        is SharePayload.Url -> handoffUserInfo(mapOf("kind" to "share", "type" to "url", "value" to payload.href))
        is SharePayload.Image -> handoffFileFromBytes(payload.bytes, payload.mimeType, payload.filename)
        is SharePayload.File -> handoffFileFromUri(payload.uri, payload.mimeType, payload.filename)
        is SharePayload.Multi -> multiShare(payload)
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
     * Materialize Image bytes to NSDocumentDirectory tmp file then transferFile() to paired iPhone.
     * The companion app's WCSessionDelegate.session:didReceiveFile: receives the file URL + metadata.
     */
    private fun handoffFileFromBytes(bytes: ByteArray, mimeType: String, filename: String?): ShareResult {
        val session = WCSession.defaultSession
        if (!session.isReachable()) return ShareResult.Failed(ShareError.NoHandler)
        val docsDir = NSFileManager.defaultManager.URLForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask,
            null,
            true,
            null,
        ) ?: return ShareResult.Failed(ShareError.NoHandler)
        val ext = filename?.substringAfterLast('.', "bin") ?: mimeType.substringAfter('/', "bin")
        val tmpUrl = docsDir.URLByAppendingPathComponent("cmp-share-${NSUUID().UUIDString}.$ext")
            ?: return ShareResult.Failed(ShareError.NoHandler)
        // Write bytes to tmpUrl
        val data: NSData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        if (!data.writeToURL(tmpUrl, atomically = true)) {
            return ShareResult.Failed(ShareError.Unknown("watchOS tmp file write failed"))
        }
        val metadata: Map<Any?, Any?> = mapOf("kind" to "share", "type" to "image", "mimeType" to mimeType)
        session.transferFile(tmpUrl, metadata = metadata)
        return ShareResult.Completed
    }

    /** Transfer a caller-provided file URI to paired iPhone via transferFile(). */
    private fun handoffFileFromUri(uri: String, mimeType: String, filename: String?): ShareResult {
        val session = WCSession.defaultSession
        if (!session.isReachable()) return ShareResult.Failed(ShareError.NoHandler)
        val nsUrl = NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)
        val metadata: Map<Any?, Any?> = mapOf(
            "kind" to "share",
            "type" to "file",
            "mimeType" to mimeType,
            "filename" to (filename ?: ""),
        )
        session.transferFile(nsUrl, metadata = metadata)
        return ShareResult.Completed
    }

    /** Per-item first-success strategy. */
    private fun multiShare(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r: ShareResult = when (item) {
                is SharePayload.Text -> handoffUserInfo(
                    mapOf(
                        "kind" to "share",
                        "type" to "text",
                        "value" to item.content,
                    ),
                )

                is SharePayload.Url -> handoffUserInfo(mapOf("kind" to "share", "type" to "url", "value" to item.href))

                is SharePayload.Image -> handoffFileFromBytes(item.bytes, item.mimeType, item.filename)

                is SharePayload.File -> handoffFileFromUri(item.uri, item.mimeType, item.filename)

                is SharePayload.Multi -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            }
            if (r is ShareResult.Completed) return r
        }
        return ShareResult.Failed(ShareError.NoHandler)
    }
}
