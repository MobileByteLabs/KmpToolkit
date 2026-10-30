package com.mobilebytelabs.kmptoolkit.openurl

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

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

/**
 * `SFSafariViewController` presented over whatever is on top, so it works from a modal or a sheet too.
 * Safari only shows http(s) — any other scheme goes to [openUrl]. Must be called on the main thread, as any
 * UIKit presentation (a Compose click handler already is).
 */
actual fun openInApp(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val scheme = nsUrl.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return openUrl(url)
    return try {
        var top: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return openUrl(url)
        while (true) top = top.presentedViewController ?: break
        top.presentViewController(SFSafariViewController(uRL = nsUrl), animated = true, completion = null)
        true
    } catch (_: Throwable) {
        openUrl(url)
    }
}

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    // G6 fix (inter-app-comms-suite 03-open-url-g6-fix): rewrite URL per AppHint
    // before calling UIApplication.openURL. Previously AppHint was a silent no-op on iOS.
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
