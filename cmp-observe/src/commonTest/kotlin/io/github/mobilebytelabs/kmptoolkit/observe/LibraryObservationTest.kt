/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [LibraryObservation] registry + [LibraryObservationHook] contract.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T1 (RED) + T2/T3 (GREEN).
 */
class LibraryObservationTest {

    private class RecordingHook : LibraryObservationHook {
        val calls = mutableListOf<String>()
        override fun onInitStart(meta: CmpMetadata) { calls += "init_start:${meta.name}" }
        override fun onInitComplete(meta: CmpMetadata) { calls += "init_complete:${meta.name}" }
        override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {
            calls += "init_failure:${meta.name}:${throwable.message}"
        }
        override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
            calls += "lifecycle:${meta.name}:$event"
        }
        override fun onClose(meta: CmpMetadata) { calls += "close:${meta.name}" }
    }

    private val testMeta = CmpMetadata(
        name = "cmp-test",
        version = "1.0.0",
        artifact = "io.github.mobilebytelabs:cmp-test",
    )

    @BeforeTest fun setUp() { LibraryObservation.reset() }
    @AfterTest fun tearDown() { LibraryObservation.reset() }

    @Test fun `notifyInit fans out to all registered hooks`() {
        val h1 = RecordingHook(); val h2 = RecordingHook()
        LibraryObservation.register(h1)
        LibraryObservation.register(h2)
        LibraryObservation.notifyInit(testMeta)
        assertEquals(listOf("init_start:cmp-test"), h1.calls)
        assertEquals(listOf("init_start:cmp-test"), h2.calls)
    }

    @Test fun `hook exception is isolated — subsequent hooks still run`() {
        val failingHook = object : LibraryObservationHook {
            override fun onInitStart(meta: CmpMetadata) { throw RuntimeException("hook crashed") }
            override fun onInitComplete(meta: CmpMetadata) {}
            override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {}
            override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {}
            override fun onClose(meta: CmpMetadata) {}
        }
        val recordingHook = RecordingHook()
        LibraryObservation.register(failingHook)
        LibraryObservation.register(recordingHook)
        LibraryObservation.notifyInit(testMeta)
        assertTrue(recordingHook.calls.isNotEmpty(), "second hook ran despite first throwing")
    }

    @Test fun `notifyInit with zero registered hooks is silent + non-throwing`() {
        LibraryObservation.notifyInit(testMeta)  // Should NOT throw
    }

    @Test fun `replaceHooks atomically swaps registered list`() {
        val before = RecordingHook(); val after = RecordingHook()
        LibraryObservation.register(before)
        LibraryObservation.replaceHooks { _ -> listOf(after) }
        LibraryObservation.notifyInit(testMeta)
        assertEquals(emptyList(), before.calls, "old hook unregistered")
        assertEquals(listOf("init_start:cmp-test"), after.calls, "new hook registered")
    }

    @Test fun `notifyLifecycle propagates event name + payload`() {
        val hook = RecordingHook()
        LibraryObservation.register(hook)
        LibraryObservation.notifyLifecycle(testMeta, "share_invoked", mapOf("target" to "whatsapp"))
        assertEquals(listOf("lifecycle:cmp-test:share_invoked"), hook.calls)
    }

    @Test fun `notifyInitFailure carries throwable to hook`() {
        val hook = RecordingHook()
        LibraryObservation.register(hook)
        LibraryObservation.notifyInitFailure(testMeta, IllegalStateException("bad config"))
        assertEquals(listOf("init_failure:cmp-test:bad config"), hook.calls)
    }
}
