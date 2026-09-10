package io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android host run of the shared UI scenarios.
 *
 * `@RunWith(RobolectricTestRunner::class)` is the whole point: Robolectric supplies a real
 * `android.os.Build` (FINGERPRINT = "robolectric"), which is what
 * `androidx.compose.ui.test`'s `AndroidComposeUiTestEnvironment` reads to pick an idling
 * strategy. Without it the field is null under the android.jar stub and every composition
 * fails before rendering.
 *
 * `@Config(sdk = [ROBOLECTRIC_MAX_SDK])` pins the emulated API level: this module compiles against
 * SDK 37, but Robolectric 4.14.1 ships images only up to 35 and otherwise refuses the class with
 * "Package targetSdkVersion=37 > maxSdkVersion=35". These scenarios assert connectivity-driven
 * composition, which is not API-level sensitive, so emulating 35 costs nothing. Raise it when
 * Robolectric ships a 37 image.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_MAX_SDK])
class ConnectivityUiScenarioAndroidTest : ConnectivityUiScenarios()

/** Highest API level with a Robolectric image in the pinned version. */
private const val ROBOLECTRIC_MAX_SDK = 35
