/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: full

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

            is SharePayload.Image -> {
                // Web Share Level 2: build a File from the bytes + pass via `files: [file]`.
                // 2026-06-01 — Replaces the v0.1 fall-through (sub-plan 02 T3).
                val file = createJsFile(payload.bytes, payload.mimeType, payload.filename ?: "shared")
                if (file != null && canShareFiles(file)) {
                    val filesArr: dynamic = js("[]")
                    js("filesArr.push(file)")
                    data["files"] = filesArr
                    hasContent = true
                } else {
                    // Browser doesn't support file share (e.g. Firefox < 130, older Safari) —
                    // fall through to clipboard (Image bytes can't be clipboard'd; surfaces NoHandler).
                    return null
                }
            }

            is SharePayload.File -> {
                // File payload references an existing URI; Web Share API expects File objects,
                // not URIs. Without fetching the bytes (which would require ambient CORS perms),
                // fall back to clipboard via payloadAsText (returns null → NoHandler).
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

/**
 * Construct a [File] object from a Kotlin [ByteArray]. Returns `null` if the browser
 * doesn't support File / Blob (very old browsers) or if construction fails.
 *
 * Kotlin/JS `ByteArray` is backed by `Int8Array` at runtime; wrapping in `Uint8Array`
 * shares the underlying buffer (no copy).
 */
private fun createJsFile(bytes: ByteArray, mime: String, name: String): dynamic = try {
    js(
        """
        (function(b, m, n) {
            if (typeof File !== 'function' || typeof Blob !== 'function') return null;
            try {
                var u8 = new Uint8Array(b);
                return new File([u8], n, { type: m });
            } catch (e) { return null; }
        })(bytes, mime, name)
        """,
    )
} catch (_: Throwable) {
    null
}

private fun canShareFiles(file: dynamic): Boolean = try {
    js(
        """
        (function(f) {
            if (typeof navigator === 'undefined' || typeof navigator.canShare !== 'function') return false;
            try { return navigator.canShare({ files: [f] }); } catch (e) { return false; }
        })(file)
        """,
    ) as Boolean
} catch (_: Throwable) {
    false
}

private fun readJsErrorName(e: Throwable): String? = try {
    @Suppress("UNCHECKED_CAST")
    (e.asDynamic().name as? String)
} catch (_: Throwable) {
    null
}
