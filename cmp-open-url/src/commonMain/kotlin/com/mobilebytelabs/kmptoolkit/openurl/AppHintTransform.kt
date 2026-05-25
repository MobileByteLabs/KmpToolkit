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

/**
 * iOS / macOS URL-rewrite helper for [AppHint].
 *
 * On Apple platforms `UIApplication.openURL` / `NSWorkspace.openURL` routes by URL scheme
 * (no "Intent" abstraction). To honour an [AppHint] we rewrite the URL to the scheme-appropriate
 * form BEFORE calling the system opener. Used by [openWithApp] on iosMain + macosMain.
 *
 * Returns the transformed URL, or `null` if the [url] is incompatible with this hint
 * (e.g. `AppHint.EMAIL` + a plain HTTPS URL — caller can't know an email recipient).
 * Caller surfaces `null` as [OpenUrlResult.Error] with a clear message.
 *
 * On Android this helper is unused — Android's [openWithApp] routes via `Intent.ACTION_*`
 * directly without URL rewriting.
 *
 * Pure logic — no platform calls; lives in commonMain so behavior is shared between
 * iosMain + macosMain implementations + verifiable in commonTest.
 *
 * Fix history: G6 parity bug in `cmp-open-url 3.2.x` — Apple impls ignored [AppHint] and
 * silently called `openURL(url)` regardless. See plan `inter-app-comms-suite/03-open-url-g6-fix.md`.
 */
internal fun AppHint.transformUrl(url: String): String? = when (this) {
    AppHint.DEFAULT,
    AppHint.BROWSER,
    -> url

    AppHint.EMAIL ->
        if (url.startsWith("mailto:")) url else null

    AppHint.MAPS -> when {
        url.startsWith("maps:") || url.startsWith("geo:") -> url

        // Common Google Maps HTTPS forms → maps: scheme (Apple Maps preferred;
        // if Google Maps app is installed and registered as a maps handler, OS routes there).
        url.startsWith("https://maps.google.com") ->
            url.replaceFirst("https://maps.google.com", "maps://maps.google.com")

        url.startsWith("https://www.google.com/maps") ->
            url.replaceFirst("https://www.google.com/maps", "maps://maps.google.com/maps")

        url.startsWith("https://maps.apple.com") ->
            url.replaceFirst("https://maps.apple.com", "maps://maps.apple.com")

        else -> null
    }

    AppHint.PHONE -> when {
        url.startsWith("tel:") -> url

        url.isPhoneNumberCandidate() ->
            "tel:" + url.filter { it.isDigit() || it == '+' }

        else -> null
    }

    AppHint.SMS -> when {
        url.startsWith("sms:") -> url

        url.isPhoneNumberCandidate() ->
            "sms:" + url.filter { it.isDigit() || it == '+' }

        else -> null
    }

    is AppHint.Custom -> url // iOS / macOS: no-op (Custom is Android-only by contract; documented in AppHint.kt KDoc)
}

/**
 * Heuristic: does [this] look like a phone number a user typed?
 * Permits digits, `+`, spaces, hyphens, parens; at least one digit.
 * Not a strict E.164 check — the OS does the final validation on `tel:` / `sms:`.
 */
private fun String.isPhoneNumberCandidate(): Boolean {
    if (isBlank()) return false
    if (!any { it.isDigit() }) return false
    return all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
}
