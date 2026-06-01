/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavior contract tests for [FakeIntentLauncher]. Documents the expected
 * semantics of [IntentLauncher] + [SystemIntents] via the Fake — every real
 * platform impl is expected to satisfy the same observable behavior (typed
 * sealed result classes, FIFO script queue, default UnsupportedPlatform on
 * empty queue, history capture for each call).
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 03.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
class IntentLauncherContractTest {

    // ---- launch() result-typing contract ---------------------------------------------------

    @Test
    fun `launch returns the scripted Ok result`() = runTest {
        val fake = FakeIntentLauncher().apply {
            scriptResult(IntentResult.Ok(IntentData(uri = "https://x", mimeType = null)))
        }
        val result = fake.launch {
            action = "VIEW"
            data = "https://x"
        }
        assertIs<IntentResult.Ok>(result)
        assertEquals("https://x", result.data?.uri)
    }

    @Test
    fun `launch returns the scripted Cancelled result`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptCancellation() }
        val result = fake.launch { resultContract = ResultContracts.PickImage }
        assertIs<IntentResult.Cancelled>(result)
    }

    @Test
    fun `launch returns the scripted Failed result with typed cause`() = runTest {
        val fake = FakeIntentLauncher().apply {
            scriptError(IntentError.NoHandler)
        }
        val result = fake.launch { action = "EDIT" }
        assertIs<IntentResult.Failed>(result)
        assertEquals(IntentError.NoHandler, result.cause)
    }

    // ---- launchHistory inspection surface -------------------------------------------------

    @Test
    fun `launchHistory captures every call's IntentBuilder state in FIFO order`() = runTest {
        val fake = FakeIntentLauncher().apply {
            scriptResult(IntentResult.Ok(null))
            scriptCancellation()
        }
        fake.launch {
            action = "VIEW"
            data = "https://a"
        }
        fake.launch {
            action = "PICK"
            type = "image/*"
        }

        assertEquals(2, fake.launchHistory.size)
        assertEquals("VIEW", fake.launchHistory[0].action)
        assertEquals("https://a", fake.launchHistory[0].data)
        assertEquals("PICK", fake.launchHistory[1].action)
        assertEquals("image/*", fake.launchHistory[1].mimeType)
    }

    @Test
    fun `launchHistory captures the resultContract when set`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        fake.launch { resultContract = ResultContracts.PickImage }
        assertEquals(ResultContracts.PickImage, fake.launchHistory.single().resultContract)
    }

    // ---- script queue drainage + default fallback -----------------------------------------

    @Test
    fun `empty script queue returns Failed UnsupportedPlatform`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        // 1st call drains the only scripted result
        assertIs<IntentResult.Ok>(fake.launch { action = "VIEW" })
        // 2nd call — queue empty → default UnsupportedPlatform
        val second = fake.launch { action = "VIEW" }
        assertIs<IntentResult.Failed>(second)
        assertEquals(IntentError.UnsupportedPlatform, second.cause)
    }

    @Test
    fun `script queue drains FIFO across launch + systemIntents calls`() = runTest {
        val fake = FakeIntentLauncher().apply {
            scriptResult(IntentResult.Ok(null)) // for launch()
            scriptResult(IntentResult.Cancelled) // for openAppSettings()
            scriptResult(IntentResult.Failed(IntentError.UserGestureMissing)) // for next launch()
        }
        assertIs<IntentResult.Ok>(fake.launch { action = "VIEW" })
        assertIs<IntentResult.Cancelled>(fake.systemIntents.openAppSettings())
        val third = fake.launch { action = "VIEW" }
        assertIs<IntentResult.Failed>(third)
        assertEquals(IntentError.UserGestureMissing, third.cause)
    }

    // ---- systemIntents call-history --------------------------------------------------------

    @Test
    fun `systemIntents openAppSettings is recorded in systemIntentsCallHistory`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        assertIs<IntentResult.Ok>(fake.systemIntents.openAppSettings())
        assertTrue("openAppSettings" in fake.systemIntentsCallHistory)
    }

    @Test
    fun `systemIntents createDocument records suggestedName and mimeType`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        fake.systemIntents.createDocument("report.pdf", "application/pdf")
        assertEquals("createDocument:application/pdf:report.pdf", fake.systemIntentsCallHistory.single())
    }

    // ---- reset() ---------------------------------------------------------------------------

    @Test
    fun `reset clears history and script queue`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        fake.launch { action = "VIEW" }
        assertEquals(1, fake.launchHistory.size)

        fake.reset()
        assertEquals(0, fake.launchHistory.size)
        assertEquals(0, fake.systemIntentsCallHistory.size)
        // Empty queue after reset → default fallback returned
        val r = fake.launch { action = "VIEW" }
        assertIs<IntentResult.Failed>(r)
        assertEquals(IntentError.UnsupportedPlatform, r.cause)
    }

    // ---- IntentData round-trip --------------------------------------------------------------

    @Test
    fun `Ok result with null data carries null payload`() = runTest {
        val fake = FakeIntentLauncher().apply { scriptResult(IntentResult.Ok(null)) }
        val r = fake.launch { action = "PICK" }
        assertIs<IntentResult.Ok>(r)
        assertNull(r.data)
    }
}
