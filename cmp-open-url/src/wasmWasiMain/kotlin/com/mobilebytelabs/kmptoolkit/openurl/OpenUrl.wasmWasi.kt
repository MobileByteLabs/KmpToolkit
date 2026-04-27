package com.mobilebytelabs.kmptoolkit.openurl

// WASI has no display/browser concept — all operations are deliberate no-ops.

actual fun openUrl(url: String): Boolean = false

actual fun openInBrowser(url: String): Boolean = false

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = OpenUrlResult.NoHandler

actual fun canOpen(url: String): Boolean = false
