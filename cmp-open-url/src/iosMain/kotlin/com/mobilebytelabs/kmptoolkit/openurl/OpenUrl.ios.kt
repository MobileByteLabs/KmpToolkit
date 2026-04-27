package com.mobilebytelabs.kmptoolkit.openurl

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return try {
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(nsUrl)) {
            app.openURL(nsUrl)
            true
        } else {
            false
        }
    } catch (_: Throwable) {
        false
    }
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    val nsUrl = NSURL.URLWithString(url) ?: return OpenUrlResult.Error("Invalid URL: $url")
    return try {
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(nsUrl)) {
            app.openURL(nsUrl)
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
        UIApplication.sharedApplication.canOpenURL(nsUrl)
    } catch (_: Throwable) {
        false
    }
}
