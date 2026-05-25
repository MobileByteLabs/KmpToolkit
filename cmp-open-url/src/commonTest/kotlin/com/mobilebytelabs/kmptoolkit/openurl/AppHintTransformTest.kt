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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * G6 fix regression tests — `AppHint.transformUrl()` URL rewriting.
 *
 * Pure-logic helper lives in commonMain so behaviour is verified once here +
 * shared between iosMain + macosMain `openWithApp` actuals.
 *
 * Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/03-open-url-g6-fix.md
 */
class AppHintTransformTest {
    // -------------------------------------------------------------------------
    // DEFAULT + BROWSER — identity
    // -------------------------------------------------------------------------

    @Test
    fun default_passesThrough() {
        assertEquals("https://example.com", AppHint.DEFAULT.transformUrl("https://example.com"))
    }

    @Test
    fun browser_passesThrough() {
        assertEquals("https://example.com", AppHint.BROWSER.transformUrl("https://example.com"))
    }

    // -------------------------------------------------------------------------
    // EMAIL
    // -------------------------------------------------------------------------

    @Test
    fun email_mailto_passesThrough() {
        assertEquals(
            "mailto:foo@bar.com",
            AppHint.EMAIL.transformUrl("mailto:foo@bar.com"),
        )
    }

    @Test
    fun email_mailtoWithSubject_passesThrough() {
        val url = "mailto:foo@bar.com?subject=hi&body=hello"
        assertEquals(url, AppHint.EMAIL.transformUrl(url))
    }

    @Test
    fun email_httpsUrl_returnsNull() {
        // Caller must provide a mailto: URL; we can't infer the recipient
        assertNull(AppHint.EMAIL.transformUrl("https://example.com"))
    }

    @Test
    fun email_plainAddress_returnsNull() {
        // Bare "foo@bar.com" is ambiguous — could be an email OR an SMTP server URL
        assertNull(AppHint.EMAIL.transformUrl("foo@bar.com"))
    }

    // -------------------------------------------------------------------------
    // MAPS
    // -------------------------------------------------------------------------

    @Test
    fun maps_geoScheme_passesThrough() {
        assertEquals("geo:37.7749,-122.4194", AppHint.MAPS.transformUrl("geo:37.7749,-122.4194"))
    }

    @Test
    fun maps_mapsScheme_passesThrough() {
        assertEquals("maps://maps.apple.com/?q=SF", AppHint.MAPS.transformUrl("maps://maps.apple.com/?q=SF"))
    }

    @Test
    fun maps_googleMapsHttps_rewriteToMapsScheme() {
        assertEquals(
            "maps://maps.google.com/?q=SF",
            AppHint.MAPS.transformUrl("https://maps.google.com/?q=SF"),
        )
    }

    @Test
    fun maps_googleMapsCanonical_rewriteToMapsScheme() {
        assertEquals(
            "maps://maps.google.com/maps?q=SF",
            AppHint.MAPS.transformUrl("https://www.google.com/maps?q=SF"),
        )
    }

    @Test
    fun maps_appleMapsHttps_rewriteToMapsScheme() {
        assertEquals(
            "maps://maps.apple.com/?q=SF",
            AppHint.MAPS.transformUrl("https://maps.apple.com/?q=SF"),
        )
    }

    @Test
    fun maps_arbitraryHttps_returnsNull() {
        assertNull(AppHint.MAPS.transformUrl("https://example.com"))
    }

    // -------------------------------------------------------------------------
    // PHONE
    // -------------------------------------------------------------------------

    @Test
    fun phone_telScheme_passesThrough() {
        assertEquals("tel:+15551234", AppHint.PHONE.transformUrl("tel:+15551234"))
    }

    @Test
    fun phone_numericPlus_prefixesTel() {
        assertEquals("tel:+15551234567", AppHint.PHONE.transformUrl("+1-555-1234567"))
    }

    @Test
    fun phone_digitsOnly_prefixesTel() {
        assertEquals("tel:5551234", AppHint.PHONE.transformUrl("555-1234"))
    }

    @Test
    fun phone_parentheses_strippedThenPrefixed() {
        assertEquals("tel:+15551234567", AppHint.PHONE.transformUrl("+1 (555) 123-4567"))
    }

    @Test
    fun phone_httpsUrl_returnsNull() {
        assertNull(AppHint.PHONE.transformUrl("https://example.com"))
    }

    @Test
    fun phone_blank_returnsNull() {
        assertNull(AppHint.PHONE.transformUrl(""))
    }

    // -------------------------------------------------------------------------
    // SMS
    // -------------------------------------------------------------------------

    @Test
    fun sms_smsScheme_passesThrough() {
        assertEquals("sms:+15551234", AppHint.SMS.transformUrl("sms:+15551234"))
    }

    @Test
    fun sms_numeric_prefixesSms() {
        assertEquals("sms:+15551234", AppHint.SMS.transformUrl("+1 555 1234"))
    }

    @Test
    fun sms_httpsUrl_returnsNull() {
        assertNull(AppHint.SMS.transformUrl("https://example.com"))
    }

    // -------------------------------------------------------------------------
    // Custom — iOS/macOS no-op
    // -------------------------------------------------------------------------

    @Test
    fun custom_appleNoOp_passesThrough() {
        assertEquals(
            "https://example.com",
            AppHint.Custom("com.example.app").transformUrl("https://example.com"),
        )
    }

    // -------------------------------------------------------------------------
    // Coverage sanity — every AppHint subtype handled
    // -------------------------------------------------------------------------

    @Test
    fun allHints_neverThrow() {
        val hints = listOf(
            AppHint.DEFAULT,
            AppHint.BROWSER,
            AppHint.EMAIL,
            AppHint.MAPS,
            AppHint.PHONE,
            AppHint.SMS,
            AppHint.Custom("com.example.app"),
        )
        for (hint in hints) {
            val result = runCatching { hint.transformUrl("https://example.com") }
            assertTrue(
                result.isSuccess,
                "AppHint.$hint.transformUrl must never throw; got: ${result.exceptionOrNull()}",
            )
        }
    }
}
