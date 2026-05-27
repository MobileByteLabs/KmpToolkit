package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM-platform tests for [JvmNetworkMonitor]. Focus: lifecycle (close + idempotency).
 *
 * The JVM monitor uses adaptive polling on an internal [kotlinx.coroutines.CoroutineScope]
 * built with [kotlinx.coroutines.SupervisorJob] + [kotlinx.coroutines.Dispatchers.IO]. The
 * polling loop calls [JvmNetworkMonitor.checkNetwork] every `adaptivePolling.intervalMs`,
 * so [close] must cancel that scope cleanly without throwing and without leaving the loop
 * alive (which would continue emitting to the StateFlows).
 *
 * These tests exercise the same class of failure mode as M-001 (resource cleanup race) for
 * the JVM platform, but as platform-coverage rather than regression for a known bug.
 *
 * Note on validation strategy: tests use [ValidationStrategy.NativeOnly] to avoid any
 * outbound HTTP traffic (HEAD against `clients3.google.com/generate_204`) and to keep
 * the tests fully offline-safe / hermetic.
 */
class JvmNetworkMonitorCloseTest {

    @Test
    fun close_does_not_throw() = runTest {
        val monitor = JvmNetworkMonitor(
            NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly),
        )
        monitor.close()
        assertTrue(true, "close() returned cleanly")
    }

    @Test
    fun close_is_idempotent() = runTest {
        val monitor = JvmNetworkMonitor(
            NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly),
        )
        monitor.close()
        // Second + third close() must not throw -- the `closed` flag guards re-entry.
        monitor.close()
        monitor.close()
        assertTrue(true, "close() is safely idempotent on JVM")
    }

    @Test
    fun close_stops_polling_loop() = runTest {
        // Use a very short poll interval so the loop would normally tick multiple times
        // during the post-close observation window if it were still alive.
        val monitor = JvmNetworkMonitor(
            NetworkMonitorConfig(
                pollIntervalMs = 50L,
                validationStrategy = ValidationStrategy.NativeOnly,
            ),
        )

        // Snapshot state immediately after close(). If the polling scope is correctly
        // cancelled, no further updateState() calls will fire and the StateFlow value
        // must remain at this snapshot.
        monitor.close()
        val statusAtClose = monitor.networkStatus.value
        val onlineAtClose = monitor.isOnline.value

        // Wait several poll intervals' worth on the real clock. Note: the polling loop
        // runs on Dispatchers.IO (a *real* scheduler), NOT on runTest's TestDispatcher --
        // so we use kotlinx.coroutines.delay against real time here. The test scope only
        // suspends; the IO scope (if still alive) would actually tick.
        kotlinx.coroutines.runBlocking { delay(300L) }

        assertEquals(
            statusAtClose,
            monitor.networkStatus.value,
            "networkStatus changed AFTER close() -- polling loop was not cancelled",
        )
        assertEquals(
            onlineAtClose,
            monitor.isOnline.value,
            "isOnline changed AFTER close() -- polling loop was not cancelled",
        )
    }
}
