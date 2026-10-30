/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.openurl.testing

import com.mobilebytelabs.kmptoolkit.openurl.AppHint
import com.mobilebytelabs.kmptoolkit.openurl.OpenUrlResult
import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncher

/** One recorded open request. */
public data class OpenedUrl(public val url: String, public val appHint: AppHint, public val browser: Boolean)

/**
 * In-memory [UrlLauncher] for tests — shipped in the main artifact, like `FakeNetworkMonitor`, so
 * consumers can assert on link handling without launching anything.
 *
 * ```kotlin
 * val urls = FakeUrlLauncher()
 * ArticleViewModel(urls).openSource("https://example.com")
 * assertEquals("https://example.com", urls.opened.single().url)
 * ```
 *
 * Simulate a platform that cannot open a link — tvOS with an `https://` URL, say — so a test can
 * prove the UI hides the affordance rather than rendering a dead one:
 *
 * ```kotlin
 * val locked = FakeUrlLauncher(canOpenPredicate = { false })
 * ArticleViewModel(locked).openSource("https://example.com")
 * assertTrue(locked.opened.isEmpty())
 * ```
 */
public class FakeUrlLauncher(
    /** Decides [canOpen]; also gates [open], matching how a real platform behaves. */
    private val canOpenPredicate: (String) -> Boolean = { it.isNotBlank() },
) : UrlLauncher {

    /** Every successful open, in order. */
    public val opened: MutableList<OpenedUrl> = mutableListOf()

    /** Every URL passed to [openInApp] and accepted, in order — a subset of [opened]. */
    public val openedInApp: MutableList<String> = mutableListOf()

    /** Every open that was refused because [canOpenPredicate] said no. */
    public val refused: MutableList<String> = mutableListOf()

    override fun open(url: String): Boolean = record(url, AppHint.DEFAULT, browser = false)

    override fun openInBrowser(url: String): Boolean = record(url, AppHint.BROWSER, browser = true)

    override fun openInApp(url: String): Boolean =
        record(url, AppHint.BROWSER, browser = true).also { if (it) openedInApp += url }

    override fun openWith(url: String, appHint: AppHint): OpenUrlResult =
        if (record(url, appHint, browser = false)) OpenUrlResult.Success else OpenUrlResult.NoHandler

    override fun canOpen(url: String): Boolean = canOpenPredicate(url)

    /** Forget every recorded and refused URL. */
    public fun reset() {
        opened.clear()
        openedInApp.clear()
        refused.clear()
    }

    private fun record(url: String, hint: AppHint, browser: Boolean): Boolean {
        if (!canOpenPredicate(url)) {
            refused += url
            return false
        }
        opened += OpenedUrl(url, hint, browser)
        return true
    }
}
