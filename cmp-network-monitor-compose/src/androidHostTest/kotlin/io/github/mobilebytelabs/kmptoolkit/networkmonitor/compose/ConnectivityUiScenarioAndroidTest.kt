package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK

/**
 * Android host run of the shared UI scenarios.
 *
 * `@RunWith(RobolectricTestRunner::class)` is the whole point: Robolectric supplies a real
 * `android.os.Build` (FINGERPRINT = "robolectric"), which is what
 * `androidx.compose.ui.test`'s `AndroidComposeUiTestEnvironment` reads to pick an idling
 * strategy. Without it the field is null under the android.jar stub and every composition
 * fails before rendering.
 *
 * The emulated API level comes from `robolectricSdk` in gradle/libs.versions.toml, applied via
 * robolectric-host-test.gradle.kts, which code-generates ROBOLECTRIC_SDK — no per-file constant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ConnectivityUiScenarioAndroidTest : ConnectivityUiScenarios()
