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

import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.js.json

/**
 * JS browser implementation.
 *
 * Strategy:
 * - If `navigator.share` is available (mobile Safari 12.1+, Chrome 89+): use Web Share API
 * - Else: fall back to `navigator.clipboard.writeText`
 *
 * **HARD CONSTRAINT (Phase 0 TS6)**: `.share()` MUST be called from within a user-gesture
 * handler. Browsers reject `navigator.share()` outside a user-activation call stack.
 * Returns [ShareError.UserGestureMissing] on `NotAllowedError` from the browser.
 */
@ExperimentalShareApi
public actual object Share {

    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        val shareData = buildShareData(payload, options) ?: return ShareResult.Failed(
            ShareError.Unknown("Empty share payload"),
        )

        // Web Share API path
        if (hasNavigatorShare()) {
            return try {
                navigatorShare(shareData).await()
                ShareResult.Completed
            } catch (e: Throwable) {
                val name = readJsErrorName(e)
                when (name) {
                    "AbortError" -> ShareResult.Cancelled
                    "NotAllowedError" -> ShareResult.Failed(ShareError.UserGestureMissing)
                    else -> ShareResult.Failed(ShareError.Unknown(e.message ?: name ?: "navigator.share failed"))
                }
            }
        }

        // Clipboard fallback path
        val text = payloadAsText(payload) ?: return ShareResult.Failed(ShareError.NoHandler)
        return try {
            navigatorClipboardWriteText(text).await()
            ShareResult.Completed
        } catch (e: Throwable) {
            val name = readJsErrorName(e)
            if (name == "NotAllowedError") {
                ShareResult.Failed(ShareError.UserGestureMissing)
            } else {
                ShareResult.Failed(ShareError.Unknown(e.message ?: name ?: "clipboard.writeText failed"))
            }
        }
    }

    private fun buildShareData(payload: SharePayload, options: ShareOptions): dynamic {
        val data: dynamic = json()
        var hasContent = false
        options.chooserTitle?.let {
            data["title"] = it
            hasContent = true
        }
        when (payload) {
            is SharePayload.Text -> {
                data["text"] = payload.content
                hasContent = true
            }

            is SharePayload.Url -> {
                data["url"] = payload.href
                hasContent = true
            }

            is SharePayload.Image, is SharePayload.File -> {
                // Web Share Level 2 file support varies; fall back to clipboard via payloadAsText.
                // (Real file sharing would need to construct File objects from bytes — deferred.)
                return null
            }

            is SharePayload.Multi -> {
                val parts = mutableListOf<String>()
                for (item in payload.items) {
                    when (item) {
                        is SharePayload.Text -> parts.add(item.content)
                        is SharePayload.Url -> parts.add(item.href)
                        else -> { /* skip image/file in Multi for v0.1 */ }
                    }
                }
                if (parts.isNotEmpty()) {
                    data["text"] = parts.joinToString("\n")
                    hasContent = true
                }
            }
        }
        return if (hasContent) data else null
    }

    private fun payloadAsText(payload: SharePayload): String? = when (payload) {
        is SharePayload.Text -> payload.content

        is SharePayload.Url -> payload.href

        is SharePayload.Image, is SharePayload.File -> null

        is SharePayload.Multi -> payload.items.mapNotNull { payloadAsText(it) }
            .takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

private fun hasNavigatorShare(): Boolean =
    js("typeof navigator !== 'undefined' && typeof navigator.share === 'function'") as Boolean

private fun navigatorShare(data: dynamic): Promise<dynamic> = js("navigator.share(data)") as Promise<dynamic>

private fun navigatorClipboardWriteText(text: String): Promise<dynamic> =
    js("navigator.clipboard.writeText(text)") as Promise<dynamic>

private fun readJsErrorName(e: Throwable): String? = try {
    @Suppress("UNCHECKED_CAST")
    (e.asDynamic().name as? String)
} catch (_: Throwable) {
    null
}
