package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.js.Promise
import kotlinx.coroutines.await

/**
 * Wasm JS async clipboard operations using the browser Clipboard API.
 *
 * Unlike the synchronous functions, these actually work in Wasm JS
 * by using `navigator.clipboard.readText()` / `writeText()` via JS interop.
 */

@JsFun("() => { return navigator.clipboard.readText(); }")
private external fun jsReadClipboardAsync(): Promise<JsString>

@JsFun("(text) => { return navigator.clipboard.writeText(text); }")
private external fun jsWriteClipboardAsync(text: String): Promise<JsAny?>

actual suspend fun getFromClipboardAsync(): String? = try {
    jsReadClipboardAsync().await<JsString>().toString()
} catch (e: Throwable) {
    null
}

actual suspend fun copyToClipboardAsync(text: String): Boolean = try {
    jsWriteClipboardAsync(text).await<JsAny?>()
    true
} catch (e: Throwable) {
    false
}

actual suspend fun hasClipboardTextAsync(): Boolean = try {
    val text = getFromClipboardAsync()
    text != null && text.isNotEmpty()
} catch (e: Throwable) {
    false
}
