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

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.OfflineEventQueue
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Engine tests for the analytics capabilities adopted into cmp-firebase: the opt-out cascade,
 * percentile performance stats, and the offline event queue.
 */
class AnalyticsEngineTest {

    @Test
    fun optout_cascade_suppresses_auto_trackers() {
        val analytics = TestAnalyticsHelper()

        // Opted out → an auto-tracker (funnel) emits nothing.
        analytics.setCollectionEnabled(false)
        analytics.funnel("checkout").start()
        assertEquals(0, analytics.events.size, "collection off must suppress emission")

        // Opted back in → emission resumes.
        analytics.setCollectionEnabled(true)
        analytics.funnel("checkout").start()
        assertTrue(analytics.events.isNotEmpty(), "collection on must allow emission")
    }

    @Test
    fun performance_tracker_exposes_percentiles() {
        val analytics = TestAnalyticsHelper()
        val perf = PerformanceTracker(analytics)
        repeat(3) { perf.stop(perf.start("api_call")) }

        val stats = perf.getPerformanceStats("api_call")
        assertNotNull(stats, "stats present after timings")
        assertEquals(3, stats.count)
        assertTrue(stats.p95Ms >= 0.0 && stats.p99Ms >= 0.0, "percentiles computed")
        assertEquals(3, analytics.events.size, "each stop emits a loading_time event")
    }

    @Test
    fun offline_queue_buffers_then_flushes_on_reconnect() = runTest {
        val monitor = FakeNetworkMonitor()
        val sink = TestAnalyticsHelper()
        val queue = OfflineEventQueue(sink, monitor, backgroundScope)
        runCurrent()

        monitor.setOnline(false)
        runCurrent()
        queue.logEvent(AnalyticsEvent("e1"))
        queue.logEvent(AnalyticsEvent("e2"))
        assertEquals(0, sink.events.size, "offline events are buffered, not delivered")
        assertEquals(2, queue.bufferedCount)

        monitor.setOnline(true)
        runCurrent()
        assertEquals(2, sink.events.size, "reconnect flushes the buffer")
    }
}
