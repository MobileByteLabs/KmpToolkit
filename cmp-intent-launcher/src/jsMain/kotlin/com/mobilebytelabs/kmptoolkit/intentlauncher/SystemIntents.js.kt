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

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * JS browser `SystemIntents` actual.
 *
 * - `openAppSettings()` — `UnsupportedPlatform` (browsers have no per-app settings).
 * - `createDocument()` — `window.showSaveFilePicker()` via inline `js("...")` with a
 *   single state-discriminator callback (mirrors the wasmJs binding's shape so both
 *   actuals share a contract). Returns `UnsupportedPlatform` on browsers without the
 *   File System Access API (Firefox + Safari today).
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult = IntentResult.Failed(IntentError.UnsupportedPlatform)

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        suspendCancellableCoroutine { cont ->
            try {
                callShowSaveFilePicker(suggestedName, mimeType) { state, value ->
                    if (!cont.isActive) return@callShowSaveFilePicker
                    cont.resume(
                        when (state) {
                            "ok" -> if (value.isNullOrBlank()) {
                                IntentResult.Failed(IntentError.Unknown("showSaveFilePicker returned blank name"))
                            } else {
                                IntentResult.Ok(IntentData(uri = "file-system-access:$value", mimeType = mimeType))
                            }

                            "cancelled" -> IntentResult.Cancelled

                            "unsupported" -> IntentResult.Failed(IntentError.UnsupportedPlatform)

                            else -> IntentResult.Failed(IntentError.Unknown(value ?: "showSaveFilePicker error"))
                        },
                    )
                }
            } catch (t: Throwable) {
                if (cont.isActive) {
                    cont.resume(IntentResult.Failed(IntentError.Unknown(t.message ?: "JS save picker error")))
                }
            }
        }
}

/**
 * Single-call JS bridge for `window.showSaveFilePicker`. Reports via the Kotlin
 * lambda callback with a `state` discriminator ("ok" | "cancelled" | "unsupported"
 * | "error") and a `value` carrying the picked filename or the error message.
 *
 * Kotlin/JS lambdas are JS-callable functions under the hood, so calling
 * `onResult('ok', 'name')` from inside the `js("...")` body just works.
 */
@Suppress("UNUSED_PARAMETER")
private fun callShowSaveFilePicker(
    suggestedName: String,
    mimeType: String,
    onResult: (state: String, value: String?) -> Unit,
) {
    js(
        """
        (function(){
            if (typeof window === 'undefined' || typeof window.showSaveFilePicker !== 'function') {
                onResult('unsupported', null);
                return;
            }
            var opts = { suggestedName: suggestedName };
            if (mimeType && mimeType !== '*/*') {
                var acceptObj = {};
                acceptObj[mimeType] = [];
                opts.types = [{ description: 'Document', accept: acceptObj }];
            }
            window.showSaveFilePicker(opts).then(function(handle){
                onResult('ok', handle.name || suggestedName);
            }).catch(function(err){
                if (err && (err.name === 'AbortError' || err.name === 'NotAllowedError')) {
                    onResult('cancelled', null);
                } else {
                    onResult('error', (err && err.message) ? err.message : 'showSaveFilePicker error');
                }
            });
        })()
        """,
    )
}
