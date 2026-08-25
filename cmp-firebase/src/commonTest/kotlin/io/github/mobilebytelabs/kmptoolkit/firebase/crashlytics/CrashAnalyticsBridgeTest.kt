/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.EventTypes
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.ParamKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the single-all-platform crash dashboard invariant: the fallback-tier
 * [LoggingCrashReporter] (JVM/desktop/web/linux/wasm/tvOS/watchOS — the targets
 * Crashlytics can't ingest) mirrors every crash to analytics as an
 * [EventTypes.APP_CRASH] event tagged with [ParamKeys.PLATFORM], so all platforms
 * reach one GA4/BigQuery view — and it stays PII-safe (no raw message/stack).
 */
class CrashAnalyticsBridgeTest {

    private class RecordingAnalyticsHelper : AnalyticsHelper {
        val events = mutableListOf<AnalyticsEvent>()
        override fun logEvent(event: AnalyticsEvent) {
            events.add(event)
        }
    }

    private fun AnalyticsEvent.param(key: String): String? = extras.firstOrNull { it.key == key }?.value

    @Test
    fun fallback_crash_emits_app_crash_event_with_platform_tag() {
        val sink = RecordingAnalyticsHelper()
        val reporter = LoggingCrashReporter(platform = "jvm", analyticsSink = { sink })

        reporter.recordException(IllegalStateException("boom"), fatal = false)

        assertEquals(1, sink.events.size)
        val e = sink.events.single()
        assertEquals(EventTypes.APP_CRASH, e.type)
        assertEquals("jvm", e.param(ParamKeys.PLATFORM))
        assertEquals("IllegalStateException", e.param(ParamKeys.EXCEPTION_TYPE))
        assertEquals("false", e.param(ParamKeys.FATAL))
    }

    @Test
    fun fatal_flag_propagates_to_bridge_event() {
        val sink = RecordingAnalyticsHelper()
        val reporter = LoggingCrashReporter(platform = "linux", analyticsSink = { sink })

        reporter.recordException(RuntimeException("fatal"), fatal = true)

        assertEquals("true", sink.events.single().param(ParamKeys.FATAL))
    }

    @Test
    fun bridge_event_is_pii_safe_no_raw_message() {
        val sink = RecordingAnalyticsHelper()
        val reporter = LoggingCrashReporter(platform = "jvm", analyticsSink = { sink })

        reporter.recordException(IllegalArgumentException("user email leaked@example.com"), fatal = false)

        val e = sink.events.single()
        assertTrue(e.extras.none { it.value.contains("leaked@example.com") }, "raw message must not reach analytics")
        assertNull(e.param(ParamKeys.ERROR_MESSAGE))
    }

    @Test
    fun no_sink_means_no_bridge_event_and_no_throw() {
        // default (no analytics sink) must not throw and must still build lastReport
        val reporter = LoggingCrashReporter(platform = "jvm")
        reporter.recordException(IllegalStateException("boom"))
        assertEquals("IllegalStateException", reporter.lastReport?.throwableClass)
    }
}
