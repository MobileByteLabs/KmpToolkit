/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import com.mobilebytelabs.kmptoolkit.openurl.testing.FakeUrlLauncher
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservation
import io.github.mobilebytelabs.kmptoolkit.observe.testing.FakeLibraryObservationHook
import io.github.mobilebytelabs.kmptoolkit.observe.testing.resetLibraryObservation
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves this module's events actually reach a registered hook.
 *
 * Worth asserting rather than assuming: 23 modules generate `CmpMetadata` and, before 2026-09-13,
 * exactly one reported anything — the scaffolding existed everywhere and produced silence, with
 * nothing failing to signal that it was silent. A test per integrated module is what stops that
 * recurring.
 */
class AppReviewObservabilityTest {

    private val hook = FakeLibraryObservationHook()

    @BeforeTest
    fun setUp() {
        resetLibraryObservation()
        LibraryObservation.register(hook)
    }

    @AfterTest
    fun tearDown() = resetLibraryObservation()

    @Test
    fun opening_the_store_listing_reports_a_lifecycle_event() {
        AppReviewManagerImpl(
            listingOverride = StoreListing(webUrl = "https://example.com/app"),
            urlLauncher = FakeUrlLauncher(),
        ).openStoreListing()

        assertTrue(hook.receivedLifecycle("store_listing_opened"), "the event fired")
        assertEquals("cmp-app-review", hook.lifecycleEvents.single().meta.name)
    }

    @Test
    fun the_payload_carries_the_outcome_and_route_but_never_the_url() {
        // The privacy contract: hooks forward to Crashlytics and Analytics, so the store URL — an app
        // identifier — must not be in the payload. Outcome and route are what an observer can act on.
        AppReviewManagerImpl(
            listingOverride = StoreListing(webUrl = "https://example.com/secret-build-xyz"),
            urlLauncher = FakeUrlLauncher(),
        ).openStoreListing()

        val payload = hook.payloadOf("store_listing_opened")!!
        assertEquals("StoreOpened", payload["outcome"])
        assertEquals("store", payload["route"])
        assertTrue(
            payload.values.none { it?.toString()?.contains("secret-build-xyz") == true },
            "no payload value may contain the URL, got $payload",
        )
    }

    @Test
    fun a_refused_launcher_is_reported_as_a_failure_not_a_silence() {
        // An observer's question is "did the rate button work?" — a failure that reports nothing is
        // indistinguishable from never being pressed.
        AppReviewManagerImpl(
            listingOverride = StoreListing(webUrl = "https://example.com/app"),
            urlLauncher = FakeUrlLauncher(canOpenPredicate = { false }),
        ).openStoreListing()

        assertEquals("Failed", hook.payloadOf("store_listing_opened")?.get("outcome"))
    }

    @Test
    fun an_unconfigured_listing_still_reports() {
        AppReviewManagerImpl(urlLauncher = FakeUrlLauncher()).openStoreListing()

        assertEquals("NoStoreConfigured", hook.payloadOf("store_listing_opened")?.get("outcome"))
    }

    @Test
    fun requestReview_reports_exactly_once_per_call() = runTest {
        // Guards double-reporting: requestReview() may fall through to openStoreListing(), and both
        // report. A consumer counting "review asks" must not see two events for one ask.
        AppReviewManagerImpl(
            listingOverride = StoreListing(webUrl = "https://example.com/app"),
            urlLauncher = FakeUrlLauncher(),
        ).requestReview()

        assertEquals(1, hook.lifecycleEvents.size, "one call, one event: ${hook.lifecycleNames()}")
    }
}
