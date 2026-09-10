/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlin.test.Test

/**
 * End-to-end UI scenarios for `cmp-network-monitor-compose` — what the user actually SEES as
 * connectivity changes underneath a running composition.
 *
 * Before this suite the module had one test class that asserted `LocalNetworkMonitor` was
 * non-null, with a comment conceding that "actual @Composable access test requires Compose UI
 * test framework". Nothing here had ever been composed. These tests render the real composables
 * through `runComposeUiTest` and assert the visible result, so a regression that swaps the
 * offline and online branches — or leaves a stale banner on screen after reconnect — fails here
 * instead of shipping.
 *
 * `FakeNetworkMonitor` drives the transitions, so no real connectivity is involved.
 *
 * ABSTRACT ON PURPOSE. `androidx.compose.ui.test`'s Android environment reads
 * `android.os.Build.FINGERPRINT` to choose an idling strategy, and that static field is null
 * under the plain android.jar stub — every composition died with
 * `NullPointerException: … because "android.os.Build.FINGERPRINT" is null`. The field is
 * `static final` and NOT writable by reflection (verified), so the only real fix is to run the
 * Android host variant under Robolectric, which needs a JUnit4 `@RunWith` that a commonTest class
 * cannot carry. Keeping the scenarios here and letting each target subclass them gives every
 * platform the runner it needs while the assertions stay written once.
 *
 * Subclasses: `ConnectivityUiScenarioJvmTest`, `ConnectivityUiScenarioAndroidTest` (Robolectric),
 * `ConnectivityUiScenarioAppleTest`.
 */
@OptIn(ExperimentalTestApi::class)
abstract class ConnectivityUiScenarios {

    private fun wifi() = NetworkInfo(type = NetworkType.WiFi)
    private fun cellular() = NetworkInfo(type = NetworkType.Cellular, isMetered = true)

    // ── NetworkAwareContent: the three-way branch ───────────────────────────────────────────

