package com.mobilebytelabs.kmptoolkit.deeplink

/**
 * Routing mode for browser-based deep link detection.
 *
 * - [HASH]: listen to `hashchange` events; parse `window.location.hash`.
 * - [HISTORY]: listen to `popstate` events; parse `window.location.pathname`.
 */
enum class BrowserRoutingMode { HASH, HISTORY }

/**
 * Auto-registers browser listeners at module load time (HASH mode default).
 * Runs via the Wasm module start function before any consumer code.
 * The SSR guard prevents crashes in environments where `window` is undefined.
 * Consumers who need HISTORY mode call [initBrowser] with [BrowserRoutingMode.HISTORY] explicitly.
 */
@Suppress("unused")
private val deepLinkAutoInit: Unit = run {
    if (wasmIsBrowserEnvironment()) {
        DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
    }
}

/** Browser (Wasm/JS) entry point. Call with [BrowserRoutingMode.HISTORY] to opt-in to history routing. */
fun DeepLinkHandler.initBrowser(mode: BrowserRoutingMode = BrowserRoutingMode.HASH) {
    // Guard: prevent double-registration if _autoInit already ran or consumer calls this explicitly.
    if (browserInitialized) return
    browserInitialized = true

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

@JsFun("() => typeof window !== 'undefined' && typeof window.location !== 'undefined'")
private external fun wasmIsBrowserEnvironment(): Boolean

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
