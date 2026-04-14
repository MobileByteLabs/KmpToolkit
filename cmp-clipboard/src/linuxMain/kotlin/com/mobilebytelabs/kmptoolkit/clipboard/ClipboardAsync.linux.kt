package com.mobilebytelabs.kmptoolkit.clipboard

actual suspend fun getFromClipboardAsync(): String? = getFromClipboard()

actual suspend fun copyToClipboardAsync(text: String): Boolean = copyToClipboard(text)

actual suspend fun hasClipboardTextAsync(): Boolean = hasClipboardText()
