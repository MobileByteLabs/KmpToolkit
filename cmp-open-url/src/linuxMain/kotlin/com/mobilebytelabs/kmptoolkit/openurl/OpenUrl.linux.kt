package com.mobilebytelabs.kmptoolkit.openurl

import platform.posix.system

actual fun openUrl(url: String): Boolean = try {
    // xdg-open is the standard URL-opener on Linux desktop environments.
    // We run it in the background (&) so the call returns immediately.
    val safeUrl = url.replace("'", "%27") // basic shell-safety for single-quoted arg
    system("xdg-open '$safeUrl' &") == 0
} catch (_: Throwable) {
    false
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    if (openUrl(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
} catch (e: Throwable) {
    OpenUrlResult.Error(e.message ?: "Linux error opening URL")
}

actual fun canOpen(url: String): Boolean = true // assume xdg-open is present on Linux desktop