    /** Online at first composition must render the online branch and nothing else. */
    @Test
    fun online_at_start_renders_only_online_content() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("ONLINE").assertIsDisplayed()
        onNodeWithText("OFFLINE").assertDoesNotExist()
    }

    /** Offline at first composition must render the offline branch, not an empty screen. */
    @Test
    fun offline_at_start_renders_only_offline_content() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("OFFLINE").assertIsDisplayed()
        onNodeWithText("ONLINE").assertDoesNotExist()
    }

    /**
     * The journey that matters: connectivity drops while the user is looking at the screen. The
     * composition must swap branches without a restart — this is the recomposition path.
     */
    @Test
    fun losing_connection_swaps_online_content_for_offline_content_live() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("ONLINE").assertIsDisplayed()

        monitor.setOnline(false)
        waitForIdle()

        onNodeWithText("OFFLINE").assertIsDisplayed()
        onNodeWithText("ONLINE").assertDoesNotExist()
    }

    /** …and recovery must swap back. A UI stuck on "offline" after reconnect is the classic bug. */
    @Test
    fun regaining_connection_restores_online_content() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("OFFLINE").assertIsDisplayed()

        monitor.setOnline(true)
        waitForIdle()

        onNodeWithText("ONLINE").assertIsDisplayed()
        onNodeWithText("OFFLINE").assertDoesNotExist()
    }

    /**
     * A captive portal must reach its OWN branch, not be lumped in with offline — that branch is
     * where an app shows the "sign in to this network" prompt.
     */
    @Test
    fun captive_portal_renders_its_dedicated_branch_with_the_redirect_url() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        monitor.setNetworkStatus(
            NetworkStatus.CaptivePortal(wifi(), redirectUrl = "https://login.example.net"),
        )
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                captivePortalContent = { Text("PORTAL ${it.redirectUrl}") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("PORTAL https://login.example.net").assertIsDisplayed()
        onNodeWithText("OFFLINE").assertDoesNotExist()
        onNodeWithText("ONLINE").assertDoesNotExist()
    }

    /** With no captivePortalContent supplied, the documented default falls back to offline. */
    @Test
    fun captive_portal_falls_back_to_offline_content_when_no_branch_given() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        monitor.setNetworkStatus(NetworkStatus.CaptivePortal(wifi()))
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("ONLINE") },
            )
        }
        onNodeWithText("OFFLINE").assertIsDisplayed()
    }

    /** The online branch receives the live status, so UI can show the transport. */
    @Test
    fun online_branch_receives_live_network_info() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        monitor.setNetworkStatus(NetworkStatus.Available(cellular()))
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                onlineContent = { Text("VIA ${it.info.type} metered=${it.info.isMetered}") },
            )
        }
        onNodeWithText("VIA Cellular metered=true").assertIsDisplayed()
    }

    /** A WiFi→cellular handoff must keep the user on the online branch, with updated info. */
    @Test
    fun handoff_keeps_online_branch_and_updates_the_transport() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        setContent {
            NetworkAwareContent(
                monitor = monitor,
                offlineContent = { Text("OFFLINE") },
                onlineContent = { Text("VIA ${it.info.type}") },
            )
        }
        onNodeWithText("VIA WiFi").assertIsDisplayed()

        monitor.simulateHandoff(from = NetworkType.WiFi, to = NetworkType.Cellular)
        waitForIdle()

        onNodeWithText("VIA Cellular").assertIsDisplayed()
        onNodeWithText("OFFLINE").assertDoesNotExist()
    }

    // ── ConnectivityBanner ──────────────────────────────────────────────────────────────────

    /** The banner's message must be visible to the user when offline. */
    @Test
    fun banner_shows_its_message_while_offline() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            ConnectivityBanner(monitor = monitor, message = "No internet connection")
        }
        onNodeWithText("No internet connection").assertIsDisplayed()
    }

    /** Losing connectivity must surface the banner without a recomposition of the whole tree. */
    @Test
    fun banner_appears_on_disconnect_and_carries_a_custom_message() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        setContent {
            ConnectivityBanner(monitor = monitor, message = "You are offline")
        }

        monitor.setOnline(false)
        waitForIdle()

        onNodeWithText("You are offline").assertIsDisplayed()
    }

    /** The banner must host trailing content — apps put a Retry action there. */
    @Test
    fun banner_renders_trailing_action_content() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            ConnectivityBanner(
                monitor = monitor,
                message = "Offline",
                trailingContent = { Text("RETRY") },
            )
        }
        onNodeWithText("Offline").assertIsDisplayed()
        onNodeWithText("RETRY").assertIsDisplayed()
    }

    // ── CompositionLocal wiring ─────────────────────────────────────────────────────────────

    /**
     * `ProvideNetworkMonitor` must actually reach descendants through `LocalNetworkMonitor` —
     * this is the wiring apps rely on instead of threading the monitor through every call.
     */
    @Test
    fun provided_monitor_is_visible_to_descendants_via_the_composition_local() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            ProvideNetworkMonitor(monitor) {
                val local = LocalNetworkMonitor.current
                val online by local.collectIsOnlineAsState()
                Text(if (online) "LOCAL ONLINE" else "LOCAL OFFLINE")
            }
        }
        onNodeWithText("LOCAL OFFLINE").assertIsDisplayed()
    }

    /** …and updates through the local must recompose descendants live. */
    @Test
    fun composition_local_propagates_connectivity_changes_to_descendants() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        setContent {
            ProvideNetworkMonitor(monitor) {
                val local = LocalNetworkMonitor.current
                val online by local.collectIsOnlineAsState()
                Text(if (online) "LOCAL ONLINE" else "LOCAL OFFLINE")
            }
        }
        onNodeWithText("LOCAL OFFLINE").assertIsDisplayed()

        monitor.setOnline(true)
        waitForIdle()

        onNodeWithText("LOCAL ONLINE").assertIsDisplayed()
    }

    // ── State-collection extensions ─────────────────────────────────────────────────────────

    /** `collectNetworkStatusAsState` must deliver the status object, not just a boolean. */
    @Test
    fun collect_status_as_state_exposes_the_full_status_to_the_ui() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        setContent {
            val status by monitor.collectNetworkStatusAsState()
            Text(
                text = when (val s = status) {
                    is NetworkStatus.Available -> "AVAILABLE ${s.info.type}"
                    is NetworkStatus.CaptivePortal -> "PORTAL"
                    is NetworkStatus.Unavailable -> "UNAVAILABLE"
                },
                modifier = Modifier.testTag("status"),
            )
        }
        onNodeWithTag("status").assertIsDisplayed()
        onNodeWithText("AVAILABLE WiFi").assertIsDisplayed()
    }

    /** Quality drives adaptive UI (e.g. lower video resolution); it must recompose on change. */
    @Test
    fun collect_quality_as_state_updates_when_connectivity_degrades() = runComposeUiTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        monitor.setNetworkStatus(NetworkStatus.Available(wifi()))
        setContent {
            val quality by monitor.collectNetworkQualityAsState()
            Text("Q=$quality")
        }

        monitor.setNetworkStatus(NetworkStatus.Unavailable)
        waitForIdle()

        onNodeWithText("Q=Offline").assertIsDisplayed()
    }
}
