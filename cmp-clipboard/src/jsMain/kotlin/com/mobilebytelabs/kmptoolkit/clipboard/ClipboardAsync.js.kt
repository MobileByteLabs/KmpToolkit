package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.await
import kotlin.js.Promise

actual suspend fun getFromClipboardAsync(): String? {
    return try {
        val clipboard = js("navigator.clipboard")
        if (clipboard != null) {
            (clipboard.readText() as Promise<String>).await()
        } else {
            getFromClipboard()
        }
    } catch (e: Throwable) {
        getFromClipboard()
    }
}

actual suspend fun copyToClipboardAsync(text: String): Boolean {
    return try {
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
}

actual suspend fun hasClipboardTextAsync(): Boolean {
    return try {
        val text = getFromClipboardAsync()
        text != null && text.isNotEmpty()
    } catch (e: Throwable) {
        false
    }
}
