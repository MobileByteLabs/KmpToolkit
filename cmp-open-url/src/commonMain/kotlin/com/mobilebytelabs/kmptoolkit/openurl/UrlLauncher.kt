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

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * Injectable URL opening — the type to depend on from a ViewModel, repository or composable.
 *
 * ## Why an interface when top-level functions already exist
 * `openUrl(...)` and friends are top-level `expect fun`s. A function cannot be substituted, so
 * code calling them is untestable without actually launching a browser, and cannot be wrapped in a
 * decorator that adds analytics, an "external link" confirmation, or an allow-list. [UrlLauncher]
 * is the same capability behind an injectable type. The top-level functions stay public.
 *
 * ## Implementing
 * All four members are abstract, and each maps to exactly one function — a fake is short and
 * obvious:
 *
 * ```kotlin
 * class RecordingLauncher : UrlLauncher {
 *     val opened = mutableListOf<String>()
 *     override fun open(url: String) = opened.add(url)
 *     override fun openInBrowser(url: String) = open(url)
 *     override fun openWith(url: String, appHint: AppHint) =
 *         if (open(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
 *     override fun canOpen(url: String) = true
 * }
 * ```
 *
 * Or just use `FakeUrlLauncher`, which ships in this artifact.
 *
 * ## Using
 * ```kotlin
 * class ArticleViewModel(private val urls: UrlLauncher) : ViewModel() {
 *     fun openSource(url: String) {
 *         // Ask before offering — do not render a dead link.
 *         if (urls.canOpen(url)) urls.open(url)
 *     }
 * }
 * ```
 */
public interface UrlLauncher {

    /** Open [url] with the platform's default handler. `false` if nothing could handle it. */
    public fun open(url: String): Boolean

    /** Open [url] in a browser specifically, bypassing any app that claims the link. */
    public fun openInBrowser(url: String): Boolean

    /** Open [url] with a specific kind of app — mail client, maps, dialer. */
    public fun openWith(url: String, appHint: AppHint = AppHint.DEFAULT): OpenUrlResult

    /** Whether [url] has a handler — check BEFORE offering the action, not after it fails. */
    public fun canOpen(url: String): Boolean
}

/**
 * The one [UrlLauncher] — for every target.
 *
 * Three one-line delegations to the platform functions. Stateless, so a single instance is safe to
 * share (which is why the DI module binds it as a singleton).
 */
public class UrlLauncherImpl : UrlLauncher {

    override fun open(url: String): Boolean = openUrl(url).also { report("url_opened", url, it) }

    override fun openInBrowser(url: String): Boolean =
        com.mobilebytelabs.kmptoolkit.openurl.openInBrowser(url).also { report("url_opened_in_browser", url, it) }

    override fun openWith(url: String, appHint: AppHint): OpenUrlResult = openWithApp(url, appHint).also {
        observeLifecycle(
            cmpMetadata(),
            "url_opened_with_app",
            mapOf("scheme" to url.scheme(), "hint" to appHint::class.simpleName, "result" to it::class.simpleName),
        )
    }

    // canOpen is a pure query with no side effect — reporting it would drown the signal in noise.
    override fun canOpen(url: String): Boolean = com.mobilebytelabs.kmptoolkit.openurl.canOpen(url)

    /**
     * Reports the SCHEME, never the URL.
     *
     * A URL routinely carries a session token, a password-reset nonce or a document id, and hooks
     * forward what they receive to Crashlytics and Analytics. The scheme plus the outcome answers the
     * question an observer actually has — "are my mailto: links failing on this platform?" — without
     * putting user data into a third-party pipeline.
     */
    private fun report(event: String, url: String, handled: Boolean) {
        observeLifecycle(cmpMetadata(), event, mapOf("scheme" to url.scheme(), "handled" to handled))
    }

    private fun String.scheme(): String = substringBefore("://", missingDelimiterValue = "none")
}
