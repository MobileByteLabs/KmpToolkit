package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for the M-002 fix pattern: events submitted to a
 * CompletableDeferred-gated `scope.launch { seedComplete.await(); body }` block
 * are QUEUED until seedComplete.complete(Unit) — not dropped.
 *
 * Mirrors the structural change applied to AndroidNetworkMonitor: AtomicBoolean
 * `callbackReady` replaced with CompletableDeferred `seedComplete`, callback
 * bodies wrapped in `scope.launch { seedComplete.await(); ... }`.
 *
 * The class under fix cannot be instantiated cross-platform without Android
 * runtime, so this test validates the PATTERN itself. The behavioural assertion
 * is: events fired before seed completes still execute (in order) after seed
 * completes.
 *
 * Implementation note: launches use the foreground TestScope (not
 * `backgroundScope`) because `advanceUntilIdle()` in kotlinx-coroutines-test
 * 1.10.x only drives foreground work — background tasks are intentionally
 * excluded so they cannot deadlock the scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SeedGatePatternTest {

    @Test
    fun events_during_seed_are_queued_not_dropped() = runTest {
        val seedComplete = CompletableDeferred<Unit>()
        val processed = mutableListOf<Int>()

        // Three events fire BEFORE seedComplete is released
        repeat(3) { event ->
            launch {
                seedComplete.await()
                processed.add(event)
            }
        }

        // None are processed yet — launches are suspended on await(); no foreground work runs to completion
        advanceUntilIdle()
        assertEquals(emptyList<Int>(), processed, "events ran before seedComplete — gate not honoured")

        // Release the seed
        seedComplete.complete(Unit)
        advanceUntilIdle()

        // All three are now processed, in order
        assertEquals(listOf(0, 1, 2), processed, "events were dropped instead of queued")
    }

    @Test
    fun events_after_seed_run_immediately() = runTest {
        val seedComplete = CompletableDeferred<Unit>()
        seedComplete.complete(Unit) // released eagerly

        val processed = mutableListOf<Int>()
        repeat(3) { event ->
            launch {
                seedComplete.await()
                processed.add(event)
            }
        }

        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), processed, "events fired after seed should run without delay")
    }

    @Test
    fun completing_seed_is_idempotent() = runTest {
        val seedComplete = CompletableDeferred<Unit>()
        assertTrue(seedComplete.complete(Unit), "first complete() should return true")
        // Second complete() returns false (already completed) — must NOT throw
        val second = seedComplete.complete(Unit)
        assertEquals(false, second, "second complete() must return false, not throw")
    }
}
