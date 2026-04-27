package com.mobilebytelabs.kmptoolkit.openurl

actual fun openUrl(url: String): Boolean = try {
    if (isBrowserEnvironment()) {
        jsWindowOpen(url, "_blank")
        true
    } else {
        nodeOpen(url)
    }
} catch (_: Throwable) {
    false
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    if (openUrl(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
} catch (e: Throwable) {
    OpenUrlResult.Error(e.message ?: "JS error opening URL")
}

actual fun canOpen(url: String): Boolean = isBrowserEnvironment() || hasNodeOpenModule()

// ---------------------------------------------------------------------------
// JS interop helpers
// ---------------------------------------------------------------------------

private fun isBrowserEnvironment(): Boolean = js("typeof window !== 'undefined'") as Boolean

private fun hasNodeOpenModule(): Boolean = try {
    js("typeof require !== 'undefined'") as Boolean
} catch (_: Throwable) {
    false
}

private fun jsWindowOpen(url: String, target: String) {
    js("window.open(url, target)")
}

private fun nodeOpen(url: String): Boolean = try {
    js("require('open')(url)")
    true
} catch (_: Throwable) {
    false
}
