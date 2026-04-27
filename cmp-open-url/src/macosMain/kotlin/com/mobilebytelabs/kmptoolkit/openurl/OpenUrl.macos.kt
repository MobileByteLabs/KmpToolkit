package com.mobilebytelabs.kmptoolkit.openurl

import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return try {
        NSWorkspace.sharedWorkspace.openURL(nsUrl)
    } catch (_: Throwable) {
        false
    }
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    val nsUrl = NSURL.URLWithString(url) ?: return OpenUrlResult.Error("Invalid URL: $url")
    return try {
        if (NSWorkspace.sharedWorkspace.openURL(nsUrl)) {
            OpenUrlResult.Success
        } else {
            OpenUrlResult.NoHandler
        }
    } catch (_: Throwable) {
        OpenUrlResult.NoHandler
    }
}

actual fun canOpen(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return try {
        NSWorkspace.sharedWorkspace.URLForApplicationToOpenURL(nsUrl) != null
    } catch (_: Throwable) {
        false
    }
}
