package com.mobilebytelabs.kmptoolkit.deeplink

/**
 * Routing mode for browser-based deep link detection.
 *
 * - [HASH]: listen to `hashchange` events; parse `window.location.hash`.
 * - [HISTORY]: listen to `popstate` events; parse `window.location.pathname`.
 */
enum class BrowserRoutingMode { HASH, HISTORY }

/**
 * Browser (Wasm/JS) entry point for deep link handling.
 *
 * Call once at app startup to begin listening for URL changes.
 *
 * ## Usage
 *
 * ```kotlin
 * fun main() {
 *     DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
 * }
 * ```
 */
fun DeepLinkHandler.initBrowser(mode: BrowserRoutingMode = BrowserRoutingMode.HASH) {
    parseBrowserUrl(mode)
    when (mode) {
        BrowserRoutingMode.HASH ->
            wasmAddEventListener("hashchange") { parseBrowserUrl(BrowserRoutingMode.HASH) }

        BrowserRoutingMode.HISTORY ->
            wasmAddEventListener("popstate") { parseBrowserUrl(BrowserRoutingMode.HISTORY) }
    }
}

private fun DeepLinkHandler.parseBrowserUrl(mode: BrowserRoutingMode) {
    val uri = when (mode) {
        BrowserRoutingMode.HASH -> {
            val hash = wasmGetLocationHash().removePrefix("#")
            if (hash.isNotBlank()) "${wasmGetLocationOrigin()}$hash" else return
        }

        BrowserRoutingMode.HISTORY -> {
            "${wasmGetLocationOrigin()}${wasmGetLocationPathname()}${wasmGetLocationSearch()}"
        }
    }
    if (uri.isNotBlank()) handle(uri)
}

@JsFun("(type, cb) => window.addEventListener(type, cb)")
private external fun wasmAddEventListener(type: String, cb: () -> Unit)

@JsFun("() => window.location.hash")
private external fun wasmGetLocationHash(): String

@JsFun("() => window.location.origin")
private external fun wasmGetLocationOrigin(): String

@JsFun("() => window.location.pathname")
private external fun wasmGetLocationPathname(): String

@JsFun("() => window.location.search")
private external fun wasmGetLocationSearch(): String
