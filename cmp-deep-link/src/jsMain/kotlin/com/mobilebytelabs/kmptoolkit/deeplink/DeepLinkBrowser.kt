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
 * Auto-registers browser listeners at module load time (HASH mode default).
 * Runs before any consumer code via the Kotlin/JS module IIFE.
 * The SSR guard prevents crashes in Node.js where `window` is undefined.
 * Consumers who need HISTORY mode call [initBrowser] with [BrowserRoutingMode.HISTORY] explicitly.
 */
@Suppress("unused")
private val deepLinkAutoInit: Unit = run {
    if (js("typeof window !== 'undefined' && typeof window.location !== 'undefined'") as Boolean) {
        DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
    }
}

fun DeepLinkHandler.initBrowser(mode: BrowserRoutingMode = BrowserRoutingMode.HASH) {
    // Guard: prevent double-registration if deepLinkAutoInit already ran or consumer calls this explicitly.
    if (browserInitialized) return
    browserInitialized = true

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
