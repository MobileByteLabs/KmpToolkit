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

import com.mobilebytelabs.kmptoolkit.openurl.testing.FakeUrlLauncher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Contract for the injectable [UrlLauncher] facade.
 *
 * `UrlLauncherImpl` is three one-line delegations, so the interesting behaviour to pin down is the
 * fake's — it is what every consumer's tests will assert against, and a fake that silently accepts
 * everything would let a "check before offering" bug ship.
 */
class UrlLauncherTest {

    @Test
    fun open_records_the_url_with_the_default_hint() {
        val urls = FakeUrlLauncher()
        assertTrue(urls.open("https://example.com"))

        val opened = urls.opened.single()
        assertEquals("https://example.com", opened.url)
        assertEquals(AppHint.DEFAULT, opened.appHint)
        assertFalse(opened.browser)
    }

    @Test
    fun openInBrowser_is_distinguishable_from_a_plain_open() {
        // They are different intents: one bypasses an app that claims the link. A fake that
        // conflated them would hide a real bug.
        val urls = FakeUrlLauncher()
        urls.openInBrowser("https://example.com")

        val opened = urls.opened.single()
        assertTrue(opened.browser)
        assertEquals(AppHint.BROWSER, opened.appHint)
    }

    @Test
    fun openWith_carries_the_hint_through() {
        val urls = FakeUrlLauncher()
        assertIs<OpenUrlResult.Success>(urls.openWith("mailto:a@b.com", AppHint.EMAIL))
        assertEquals(AppHint.EMAIL, urls.opened.single().appHint)
    }

    @Test
    fun an_unopenable_url_is_refused_rather_than_recorded_as_opened() {
        // Simulates tvOS with an https link, or watchOS with an unsupported scheme.
        val urls = FakeUrlLauncher(canOpenPredicate = { false })

        assertFalse(urls.open("https://example.com"))
        assertTrue(urls.opened.isEmpty(), "a refused open must not look like a successful one")
        assertEquals(listOf("https://example.com"), urls.refused)
    }

    @Test
    fun openWith_reports_no_handler_when_the_url_cannot_be_opened() {
        val urls = FakeUrlLauncher(canOpenPredicate = { false })
        assertIs<OpenUrlResult.NoHandler>(urls.openWith("tel:123", AppHint.PHONE))
    }

    @Test
    fun canOpen_follows_the_predicate_so_a_ui_can_gate_on_it() {
        val onlyHttps = FakeUrlLauncher(canOpenPredicate = { it.startsWith("https://") })
        assertTrue(onlyHttps.canOpen("https://example.com"))
        assertFalse(onlyHttps.canOpen("weird-scheme://x"))
    }

    @Test
    fun reset_clears_both_ledgers() {
        val urls = FakeUrlLauncher(canOpenPredicate = { it.startsWith("https") })
        urls.open("https://ok")
        urls.open("nope")
        urls.reset()

        assertTrue(urls.opened.isEmpty())
        assertTrue(urls.refused.isEmpty())
    }

    @Test
    fun the_real_impl_delegates_each_method_to_its_own_platform_function() {
        // Guards against the copy-paste slip where openInBrowser routes to openUrl: on every
        // target canOpen must agree with the top-level function it wraps.
        val impl = UrlLauncherImpl()
        assertEquals(canOpen("https://example.com"), impl.canOpen("https://example.com"))
        assertEquals(canOpen(""), impl.canOpen(""))
    }

    @Test
    fun openInApp_is_recorded_and_distinguishable_from_a_browser_open() {
        val urls = FakeUrlLauncher()
        assertTrue(urls.openInApp("https://example.com/privacy"))
        urls.openInBrowser("https://example.com/elsewhere")

        assertEquals(listOf("https://example.com/privacy"), urls.openedInApp)
        assertEquals(2, urls.opened.size, "an in-app open is still an open")
        urls.reset()
        assertTrue(urls.openedInApp.isEmpty())
    }

    @Test
    fun openInApp_defaults_to_openInBrowser_for_existing_implementers() {
        // A consumer's own UrlLauncher written before openInApp existed must keep compiling and behave sanely.
        var browserCalls = 0
        val legacy = object : UrlLauncher {
            override fun open(url: String) = true
            override fun openInBrowser(url: String): Boolean {
                browserCalls++
                return true
            }
            override fun openWith(url: String, appHint: AppHint) = OpenUrlResult.Success
            override fun canOpen(url: String) = true
        }
        assertTrue(legacy.openInApp("https://example.com"))
        assertEquals(1, browserCalls)
    }

    @Test
    fun refused_url_is_not_recorded_as_opened_in_app() {
        val urls = FakeUrlLauncher(canOpenPredicate = { false })
        assertFalse(urls.openInApp("https://example.com"))
        assertTrue(urls.openedInApp.isEmpty())
    }
}
