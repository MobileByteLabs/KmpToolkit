package com.mobilebytelabs.kmptoolkit.deeplink

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkIntentTest {

    @Before
    fun setup() {
        DeepLinkHandler.clear()
    }

    @Test
    fun handleDeepLinkIntent_actionViewWithData_emitsToHandler() = runTest {
        val activity = mock(ComponentActivity::class.java)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("myapp://open/product/99"))
        `when`(activity.intent).thenReturn(intent)

        val received = mutableListOf<DeepLink>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeepLinkHandler.incoming.collect { received.add(it) }
        }

        activity.handleDeepLinkIntent(intent)
        testScheduler.advanceUntilIdle()

        assertEquals(1, received.size)
        assertEquals("myapp://open/product/99", received.first().raw)
        job.cancel()
    }

    @Test
    fun handleDeepLinkIntent_nullIntent_noop() = runTest {
        val received = mutableListOf<DeepLink>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeepLinkHandler.incoming.collect { received.add(it) }
        }

        val activity = mock(ComponentActivity::class.java)
        activity.handleDeepLinkIntent(null)
        testScheduler.advanceUntilIdle()

        assertEquals(0, received.size)
        job.cancel()
    }

    @Test
    fun handleDeepLinkIntent_intentWithoutData_noop() = runTest {
        val received = mutableListOf<DeepLink>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeepLinkHandler.incoming.collect { received.add(it) }
        }

        val intent = Intent(Intent.ACTION_VIEW) // no URI data
        val activity = mock(ComponentActivity::class.java)
        `when`(activity.intent).thenReturn(intent)
        activity.handleDeepLinkIntent(intent)
        testScheduler.advanceUntilIdle()

        assertEquals(0, received.size)
        job.cancel()
    }
}
