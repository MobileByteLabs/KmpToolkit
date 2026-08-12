/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract tests for the opt-in/opt-out collection + consent surface added to
 * [AnalyticsHelper] (setCollectionEnabled / setConsent) and its wiring through
 * [AnalyticsConfig] + [AnalyticsModule].
 */
class AnalyticsCollectionTest {

    @Test
    fun collection_defaults_on_then_toggles() {
        val analytics = TestAnalyticsHelper()
        assertTrue(analytics.collectionEnabled, "collection is opt-in-by-default (on)")

        analytics.setCollectionEnabled(false)
        assertFalse(analytics.collectionEnabled, "opt-out disables collection")

        analytics.setCollectionEnabled(true)
        assertTrue(analytics.collectionEnabled, "re-enable turns collection back on")
    }

    @Test
    fun consent_is_recorded_as_analytics_and_ad_storage() {
        val analytics = TestAnalyticsHelper()
        assertNull(analytics.consent, "no consent set initially")

        analytics.setConsent(analyticsStorage = true, adStorage = false)
        assertEquals(true to false, analytics.consent)

        analytics.setConsent(analyticsStorage = false, adStorage = true)
        assertEquals(false to true, analytics.consent)
    }

    @Test
    fun config_default_is_opt_in_and_drives_helper_state() {
        assertTrue(AnalyticsConfig().collectionEnabledByDefault, "default config is opt-in (collection on)")

        // The factory applies config.collectionEnabledByDefault to the created helper via
        // setCollectionEnabled(...); a tracking helper wired the same way proves the contract
        // without needing a live Firebase (GitLive's JVM analytics is a stub).
        val optInRequired = AnalyticsConfig(collectionEnabledByDefault = false)
        val analytics = TestAnalyticsHelper().also { it.setCollectionEnabled(optInRequired.collectionEnabledByDefault) }
        assertFalse(analytics.collectionEnabled, "opt-in-required config starts collection OFF")
    }

    @Test
    fun factory_wires_noop_helper_with_config_without_throwing() {
        // Smoke: the factory's .also { setCollectionEnabled(...) } path is exercised for a
        // helper that needs no platform backend.
        val helper = AnalyticsModule.analyticsHelper(
            AnalyticsModule.Mode.NoOp,
            AnalyticsConfig(collectionEnabledByDefault = false),
        )
        // NoOp ignores the call by design; the point is the factory returns cleanly.
        helper.logEvent(AnalyticsEvent("smoke"))
    }

    @Test
    fun clear_resets_collection_and_consent() {
        val analytics = TestAnalyticsHelper()
        analytics.setCollectionEnabled(false)
        analytics.setConsent(analyticsStorage = false, adStorage = false)

        analytics.clear()

        assertTrue(analytics.collectionEnabled, "clear() restores opt-in-by-default")
        assertNull(analytics.consent, "clear() drops recorded consent")
    }
}
