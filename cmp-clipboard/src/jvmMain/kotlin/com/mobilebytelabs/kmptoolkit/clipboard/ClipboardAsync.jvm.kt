package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun getFromClipboardAsync(): String? = withContext(Dispatchers.IO) { getFromClipboard() }

actual suspend fun copyToClipboardAsync(text: String): Boolean = withContext(Dispatchers.IO) { copyToClipboard(text) }

actual suspend fun hasClipboardTextAsync(): Boolean = withContext(Dispatchers.IO) { hasClipboardText() }
