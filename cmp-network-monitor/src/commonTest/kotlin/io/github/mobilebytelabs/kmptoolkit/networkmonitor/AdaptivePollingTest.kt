package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptivePollingTest {

    @Test
    fun initialIntervalIsBase() {
        val state = AdaptivePollingState(
            baseIntervalMs = 3_000L,
            maxIntervalMs = 30_000L,
        )
        assertEquals(3_000L, state.intervalMs)
    }

    @Test
    fun stableChecksGrowInterval() {
        val state = AdaptivePollingState(
            baseIntervalMs = 1_000L,
            maxIntervalMs = 10_000L,
            growthFactor = 2.0,
        )
        // Need 3 stable checks before growth kicks in
        state.reportStable()
        state.reportStable()
        state.reportStable()
        assertEquals(2_000L, state.intervalMs)
    }

    @Test
    fun changeResetsInterval() {
        val state = AdaptivePollingState(
            baseIntervalMs = 1_000L,
            maxIntervalMs = 10_000L,
            growthFactor = 2.0,
        )
        repeat(5) { state.reportStable() }
        state.reportChange()
        assertEquals(1_000L, state.intervalMs)
    }

    @Test
    fun intervalCapsAtMax() {
        val state = AdaptivePollingState(
            baseIntervalMs = 1_000L,
            maxIntervalMs = 5_000L,
            growthFactor = 2.0,
        )
        repeat(20) { state.reportStable() }
        assertTrue(state.intervalMs <= 5_000L)
    }

    @Test
    fun resetClearsState() {
        val state = AdaptivePollingState(
            baseIntervalMs = 1_000L,
            maxIntervalMs = 10_000L,
        )
        repeat(10) { state.reportStable() }
        state.reset()
        assertEquals(1_000L, state.intervalMs)
    }

    // ValidationBackoffState tests

    @Test
    fun backoffInitiallyInactive() {
        val backoff = ValidationBackoffState()
        assertFalse(backoff.shouldBackoff)
        assertEquals(0L, backoff.delayMs)
    }

    @Test
    fun failureActivatesBackoff() {
        val backoff = ValidationBackoffState(baseDelayMs = 1_000L)
        backoff.reportFailure()
        assertTrue(backoff.shouldBackoff)
        assertEquals(1_000L, backoff.delayMs)
    }

    @Test
    fun consecutiveFailuresGrowBackoff() {
        val backoff = ValidationBackoffState(
            baseDelayMs = 1_000L,
            maxDelayMs = 60_000L,
            multiplier = 2.0,
        )
        backoff.reportFailure() // 1000
        backoff.reportFailure() // 2000
        backoff.reportFailure() // 4000
        assertEquals(4_000L, backoff.delayMs)
    }

    @Test
    fun backoffCapsAtMax() {
        val backoff = ValidationBackoffState(
            baseDelayMs = 1_000L,
            maxDelayMs = 5_000L,
            multiplier = 2.0,
        )
        repeat(10) { backoff.reportFailure() }
        assertTrue(backoff.delayMs <= 5_000L)
    }

    @Test
    fun successResetsBackoff() {
        val backoff = ValidationBackoffState(baseDelayMs = 1_000L)
        repeat(5) { backoff.reportFailure() }
        backoff.reportSuccess()
        assertFalse(backoff.shouldBackoff)
        assertEquals(0L, backoff.delayMs)
    }
}
