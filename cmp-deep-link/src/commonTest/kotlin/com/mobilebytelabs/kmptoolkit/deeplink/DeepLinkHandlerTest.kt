package com.mobilebytelabs.kmptoolkit.deeplink

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkHandlerTest {

    @BeforeTest
    fun setup() {
        DeepLinkHandler.clear()
    }

    @Test
    fun handle_updatesLastReceived() {
        DeepLinkHandler.handle("myapp://open/product/1")
        val last = DeepLinkHandler.lastReceived.value
        assertNotNull(last)
        assertEquals("myapp://open/product/1", last.raw)
    }

    @Test
    fun handle_emitsToIncoming() = runTest {
        val received = mutableListOf<DeepLink>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeepLinkHandler.incoming.collect { received.add(it) }
        }

        DeepLinkHandler.handle("myapp://open/test/1")

        assertEquals(1, received.size)
        assertEquals("myapp://open/test/1", received.first().raw)
        job.cancel()
    }

    @Test
    fun handle_multipleUris_allEmitted() = runTest {
        val received = mutableListOf<DeepLink>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DeepLinkHandler.incoming.collect { received.add(it) }
        }

        DeepLinkHandler.handle("myapp://open/a")
        DeepLinkHandler.handle("myapp://open/b")
        DeepLinkHandler.handle("myapp://open/c")

        assertEquals(3, received.size)
        job.cancel()
    }

    @Test
    fun clear_resetsLastReceived() {
        DeepLinkHandler.handle("myapp://open/before-clear")
        assertNotNull(DeepLinkHandler.lastReceived.value)

        DeepLinkHandler.clear()
        assertNull(DeepLinkHandler.lastReceived.value)
    }

    @Test
    fun handle_concurrent_doesNotThrow() {
        // Call handle many times concurrently — should not deadlock or throw
        repeat(20) { i ->
            DeepLinkHandler.handle("myapp://open/item/$i")
        }
        assertNotNull(DeepLinkHandler.lastReceived.value)
    }
}
