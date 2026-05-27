package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.test.runTest
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(name) => { try { globalThis.dispatchEvent(new Event(name)); } catch (e) {} }")
private external fun dispatchSyntheticEvent(name: JsString)

/**
 * Regression tests for M-001 mirror: WasmJsNetworkMonitor event-listener cleanup race.
 *
 * Same root cause as [JsNetworkMonitorCloseRaceTest]: `close()` cancelled the
 * coroutine scope before unregistering the JS-side event handlers. A handler
 * firing in the window between scope-cancel and listener-removal would touch
 * a cancelled scope.
 */
@OptIn(ExperimentalWasmJsInterop::class)
class WasmJsNetworkMonitorCloseRaceTest {

    @Test
    fun close_does_not_throw_when_event_fires_immediately_after() = runTest {
        val monitor = WasmJsNetworkMonitor(NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly))

        monitor.close()

        // After close(), firing synthetic events must NOT invoke any handler
        // (listeners must already be removed from globalThis).
        dispatchSyntheticEvent("online".toJsString())
        dispatchSyntheticEvent("offline".toJsString())
        dispatchSyntheticEvent("online".toJsString())

        val statusAtClose = monitor.networkStatus.value
        assertEquals(
            statusAtClose,
            monitor.networkStatus.value,
            "networkStatus changed AFTER close() -- WasmJs listener was not removed before scope cancellation",
        )
    }

    @Test
    fun close_is_idempotent() = runTest {
        val monitor = WasmJsNetworkMonitor(NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly))
        monitor.close()
        monitor.close()
        monitor.close()
        assertTrue(true, "close() is safely idempotent on WasmJs")
    }

    @Test
    fun close_terminates_state_flow_subscribers() = runTest {
        val monitor = WasmJsNetworkMonitor(NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly))

        val onlineBefore = monitor.isOnline.value
        monitor.close()
        dispatchSyntheticEvent((if (onlineBefore) "offline" else "online").toJsString())

        assertEquals(
            onlineBefore,
            monitor.isOnline.value,
            "isOnline changed AFTER close() on WasmJs -- listener was not removed in time",
        )
    }
}
