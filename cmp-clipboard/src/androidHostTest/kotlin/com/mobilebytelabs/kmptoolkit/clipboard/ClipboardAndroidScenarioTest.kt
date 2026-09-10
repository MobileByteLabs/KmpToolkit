package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Clipboard scenarios executed against a REAL Android runtime supplied by Robolectric.
 *
 * The `commonTest` suite is filtered away from these paths on the Android target: every one of
 * them reaches `ClipboardManager` (the framework service) or `ProcessLifecycleOwner` through an
 * application `Context`, and a bare JVM host test has neither — `appContext` is null because no
 * init ContentProvider runs, so `start()` returns early and the monitor never reaches Monitoring.
 *
 * Robolectric supplies an `Application` and a working framework clipboard, injected below through
 * [setApplicationContext] — the same seam the ContentProvider uses on a device. So instead of the
 * Android actual merely being excluded from the host run, the real copy/paste round trip is
 * exercised here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ClipboardAndroidScenarioTest {

    @Before
    fun installApplicationContext() {
        setApplicationContext(RuntimeEnvironment.getApplication())
    }

    /**
     * Robolectric runs each test ON its main thread, and `runTest` blocks the thread it is on. The
     * async clipboard actuals dispatch with `withContext(Dispatchers.Main)`, so without swapping
     * the Main dispatcher the suspending scenario deadlocks: the test holds the very thread the
     * coroutine needs, and the run hangs indefinitely rather than failing.
     *
     * `AndroidClipboardMonitor` builds its scope from `Dispatchers.Main.immediate` too, so this
     * also makes the monitor scenarios deterministic instead of looper-timing dependent.
     */
    @Before
    fun installTestMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    // ── Copy / paste round trip ─────────────────────────────────────────────────────────────

    /** The core journey: text written to the system clipboard reads back identically. */
    @Test
    fun copy_then_paste_round_trips_through_the_real_system_clipboard() {
        assertTrue(copyToClipboard("KmpToolkit round trip"), "copy must report success on Android")
        assertEquals("KmpToolkit round trip", getFromClipboard())
    }

    /** `hasClipboardText` must agree with what was just written. */
    @Test
    fun has_text_reflects_a_real_copy() {
        copyToClipboard("something")
        assertTrue(hasClipboardText(), "hasClipboardText must see the copied value")
    }

    /** Clearing must empty it — the privacy path apps use on logout. */
    @Test
    fun clear_empties_the_clipboard() {
        copyToClipboard("sensitive")
        clearClipboard()
        val after = getFromClipboard()
        assertTrue(after.isNullOrEmpty(), "clipboard must be empty after clear, got '$after'")
    }

    /** Overwriting must replace, not append or interleave. */
    @Test
    fun a_second_copy_replaces_the_first() {
        copyToClipboard("first")
        copyToClipboard("second")
        assertEquals("second", getFromClipboard())
    }

    /** Empty text is a legal clipboard value and must not throw. */
    @Test
    fun copying_empty_text_does_not_throw() {
        val result = runCatching { copyToClipboard("") }
        assertTrue(result.isSuccess, "empty copy must not throw: ${result.exceptionOrNull()}")
    }

    /** The async variants must round-trip identically to the blocking ones. */
    @Test
    fun async_copy_and_paste_round_trip() = runTest {
        copyToClipboardAsync("async value")
        assertEquals("async value", getFromClipboardAsync())
        assertTrue(hasClipboardTextAsync())
    }

    // ── Monitor lifecycle against the real actual ───────────────────────────────────────────

    /** The monitor must construct against a real Context — this is what host tests could not do. */
    @Test
    fun monitor_can_be_created_on_android() {
        val monitor = createClipboardMonitor()
        assertNotNull(monitor)
    }

    /** Start must actually reach Monitoring when a Context is present. */
    @Test
    fun monitor_reaches_monitoring_state_when_started_with_a_context() {
        val monitor = createClipboardMonitor()
        monitor.start(ClipboardMonitorConfig())
        try {
            assertTrue(
                monitor.state.value is ClipboardMonitorState.Monitoring,
                "with a real Context start() must reach Monitoring, got ${monitor.state.value}",
            )
        } finally {
            monitor.stop()
        }
    }

    /** Stop must return it to a non-monitoring state, and be safe to repeat. */
    @Test
    fun monitor_stop_is_idempotent_and_leaves_monitoring() {
        val monitor = createClipboardMonitor()
        monitor.start(ClipboardMonitorConfig())
        monitor.stop()
        monitor.stop()
        assertFalse(
            monitor.state.value is ClipboardMonitorState.Monitoring,
            "stop() must leave Monitoring, got ${monitor.state.value}",
        )
    }

    /** Double-start must not create a second registration — the duplicate-listener leak. */
    @Test
    fun monitor_double_start_is_idempotent() {
        val monitor = createClipboardMonitor()
        monitor.start(ClipboardMonitorConfig())
        monitor.start(ClipboardMonitorConfig())
        try {
            assertTrue(monitor.state.value is ClipboardMonitorState.Monitoring)
        } finally {
            monitor.stop()
        }
    }

    /** Repeated start/stop cycles must not accumulate state or leak listeners. */
    @Test
    fun repeated_start_stop_cycles_do_not_leak() {
        val monitor = createClipboardMonitor()
        repeat(10) {
            monitor.start(ClipboardMonitorConfig())
            monitor.stop()
        }
        assertFalse(monitor.state.value is ClipboardMonitorState.Monitoring)
    }

    // ── Observer + permission on the real actual ────────────────────────────────────────────

    /** The observer must construct and start/stop cleanly with a Context available. */
    @Test
    fun observer_start_and_stop_round_trip_on_android() {
        val observer = createClipboardObserver()
        assertNotNull(observer)
        observer.startObserving()
        observer.stopObserving()
        observer.stopObserving()
    }

    /** Permission checks must answer without throwing when a Context exists. */
    @Test
    fun permission_check_answers_without_throwing() {
        val permission = createClipboardPermission()
        val result = runCatching { permission.hasClipboardAccess() }
        assertTrue(result.isSuccess, "permission check must not throw: ${result.exceptionOrNull()}")
    }

    // ── Manager façade end to end ───────────────────────────────────────────────────────────

    /** The façade's full lifecycle must work against the real framework clipboard. */
    @Test
    fun manager_full_lifecycle_on_android() {
        val manager = ClipboardManager()
        manager.start()
        try {
            assertTrue(manager.copy("via manager"))
            assertEquals("via manager", manager.paste())
            assertTrue(manager.hasText())
            manager.pause()
            manager.resume()
        } finally {
            manager.stop()
        }
    }

    /** Manager clear must empty the real clipboard, not just its own cache. */
    @Test
    fun manager_clear_empties_the_system_clipboard() {
        val manager = ClipboardManager()
        manager.start()
        try {
            manager.copy("to be cleared")
            manager.clear()
            assertTrue(getFromClipboard().isNullOrEmpty(), "clear must reach the system clipboard")
        } finally {
            manager.stop()
        }
    }
}
