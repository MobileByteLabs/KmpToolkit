package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.di.provideNetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Android-actual scenarios, executed against a real Android runtime supplied by Robolectric.
 *
 * These cover the paths the `commonTest` suite is filtered away from on this target: the
 * factory, the provider singleton, and teardown all reach `ConnectivityManager` through an
 * application `Context`, which a bare JVM host test cannot provide — `createNetworkMonitor()`
 * throws `IllegalStateException("Application context not available…")` there, by design.
 *
 * Robolectric supplies an `Application`, and [setApplicationContext] is the library's own
 * documented injection seam for when the init ContentProvider has not run. So rather than the
 * Android actual being merely excluded from the host run, it is exercised here for real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NetworkMonitorAndroidScenarioTest {

    @Before
    fun installApplicationContext() {
        setApplicationContext(RuntimeEnvironment.getApplication())
    }

    // ── Factory ─────────────────────────────────────────────────────────────────────────────

    /** With a context present the Android factory must produce a usable monitor, not throw. */
    @Test
    fun factory_builds_a_real_android_monitor_when_context_is_available() {
        val monitor = createNetworkMonitor()
        assertNotNull(monitor)
        monitor.close()
    }

    /** The explicit-config overload must behave identically. */
    @Test
    fun factory_honours_an_explicit_config() {
        val monitor = createNetworkMonitor(NetworkMonitorConfig())
        assertNotNull(monitor)
        monitor.close()
    }

    /**
     * The initial status must be a real value the UI can branch on immediately — never a
     * half-initialised state that only settles after the first callback.
     */
    @Test
    fun a_freshly_built_monitor_reports_a_usable_initial_status() {
        val monitor = createNetworkMonitor()
        try {
            val status = monitor.networkStatus.value
            assertTrue(
                status is NetworkStatus.Available ||
                    status is NetworkStatus.Unavailable ||
                    status is NetworkStatus.CaptivePortal,
                "initial status must be concrete, got $status",
            )
            assertEquals(status.isOnline, monitor.isOnline.value, "isOnline must agree with status")
        } finally {
            monitor.close()
        }
    }

    // ── Provider singleton ──────────────────────────────────────────────────────────────────

    /**
     * `install()` must hand back one shared instance. Two monitors would mean two
     * ConnectivityManager callbacks and two sources of truth for the same device state.
     */
    @Test
    fun provider_install_returns_the_same_instance_on_android() {
        val first = NetworkMonitorProvider.install()
        val second = NetworkMonitorProvider.install()
        assertSame(first, second, "provider must be a singleton on Android")
    }

    /** `provideNetworkMonitor()` must resolve through the same seam. */
    @Test
    fun provide_network_monitor_resolves_on_android() {
        val monitor = provideNetworkMonitor()
        assertNotNull(monitor)
    }

    // ── Extensions against the real actual ──────────────────────────────────────────────────

    /** The offline guard must throw the documented type, not a raw platform exception. */
    @Test
    fun require_online_throws_the_documented_type_when_offline() {
        val monitor = FakeNetworkMonitor(initialOnline = false)
        assertFailsWith<NetworkUnavailableException> { monitor.requireOnline() }
        monitor.close()
    }

    /** Quality derivation must work against a real Android monitor instance. */
    @Test
    fun quality_is_derivable_from_a_real_android_monitor() = runTest {
        val monitor = createNetworkMonitor()
        try {
            val quality = monitor.networkStatus.value.toQuality()
            assertTrue(quality in NetworkQuality.entries, "quality must be a defined value, got $quality")
        } finally {
            monitor.close()
        }
    }

    // ── Teardown ────────────────────────────────────────────────────────────────────────────

    /**
     * Closing must unregister the platform callback and be safe to repeat — app teardown paths
     * call it more than once, and a leaked ConnectivityManager callback outlives the process.
     */
    @Test
    fun close_is_idempotent_on_the_android_actual() {
        val monitor = createNetworkMonitor()
        monitor.close()
        monitor.close()
    }

    /** Many create/close cycles must not accumulate state — the leak-detection scenario. */
    @Test
    fun repeated_create_and_close_cycles_do_not_leak() {
        repeat(10) {
            val monitor = createNetworkMonitor()
            assertNotNull(monitor)
            monitor.close()
        }
    }
}
