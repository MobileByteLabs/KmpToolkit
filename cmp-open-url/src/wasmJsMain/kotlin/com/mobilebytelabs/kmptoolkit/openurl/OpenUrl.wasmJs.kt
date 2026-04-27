package com.mobilebytelabs.kmptoolkit.openurl

actual fun openUrl(url: String): Boolean = try {
    wasmWindowOpen(url, "_blank") != null
} catch (_: Throwable) {
    false
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    if (openUrl(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
} catch (e: Throwable) {
    OpenUrlResult.Error(e.message ?: "wasmJs error opening URL")
}

actual fun canOpen(url: String): Boolean = true // browser environment always can

@JsFun("(url, target) => window.open(url, target)")
private external fun wasmWindowOpen(url: String, target: String): JsAny?
