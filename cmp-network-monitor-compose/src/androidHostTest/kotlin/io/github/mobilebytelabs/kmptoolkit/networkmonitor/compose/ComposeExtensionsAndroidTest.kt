package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.setApplicationContext
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK

/**
 * Android host run of the shared compose-extension scenarios.
 *
 * `NetworkMonitorProvider.install()` delegates to `createNetworkMonitor()`, which on Android
 * throws `IllegalStateException("Application context not available…")` unless the init
 * ContentProvider has run — it never does in a host test. Robolectric supplies a real
 * `Application`, and `setApplicationContext` is the library's own documented injection point for
 * exactly this case, so the scenario runs against the real Android actual rather than being
 * skipped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ComposeExtensionsAndroidTest : ComposeExtensionsScenarios() {

    @Before
    fun installApplicationContext() {
        setApplicationContext(RuntimeEnvironment.getApplication())
    }
}
