package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.await
import kotlin.js.Promise

actual suspend fun getFromClipboardAsync(): String? = try {
    val clipboard = js("navigator.clipboard")
    if (clipboard != null) {
        (clipboard.readText() as Promise<String>).await()
    } else {
        getFromClipboard()
    }
} catch (e: Throwable) {
    getFromClipboard()
}

actual suspend fun copyToClipboardAsync(text: String): Boolean = try {
    val clipboard = js("navigator.clipboard")
    if (clipboard != null) {
        (clipboard.writeText(text) as Promise<Unit>).await()
        true
    } else {
        copyToClipboard(text)
    }
} catch (e: Throwable) {
    copyToClipboard(text)
}

actual suspend fun hasClipboardTextAsync(): Boolean = try {
    val text = getFromClipboardAsync()
    text != null && text.isNotEmpty()
} catch (e: Throwable) {
    false
}
