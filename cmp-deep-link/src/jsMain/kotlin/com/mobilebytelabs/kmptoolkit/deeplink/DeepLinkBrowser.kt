package com.mobilebytelabs.kmptoolkit.deeplink

import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

/**
 * Routing mode for browser-based deep link detection.
 *
 * - [HASH]: listen to `hashchange` events; parse `window.location.hash` (e.g. `#/product/42`).
 * - [HISTORY]: listen to `popstate` events; parse `window.location.pathname` + `window.location.search`.
 */
enum class BrowserRoutingMode { HASH, HISTORY }

/**
 * Browser (JS) entry point for deep link handling.
 *
 * Call once at app startup. Parses the current URL immediately, then listens
 * for subsequent navigation events.
 *
 * ## Usage
 *
 * ```kotlin
 * fun main() {
 *     DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
 *     // start your Compose Web app...
 * }
 * ```
 *
 * See `docs/WEB.md` for routing mode selection guidance.
 */
fun DeepLinkHandler.initBrowser(mode: BrowserRoutingMode = BrowserRoutingMode.HASH) {
    // Handle current URL on init
    parseBrowserUrl(mode)

    val listener = EventListener { parseBrowserUrl(mode) }
    when (mode) {
        BrowserRoutingMode.HASH -> window.addEventListener("hashchange", listener)
        BrowserRoutingMode.HISTORY -> window.addEventListener("popstate", listener)
    }
}

private fun DeepLinkHandler.parseBrowserUrl(mode: BrowserRoutingMode) {
    val location = window.location
    val uri = when (mode) {
        BrowserRoutingMode.HASH -> {
            val hash = location.hash.removePrefix("#")
            if (hash.isNotBlank()) "${location.origin}$hash" else return
        }

        BrowserRoutingMode.HISTORY -> {
            "${location.origin}${location.pathname}${location.search}"
        }
    }
    if (uri.isNotBlank()) handle(uri)
}
