package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorExtensionsStateFlowTest {

    @Test
    fun isOnlineDebouncedState_emits_current_value_to_late_subscriber() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val flow = monitor.isOnlineDebouncedState(backgroundScope, timeoutMs = 100L)
        // Late subscriber sees the seeded initial value immediately
        assertEquals(true, flow.value)
        monitor.close()
    }

    @Test
    fun isOnlineDebouncedState_debounces_rapid_changes() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val flow = monitor.isOnlineDebouncedState(backgroundScope, timeoutMs = 100L)
        assertEquals(true, flow.value, "seed value before any changes")

        monitor.setOnline(false)
        monitor.setOnline(true)
        monitor.setOnline(false)

        // Before debounce window completes, value still reflects seed (no emission yet)
        advanceTimeBy(50)
        assertEquals(true, flow.value, "debounce window not yet elapsed; should still see seed")

        advanceTimeBy(100)
        advanceUntilIdle()
        assertEquals(false, flow.value, "after debounce window, settled value is the last change")
        monitor.close()
    }

    @Test
    fun networkStatusDebouncedState_emits_current_value_to_late_subscriber() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val flow = monitor.networkStatusDebouncedState(backgroundScope, timeoutMs = 100L)
        // Initial value is whatever FakeNetworkMonitor reports as networkStatus.value
        assertEquals(monitor.networkStatus.value, flow.value)
        monitor.close()
    }

    @Test
    fun networkStatusDebouncedState_debounces_rapid_status_changes() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val flow = monitor.networkStatusDebouncedState(backgroundScope, timeoutMs = 100L)
        val initial = flow.value

        monitor.setOnline(false)
        monitor.setOnline(true)
        monitor.setOnline(false)

        advanceTimeBy(50)
        assertEquals(initial, flow.value, "still seed during debounce window")

        advanceTimeBy(100)
        advanceUntilIdle()
        assertEquals(NetworkStatus.Unavailable, flow.value)
        monitor.close()
    }
}
