package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun getFromClipboardAsync(): String? =
    withContext(Dispatchers.Main) {
        getFromClipboard()
    }

actual suspend fun copyToClipboardAsync(text: String): Boolean =
    withContext(Dispatchers.Main) {
        copyToClipboard(text)
    }

actual suspend fun hasClipboardTextAsync(): Boolean =
    withContext(Dispatchers.Main) {
        hasClipboardText()
    }
