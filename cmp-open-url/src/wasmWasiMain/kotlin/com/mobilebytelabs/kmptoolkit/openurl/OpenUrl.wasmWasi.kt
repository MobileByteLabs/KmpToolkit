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
 * The wasmWasi URL channel.
 *
 * WASI has no display and no browser — but "open this URL" is a *request*, and a WASI module can
 * always pass a request to the host that embedded it. A CLI host might print the link, a server
 * host might enqueue it, a desktop embedder might hand it to the real OS. So rather than returning
 * `false` and dropping it, the request crosses the boundary.
 *
 * ```kotlin
 * WasiUrlOpener.handler = { url, hint -> myHost.openInSystemBrowser(url); true }
 * ```
 *
 * With no handler the URL is buffered in [opened] and echoed to stdout, so a host that only reads
 * process output still receives it — which is why [canOpen] is true by default rather than false.
 */
public object WasiUrlOpener {

    /** Serves open requests. Return `true` when the host accepted the URL. */
    public var handler: ((String, AppHint) -> Boolean)? = null

    /** Whether un-handled URLs are printed to stdout. Default `true`. */
    public var echoToStdout: Boolean = true

    private val buffer: MutableList<String> = mutableListOf()

    /** Every URL passed to the host so far and not yet drained, oldest first. */
    public val opened: List<String> get() = buffer.toList()

    /** Take everything in [opened] and empty it. */
    public fun drain(): List<String> {
        val snapshot = buffer.toList()
        buffer.clear()
        return snapshot
    }

    /** Clear buffered URLs and restore defaults — intended for tests. */
    public fun reset() {
        buffer.clear()
        handler = null
        echoToStdout = true
    }

    internal fun deliver(url: String, hint: AppHint): Boolean {
        val host = handler
        if (host != null) return host(url, hint)
        buffer += url
        if (echoToStdout) println("cmp-open-url|$hint|$url")
        return true
    }
}

/** wasmWasi — hands the URL to the host via [WasiUrlOpener] instead of dropping it. */
actual fun openUrl(url: String): Boolean = url.isNotBlank() && WasiUrlOpener.deliver(url, AppHint.DEFAULT)

actual fun openInBrowser(url: String): Boolean = url.isNotBlank() && WasiUrlOpener.deliver(url, AppHint.BROWSER)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult {
    val transformed = appHint.transformUrl(url)
        ?: return OpenUrlResult.Error("AppHint $appHint cannot be applied to '$url'")
    return if (WasiUrlOpener.deliver(transformed, appHint)) OpenUrlResult.Success else OpenUrlResult.NoHandler
}

/**
 * True for any non-blank URL: the host boundary accepts anything, and with no handler stdout
 * still carries it out of the process. What the host then does with it is the host's business.
 */
actual fun canOpen(url: String): Boolean = url.isNotBlank()

/** No in-app browser on this platform — the system browser is the closest equivalent. */
actual fun openInApp(url: String): Boolean = openInBrowser(url)
