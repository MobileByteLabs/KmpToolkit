/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.openurl

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * tvOS URL opening via `UIApplication.openURL`.
 *
 * ## What changed and why
 * These four functions used to return `false` / `NoHandler` unconditionally, above a comment
 * reading "tvOS has no browser or URL-opening concept". That is not true: `UIApplication` on tvOS
 * exposes both `canOpenURL` and `openURL` (verified by compiling against them). What tvOS lacks is
 * a *web browser*, so `https://` links generally have no handler — but App Store links, other
 * apps' custom schemes and system URLs all open fine.
 *
 * [canOpen] now asks the OS rather than assuming, so a caller gets the real answer per URL instead
 * of a blanket no.
 */
actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    val app = UIApplication.sharedApplication
    if (!app.canOpenURL(nsUrl)) return false
    app.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
    return true
}

/**
 * tvOS ships no web browser, so there is no browser to force a link into. Falls back to the
 * default handler — which succeeds for schemes tvOS does route, and reports `false` for the
 * `https://` links that genuinely have nowhere to go.
 */
actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    val transformed = appHint.transformUrl(url)
        ?: return OpenUrlResult.Error("AppHint $appHint cannot be applied to '$url' on tvOS")
    return if (openUrl(transformed)) OpenUrlResult.Success else OpenUrlResult.NoHandler
}

actual fun canOpen(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return UIApplication.sharedApplication.canOpenURL(nsUrl)
}

/** No in-app browser on this platform — the system browser is the closest equivalent. */
actual fun openInApp(url: String): Boolean = openInBrowser(url)
