package com.mobilebytelabs.kmptoolkit.openurl

// watchOS has no browser or URL-opening concept — all operations return graceful no-ops.

actual fun openUrl(url: String): Boolean = false

actual fun openInBrowser(url: String): Boolean = false

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = OpenUrlResult.NoHandler

actual fun canOpen(url: String): Boolean = false
