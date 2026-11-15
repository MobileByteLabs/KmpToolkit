package com.mobilebytelabs.kmptoolkit.appupdate

import android.app.Activity
import android.os.Looper
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Android actual against Play's own [FakeAppUpdateManager].
 *
 * Covers the two defects that kept every Android app from ever showing an update: the init provider
 * was never declared in a manifest (context always null), and [AppUpdate.startUpdate] returned
 * "available" without launching Play's flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class AppUpdateAndroidTest {

    private lateinit var fake: FakeAppUpdateManager

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        // Robolectric does not run library init providers; attach through the provider's own seam.
        AppUpdateContext.attach(app)
        fake = FakeAppUpdateManager(app)
        AppUpdateContext.managerFactory = { fake }
        AppUpdateContext.resumed.value = null // the object outlives each test's Application
    }

    @Test
    fun initProvider_isDeclaredInTheMergedManifest() {
        // Before, no manifest declared it, so on a device the context stayed null and every check
        // returned "Context not available".
        val app = RuntimeEnvironment.getApplication()
        val provider = app.packageManager.resolveContentProvider("${app.packageName}.cmp-in-app-update-init", 0)
        assertEquals(AppUpdateInitProvider::class.java.name, provider?.name)
    }

    @Test
    fun startUpdate_withUpdateAvailable_launchesTheImmediateFlow() {
        resumedActivity()
        fake.setUpdateAvailable(2, AppUpdateType.IMMEDIATE)

        val result = pumping { AppUpdate.startUpdate(UpdateType.IMMEDIATE, AppUpdateConfig.Default) }

        assertTrue(fake.isImmediateFlowVisible, "Play's immediate update flow was never launched")
        assertIs<UpdateResult.Success>(result)
        assertTrue(result.updateInfo.isAvailable)
    }

    @Test
    fun startUpdate_withFlexibleRequested_launchesTheFlexibleFlow() {
        resumedActivity()
        fake.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)

        pumping { AppUpdate.startUpdate(UpdateType.FLEXIBLE, AppUpdateConfig.Default) }

        assertTrue(fake.isConfirmationDialogVisible, "Play's flexible update dialog was never launched")
    }

    @Test
    fun startUpdate_withNoUpdate_launchesNothing() {
        resumedActivity()
        fake.setUpdateNotAvailable()

        val result = pumping { AppUpdate.startUpdate(UpdateType.IMMEDIATE, AppUpdateConfig.Default) }

        assertFalse(fake.isImmediateFlowVisible)
        assertIs<UpdateResult.Success>(result)
        assertFalse(result.updateInfo.isAvailable)
    }

    @Test
    fun startUpdate_withTypeNotAllowed_reportsAnErrorAndLaunchesNothing() {
        resumedActivity()
        fake.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)

        val result = pumping { AppUpdate.startUpdate(UpdateType.IMMEDIATE, AppUpdateConfig.Default) }

        assertIs<UpdateResult.Error>(result)
        assertFalse(fake.isImmediateFlowVisible)
    }

    @Test
    fun awaitResumedActivity_withNoActivity_timesOutToNull() {
        assertNull(pumping { AppUpdateContext.awaitResumedActivity(timeoutMillis = 100) })
    }

    @Test
    fun awaitResumedActivity_waitsForAnActivityResumedAfterTheCall() {
        val pending = CoroutineScope(Dispatchers.IO).async { AppUpdateContext.awaitResumedActivity() }
        Thread.sleep(50)
        val activity = resumedActivity()

        assertEquals(activity, pump { pending.isCompleted }.let { pending.getCompleted() })
    }

    private fun resumedActivity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    /**
     * Runs [block] off the main thread while draining the paused main looper here — Play's Tasks
     * deliver their listeners on the main looper, which nothing else would drain.
     */
    private fun <T> pumping(block: suspend () -> T): T {
        val job = CoroutineScope(Dispatchers.IO).async { block() }
        pump { job.isCompleted }
        return job.getCompleted()
    }

    private fun pump(done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!done()) {
            check(System.currentTimeMillis() < deadline) { "timed out" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }
}
