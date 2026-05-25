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
    // G6 fix (inter-app-comms-suite 03-open-url-g6-fix): rewrite URL per AppHint
    // before calling NSWorkspace.openURL. Previously AppHint was a silent no-op on macOS.
    val transformed = appHint.transformUrl(url)
        ?: return OpenUrlResult.Error(
            "AppHint.$appHint cannot route URL: '$url'. Provide a scheme-appropriate URL " +
                "(mailto: for EMAIL; tel: or numeric for PHONE; sms: or numeric for SMS; " +
                "maps:/geo:/https://maps.* for MAPS).",
        )
    val nsUrl = NSURL.URLWithString(transformed)
        ?: NSURL.URLWithString(url) // fallback to original if transform produced something NSURL rejects
        ?: return OpenUrlResult.Error("Invalid URL: $url")
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
