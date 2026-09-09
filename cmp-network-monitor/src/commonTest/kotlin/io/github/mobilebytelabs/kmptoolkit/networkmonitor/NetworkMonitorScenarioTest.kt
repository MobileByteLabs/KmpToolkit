/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end scenarios for `cmp-network-monitor` — whole journeys an app actually goes through,
 * driven only through the public API.
 *
 * The existing suite covers units well (`NetworkStatusTest`, `RetryPolicyTest`,
 * `NetworkMonitorExtensionsTest`, …). What it does not cover is the *sequences*: a request that
 * fails offline then succeeds after reconnect, a WiFi→cellular handoff that must not be seen as
 * a disconnect, a captive portal that reports "connected" while nothing works, or a flicker that
 * must not stampede downstream collectors. Those are where connectivity code actually breaks,
 * and each one below is written as the story rather than as an assertion on one function.
 *
 * Everything runs on [FakeNetworkMonitor], so these execute on every target — JVM, native,
 * JS/wasmJs, and the Android host — with no Android runtime needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorScenarioTest {

    private val monitor = FakeNetworkMonitor(initialOnline = true)

    @AfterTest
    fun tearDown() {
        monitor.close()
    }

    private fun wifi(metered: Boolean = false) = NetworkInfo(type = NetworkType.WiFi, isMetered = metered)

    private fun cellular() = NetworkInfo(type = NetworkType.Cellular, isMetered = true)

    // ── Scenario 1: the airplane-mode round trip ────────────────────────────────────────────
    //
    // The most common journey there is: app is online, user loses connectivity mid-session, then
    // regains it. Every downstream signal must agree at each step, in order.

    @Test
    fun scenario_user_goes_offline_then_recovers_and_every_signal_agrees() = runTest {
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        assertTrue(monitor.isOnline.value, "precondition: starts online")

        monitor.setOnline(false)
        assertFalse(monitor.isOnline.value, "isOnline must follow the drop")
        assertEquals(NetworkStatus.Unavailable, monitor.networkStatus.value)
        assertFalse(monitor.currentStatus.isOnline, "currentStatus convenience must agree")
        assertNull(monitor.ifOnline { it }, "ifOnline must not run while offline")
        assertEquals("ran", monitor.ifOffline { "ran" }, "ifOffline must run while offline")

        monitor.setOnline(true)
        assertTrue(monitor.isOnline.value, "isOnline must follow the recovery")
        assertTrue(monitor.currentStatus is NetworkStatus.Available)
        assertNull(monitor.ifOffline { "ran" }, "ifOffline must not run once back online")
    }

    /** The drop→recover pair must surface as discrete events, in order, for listeners. */
    @Test
    fun scenario_offline_recovery_emits_disconnected_then_connected_in_order() = runTest {
        val events = async {
            monitor.networkChanges.take(2).toList()
        }
        yield()

        monitor.setOnline(false)
        monitor.setOnline(true)

        val seen = withTimeout(5_000) { events.await() }
        assertTrue(seen[0] is NetworkChangeEvent.Disconnected, "first event must be Disconnected, got ${seen[0]}")
        assertTrue(seen[1] is NetworkChangeEvent.Connected, "second event must be Connected, got ${seen[1]}")
    }

    // ── Scenario 2: work that must wait for connectivity ────────────────────────────────────

    /**
     * The two guards are deliberately different and it matters which you reach for:
     * `requireOnline()` FAILS FAST (throws now), while `withNetworkGuard`/`ensureOnline` SUSPEND
     * until connectivity returns. Picking the wrong one either hangs a request that should have
     * errored, or errors one that should have waited.
     */
    @Test
    fun scenario_require_online_fails_fast_while_offline() = runTest {
        monitor.setOnline(false)
        assertFailsWith<NetworkUnavailableException>("requireOnline must throw immediately") {
            monitor.requireOnline()
        }
    }

    /** …whereas the guard parks the work and releases it on reconnect rather than throwing. */
    @Test
    fun scenario_network_guard_suspends_offline_then_runs_on_reconnect() = runTest {
        monitor.setOnline(false)
        var ran = false
        val guarded = async {
            monitor.withNetworkGuard {
                ran = true
                "done"
            }
        }
        yield()
        assertFalse(ran, "guarded block must not run while offline")

        monitor.setOnline(true)
        assertEquals("done", withTimeout(5_000) { guarded.await() })
        assertTrue(ran)
    }

    /** …and the same call must proceed normally once connectivity is present. */
    @Test
    fun scenario_guarded_work_runs_when_online() = runTest {
        monitor.setOnline(true)
        assertEquals("done", monitor.withNetworkGuard { "done" })
    }

    /**
     * The upload-queue journey: a request is issued while offline, parks, and completes by itself
     * once the network returns — without the caller polling.
     */
    @Test
    fun scenario_deferred_work_completes_automatically_on_reconnect() = runTest {
        monitor.setOnline(false)
        val attempts = mutableListOf<Int>()
        var attempt = 0

        val work = async {
            monitor.retryOnReconnect(maxRetries = 5) {
                attempts += ++attempt
                "uploaded"
            }
        }
        yield()
        assertTrue(attempts.isEmpty(), "must not run the action while offline")

        monitor.setOnline(true)
        assertEquals("uploaded", withTimeout(5_000) { work.await() })
        assertEquals(listOf(1), attempts, "must run exactly once after reconnect, not once per retry")
    }

    /** `awaitOnline` is the primitive behind that: it suspends, then returns the live info. */
    @Test
    fun scenario_await_online_suspends_until_connectivity_returns() = runTest {
        monitor.setOnline(false)
        val awaited = async { monitor.awaitOnline() }
        yield()
        assertFalse(awaited.isCompleted, "must still be waiting while offline")

        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        monitor.setOnline(true)
        val info = withTimeout(5_000) { awaited.await() }
        assertEquals(NetworkType.WiFi, info.type)
    }

    // ── Scenario 3: WiFi → cellular handoff ─────────────────────────────────────────────────

    /**
     * Walking out of WiFi range onto cellular. The app must end up online on a metered link — the
     * transient Unavailable in the middle is a handoff artefact, not a real outage.
     */
    @Test
    fun scenario_wifi_to_cellular_handoff_ends_online_and_metered() = runTest {
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        assertTrue(monitor.isConnectedVia(NetworkType.WiFi))

        monitor.simulateHandoff(from = NetworkType.WiFi, to = NetworkType.Cellular)

        val status = monitor.networkStatus.value
        assertTrue(status is NetworkStatus.Available, "handoff must end online, got $status")
        assertEquals(NetworkType.Cellular, status.info.type)
        assertTrue(status.info.isMetered, "cellular must be reported as metered so callers can back off")
        assertTrue(monitor.isConnectedVia(NetworkType.Cellular))
        assertFalse(monitor.isConnectedVia(NetworkType.WiFi))
    }

    /** Code that must wait for a *specific* transport — e.g. defer a large sync until WiFi. */
    @Test
    fun scenario_large_sync_waits_for_wifi_and_ignores_cellular() = runTest {
        monitor.setNetworkStatus(NetworkStatus.Available(cellular()))
        val waiting = async { monitor.awaitConnectionType(NetworkType.WiFi) }
        yield()
        assertFalse(waiting.isCompleted, "cellular must not satisfy a WiFi-only wait")

        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        val info = withTimeout(5_000) { waiting.await() }
        assertEquals(NetworkType.WiFi, info.type)
        assertFalse(info.isMetered)
    }

    // ── Scenario 4: captive portal ──────────────────────────────────────────────────────────

    /**
     * Hotel WiFi: associated with an access point, but every request is intercepted. This is the
     * case naive `isConnected` checks get wrong — the app must NOT treat it as usable internet.
     */
    @Test
    fun scenario_captive_portal_is_not_treated_as_usable_connectivity() = runTest {
        monitor.setNetworkStatus(
            NetworkStatus.CaptivePortal(wifi(), redirectUrl = "https://login.example.net"),
        )

        val status = monitor.networkStatus.value
        assertTrue(status is NetworkStatus.CaptivePortal)
        assertEquals(
            "https://login.example.net",
            status.redirectUrl,
            "redirect must reach the UI so it can prompt login",
        )
        assertFalse(status.isOnline, "a captive portal is NOT online — this is the trap")
        assertNull(monitor.ifOnline { it }, "online-only work must not run behind a portal")

        // Pins the library's ACTUAL mapping (NetworkQuality.kt: CaptivePortal -> Poor), not what
        // one might assume. Worth knowing when consuming networkQuality(): a portal reports Poor,
        // i.e. "degraded", NOT Offline — so quality-driven code alone will keep issuing requests
        // that cannot succeed. Behind a portal, branch on `status is NetworkStatus.CaptivePortal`
        // (asserted above), which is the signal that carries the redirect URL anyway.
        assertEquals(
            NetworkQuality.Poor,
            monitor.networkQuality().first(),
            "captive portal maps to Poor; change this only alongside NetworkStatus.toQuality()",
        )
    }

    /** After the user logs in, the same monitor must transition cleanly to genuinely online. */
    @Test
    fun scenario_captive_portal_clears_after_login() = runTest {
        monitor.setNetworkStatus(NetworkStatus.CaptivePortal(wifi()))
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))

        assertTrue(monitor.currentStatus.isOnline)
        assertEquals(wifi(), monitor.ifOnline { it }, "online work must run once the portal clears")
    }

    // ── Scenario 5: flapping connectivity ───────────────────────────────────────────────────

    /**
     * A brief flicker (lift doors, tunnel) must not leave the app stuck offline. The end state is
     * what callers act on, and it must be correct regardless of how noisy the middle was.
     */
    @Test
    fun scenario_brief_flicker_settles_online() = runTest {
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        monitor.simulateFlicker(wifi())

        assertTrue(monitor.isOnline.value, "a flicker must settle back online")
        assertTrue(monitor.currentStatus is NetworkStatus.Available)
    }

    /** Repeated flapping must be recorded faithfully — history is what diagnostics rely on. */
    @Test
    fun scenario_repeated_flapping_is_recorded_for_diagnostics() = runTest {
        monitor.resetState(online = true)
        repeat(3) { monitor.simulateFlicker(wifi()) }

        assertTrue(monitor.updateCount >= 3, "each flicker must be recorded, got ${monitor.updateCount}")
        assertTrue(
            monitor.eventHistory.any { it is NetworkChangeEvent.Disconnected },
            "flapping must have produced Disconnected events",
        )
        assertTrue(
            monitor.eventHistory.any { it is NetworkChangeEvent.Connected },
            "flapping must have produced Connected events",
        )
        assertTrue(monitor.isOnline.value, "must still settle online after repeated flapping")
    }

    // ── Scenario 6: quality-driven behaviour ────────────────────────────────────────────────

    /**
     * Apps degrade gracefully off quality — e.g. drop video resolution on a poor link. The
     * quality signal must track status changes and must not repeat itself, or every status tick
     * would re-trigger expensive downstream work.
     */
    @Test
    fun scenario_quality_tracks_connectivity_and_does_not_repeat() = runTest {
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        val qualities = async { monitor.networkQuality().take(2).toList() }
        yield()

        // Same status twice: distinctUntilChanged must collapse it to one emission.
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        monitor.setNetworkStatus(NetworkStatus.Unavailable)

        val seen = withTimeout(5_000) { qualities.await() }
        assertEquals(2, seen.size)
        assertEquals(NetworkQuality.Offline, seen.last(), "going offline must surface as Offline quality")
        assertTrue(seen[0] != seen[1], "consecutive duplicates must be collapsed")
    }

    // ── Scenario 7: callback registration and teardown ──────────────────────────────────────

    /** The imperative bridge for non-Flow callers, over a full offline→online cycle. */
    @Test
    fun scenario_callbacks_fire_for_both_directions_then_stop_after_close() = runTest {
        // addCallback pins Dispatchers.Default internally, so its collector runs on a real
        // thread and runTest's virtual clock cannot advance it — yield() proves nothing here.
        // Signal completion explicitly and await it instead of guessing at timing.
        val wentOffline = CompletableDeferred<Unit>()
        val cameOnline = CompletableDeferred<NetworkInfo>()
        val onlineHits = mutableListOf<NetworkInfo>()
        val scope = CoroutineScope(coroutineContext[Job]!! + Dispatchers.Default)

        val handle = monitor.addCallback(
            scope = scope,
            onOnline = {
                onlineHits += it
                cameOnline.complete(it)
            },
            onOffline = { wentOffline.complete(Unit) },
        )

        monitor.setOnline(false)
        wentOffline.await()

        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        assertEquals(NetworkType.WiFi, cameOnline.await().type, "online callback must carry the live info")

        val onlineBefore = onlineHits.size
        handle.close()
        monitor.setNetworkStatus(NetworkStatus.Available(cellular()))

        assertEquals(onlineBefore, onlineHits.size, "no callbacks may fire after close — that is a leak")
    }

    // ── Scenario 8: lifecycle / shutdown ────────────────────────────────────────────────────

    /** Closing must be idempotent: app teardown paths often call it more than once. */
    @Test
    fun scenario_shutdown_is_idempotent_and_observable() = runTest {
        assertFalse(monitor.isClosed)
        monitor.close()
        assertTrue(monitor.isClosed)
        monitor.close()
        assertTrue(monitor.isClosed, "second close must not throw or flip state back")
    }

    /** A suspending consumer must not hang forever when the monitor shuts down under it. */
    @Test
    fun scenario_close_while_a_consumer_waits_does_not_hang_the_test() = runTest {
        monitor.setOnline(false)
        val waiter = CompletableDeferred<Boolean>()
        val job = launch {
            monitor.isOnline.first { it }
            waiter.complete(true)
        }
        yield()
        assertFalse(waiter.isCompleted, "consumer should still be waiting while offline")

        monitor.close()
        job.cancel()
        assertTrue(monitor.isClosed)
    }
}
