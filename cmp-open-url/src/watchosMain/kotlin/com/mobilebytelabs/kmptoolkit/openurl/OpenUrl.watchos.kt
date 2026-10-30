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
import platform.WatchKit.WKExtension

/**
 * watchOS URL opening via `WKExtension.openSystemURL`.
 *
 * ## What changed and why
 * These four functions used to return `false` / `NoHandler` unconditionally, above a comment
 * reading "watchOS has no browser or URL-opening concept". The sibling `cmp-intent-launcher`
 * module has been using `WKExtension.sharedExtension.openSystemURL` on watchOS all along — the
 * claim was simply wrong, and this module's users got nothing as a result.
 *
 * watchOS routes a fixed set of schemes: `http`/`https` hand off to the paired iPhone, while
 * `tel:`, `sms:` and `mailto:` are handled on the watch itself. Anything else has no handler, and
 * [canOpen] says so up front rather than after a silent failure.
 */
private val SUPPORTED_SCHEMES = listOf("http://", "https://", "tel:", "sms:", "mailto:")

actual fun openUrl(url: String): Boolean {
    if (!canOpen(url)) return false
    val nsUrl = NSURL.URLWithString(url) ?: return false
    WKExtension.sharedExtension().openSystemURL(nsUrl)
    return true
}

/**
 * watchOS has no browser of its own; `http`/`https` are handed to the paired iPhone, which is the
 * closest thing to "open in a browser" the platform offers.
 */
actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    val transformed = appHint.transformUrl(url)
        ?: return OpenUrlResult.Error("AppHint $appHint cannot be applied to '$url' on watchOS")
    return if (openUrl(transformed)) OpenUrlResult.Success else OpenUrlResult.NoHandler
}

/** Whether watchOS routes this scheme. There is no `canOpenURL` on watchOS, so the list is ours. */
actual fun canOpen(url: String): Boolean =
    SUPPORTED_SCHEMES.any { url.startsWith(it, ignoreCase = true) } && NSURL.URLWithString(url) != null

/** No in-app browser on this platform — the system browser is the closest equivalent. */
actual fun openInApp(url: String): Boolean = openInBrowser(url)
