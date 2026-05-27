package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Apple-platform tests for [AppleNetworkMonitor] — covers iOS, macOS, tvOS, watchOS
 * via the auto-wired appleTest sourceset.
 *
 * The Apple monitor uses NWPathMonitor (push-based); close() must call
 * nw_path_monitor_cancel(monitor) cleanly without throwing and without leaking observers.
 * Analogous to the M-001 fix in JS/WasmJs but for the Apple platform.
 */
class AppleNetworkMonitorCloseTest {

    @Test
    fun close_does_not_throw() = runTest {
        val monitor = AppleNetworkMonitor(
            NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly),
        )
        monitor.close()
        assertTrue(true, "close() returned cleanly on Apple platform")
    }

    @Test
    fun close_is_idempotent() = runTest {
        val monitor = AppleNetworkMonitor(
            NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly),
        )
        monitor.close()
        monitor.close()
        monitor.close()
        assertTrue(true, "close() is safely idempotent on Apple")
    }
}
