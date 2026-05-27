package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkMonitorProviderVersionTest {

    @BeforeTest
    fun setUp() {
        NetworkMonitorProvider.reset() // start each test from a clean slate
    }

    @AfterTest
    fun tearDown() {
        NetworkMonitorProvider.reset()
    }

    @Test
    fun version_increments_on_first_install() = runTest {
        val before = NetworkMonitorProvider.version.value
        NetworkMonitorProvider.install()
        assertEquals(before + 1, NetworkMonitorProvider.version.value)
    }

    @Test
    fun version_does_not_increment_on_redundant_install() = runTest {
        NetworkMonitorProvider.install()
        val afterFirst = NetworkMonitorProvider.version.value
        NetworkMonitorProvider.install() // redundant — same instance returned
        NetworkMonitorProvider.install() // redundant
        assertEquals(afterFirst, NetworkMonitorProvider.version.value)
    }

    @Test
    fun version_increments_on_reset() = runTest {
        NetworkMonitorProvider.install()
        val beforeReset = NetworkMonitorProvider.version.value
        NetworkMonitorProvider.reset()
        assertEquals(beforeReset + 1, NetworkMonitorProvider.version.value)
    }

    @Test
    fun version_increments_again_on_install_after_reset() = runTest {
        NetworkMonitorProvider.install()
        NetworkMonitorProvider.reset()
        val beforeSecondInstall = NetworkMonitorProvider.version.value
        NetworkMonitorProvider.install()
        assertEquals(beforeSecondInstall + 1, NetworkMonitorProvider.version.value)
    }

    @Test
    fun consecutive_resets_on_empty_state_do_not_bump_version() = runTest {
        // After @BeforeTest reset(), instance is null. Further resets should be no-ops
        // to avoid spurious recomposition for reactive consumers (rememberNetworkMonitor).
        val before = NetworkMonitorProvider.version.value
        NetworkMonitorProvider.reset()
        NetworkMonitorProvider.reset()
        NetworkMonitorProvider.reset()
        assertEquals(before, NetworkMonitorProvider.version.value)
    }
}
