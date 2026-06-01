/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

// LD-2-coverage: full

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.js.Promise

/**
 * wasmJs implementation — Web Share API + clipboard fallback. Same shape as JS;
 * uses `@JsFun` external bindings (wasmJs has no `dynamic`).
 *
 * Same user-gesture constraint (Phase 0 TS6) as JS.
 *
 * 2026-06-01 — Added Image payload support via base64-bridged `File` construction
 * + `navigator.share({files})` (per cmp-intent-share-coverage-trueup sub-plan 02 T4).
 */
@ExperimentalShareApi
public actual object Share {

    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        // Image payload → Web Share Level 2 file share via base64 bridge
        // (ByteArray → JS Uint8Array via base64 is the simplest cross-target wasmJs interop).
        if (payload is SharePayload.Image) {
            return shareImageFile(payload, options.chooserTitle)
        }

        val text = buildShareText(payload) ?: return ShareResult.Failed(ShareError.NoHandler)
        val url = buildShareUrl(payload)
        val title = options.chooserTitle

        if (hasNavigatorShare()) {
            return try {
                navigatorShare(title, text, url).await<JsAny?>()
                ShareResult.Completed
            } catch (e: Throwable) {
                classifyJsError(e)
            }
        }

        return try {
            navigatorClipboardWriteText(text).await<JsAny?>()
            ShareResult.Completed
        } catch (e: Throwable) {
            classifyJsError(e)
        }
    }

    private suspend fun shareImageFile(image: SharePayload.Image, title: String?): ShareResult {
        val b64 = Base64.encode(image.bytes)
        return try {
            shareFileViaBase64(b64, image.mimeType, image.filename ?: "shared", title).await<JsAny?>()
            ShareResult.Completed
        } catch (e: Throwable) {
            classifyJsError(e)
        }
    }

    /**
     * Best-effort error classification on wasmJs. We can't pass [Throwable] into a JS
     * function (wasmJs interop restriction), so we scrape the message for known error
     * names. Slightly less precise than the JS path, but the common cases
     * (`AbortError`, `NotAllowedError`) are conventionally surfaced in the message.
     */
    private fun classifyJsError(e: Throwable): ShareResult {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("AbortError") -> ShareResult.Cancelled
            msg.contains("NotAllowedError") -> ShareResult.Failed(ShareError.UserGestureMissing)
            msg.contains("NoHandler") -> ShareResult.Failed(ShareError.NoHandler)
            else -> ShareResult.Failed(ShareError.Unknown(msg.ifBlank { "JS share error" }))
        }
    }

    private fun buildShareText(payload: SharePayload): String? = when (payload) {
        is SharePayload.Text -> payload.content

        is SharePayload.Url -> payload.href

        is SharePayload.Image, is SharePayload.File -> null

        is SharePayload.Multi -> payload.items.mapNotNull {
            when (it) {
                is SharePayload.Text -> it.content
                is SharePayload.Url -> it.href
                else -> null
            }
        }.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun buildShareUrl(payload: SharePayload): String? = when (payload) {
        is SharePayload.Url -> payload.href
        else -> null
    }
}

@JsFun("() => typeof navigator !== 'undefined' && typeof navigator.share === 'function'")
private external fun hasNavigatorShare(): Boolean

@JsFun(
    """(title, text, url) => {
        const data = {};
        if (title != null) data.title = title;
        if (text != null) data.text = text;
        if (url != null) data.url = url;
        return navigator.share(data);
    }""",
)
private external fun navigatorShare(title: String?, text: String?, url: String?): Promise<JsAny?>

@JsFun("(text) => navigator.clipboard.writeText(text)")
private external fun navigatorClipboardWriteText(text: String): Promise<JsAny?>

/**
 * Bridge ByteArray → JS File via base64: Kotlin/Wasm passes the bytes as a base64
 * string (simplest cross-target interop); JS decodes to Uint8Array → File → share.
 *
 * Failure modes (signalled via thrown Error so [classifyJsError] can route them):
 * - `NoHandler` — browser lacks `navigator.canShare({files: [file]})` support
 * - `AbortError` — user dismissed the share sheet
 * - `NotAllowedError` — called outside a user-gesture handler
 * - any other — surfaces as `Unknown(message)`
 */
@JsFun(
    """(b64, mime, name, title) => {
        const bin = atob(b64);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        const file = new File([bytes], name || 'shared', { type: mime });
        if (typeof navigator === 'undefined' || typeof navigator.canShare !== 'function' || !navigator.canShare({ files: [file] })) {
            throw new Error('NoHandler');
        }
        const data = { files: [file] };
        if (title != null) data.title = title;
        return navigator.share(data);
    }""",
)
private external fun shareFileViaBase64(b64: String, mime: String, name: String, title: String?): Promise<JsAny?>
