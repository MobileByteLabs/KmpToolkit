/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * wasmJs implementation — Web Share API + clipboard fallback. Same shape as JS;
 * uses `@JsFun` external bindings (wasmJs has no `dynamic`).
 *
 * Same user-gesture constraint (Phase 0 TS6) as JS.
 */
@ExperimentalShareApi
public actual object Share {

    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
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
