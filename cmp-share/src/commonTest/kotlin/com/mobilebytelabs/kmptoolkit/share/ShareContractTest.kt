/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behavior contract tests for [FakeShareLauncher]. Documents the expected
 * semantics of [Share] via the Fake — every real platform impl is expected to
 * satisfy the same observable behavior (typed sealed result classes, FIFO
 * script queue, default UnsupportedPlatform on empty queue, history capture
 * for each call, all SharePayload subtypes accepted).
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 03.
 */
@OptIn(ExperimentalShareApi::class)
class ShareContractTest {

    // ---- share() result-typing contract ---------------------------------------------------

    @Test
    fun `share returns scripted Completed result`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        val r = fake.share(SharePayload.Text("hi"), ShareOptions())
        assertIs<ShareResult.Completed>(r)
    }

    @Test
    fun `share returns scripted Cancelled result`() = runTest {
        val fake = FakeShareLauncher().apply { scriptCancellation() }
        val r = fake.share(SharePayload.Url("https://x"), ShareOptions())
        assertIs<ShareResult.Cancelled>(r)
    }

    @Test
    fun `share returns scripted Failed result with typed cause`() = runTest {
        val fake = FakeShareLauncher().apply { scriptError(ShareError.NoHandler) }
        val r = fake.share(SharePayload.Text("hi"), ShareOptions())
        assertIs<ShareResult.Failed>(r)
        assertEquals(ShareError.NoHandler, r.cause)
    }

    // ---- shareHistory inspection surface --------------------------------------------------

    @Test
    fun `shareHistory captures payload and options on every call in FIFO order`() = runTest {
        val fake = FakeShareLauncher().apply {
            scriptResult(ShareResult.Completed)
            scriptResult(ShareResult.Completed)
        }
        fake.share(SharePayload.Text("a"), ShareOptions(chooserTitle = "Pick"))
        fake.share(SharePayload.Url("https://b"), ShareOptions())

        assertEquals(2, fake.shareHistory.size)
        val firstPayload = fake.shareHistory[0].payload
        assertIs<SharePayload.Text>(firstPayload)
        assertEquals("a", firstPayload.content)
        assertEquals("Pick", fake.shareHistory[0].options.chooserTitle)
        val secondPayload = fake.shareHistory[1].payload
        assertIs<SharePayload.Url>(secondPayload)
        assertEquals("https://b", secondPayload.href)
    }

    @Test
    fun `targetPackage flows through ShareOptions for direct-to-app share`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        fake.share(
            SharePayload.File(uri = "content://media/1", mimeType = "video/mp4"),
            ShareOptions(targetPackage = "com.whatsapp"),
        )
        assertEquals("com.whatsapp", fake.shareHistory[0].options.targetPackage)
        // default is null (system chooser) when not requested
        assertEquals(null, ShareOptions().targetPackage)
    }

    @Test
    fun `shareHistory captures Image payload bytes and mimeType`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic prefix
        fake.share(SharePayload.Image(bytes, "image/png", "x.png"), ShareOptions())

        val payload = fake.shareHistory.single().payload
        assertIs<SharePayload.Image>(payload)
        assertEquals("image/png", payload.mimeType)
        assertEquals("x.png", payload.filename)
        assertEquals(4, payload.bytes.size)
    }

    // ---- script queue drainage + default fallback -----------------------------------------

    @Test
    fun `empty script queue returns Failed UnsupportedPlatform`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        assertIs<ShareResult.Completed>(fake.share(SharePayload.Text("a"), ShareOptions()))
        val second = fake.share(SharePayload.Text("b"), ShareOptions())
        assertIs<ShareResult.Failed>(second)
        assertEquals(ShareError.UnsupportedPlatform, second.cause)
    }

    @Test
    fun `script queue drains FIFO across multiple share calls`() = runTest {
        val fake = FakeShareLauncher().apply {
            scriptResult(ShareResult.Completed)
            scriptResult(ShareResult.Cancelled)
            scriptResult(ShareResult.Failed(ShareError.UserGestureMissing))
        }
        assertIs<ShareResult.Completed>(fake.share(SharePayload.Text("1"), ShareOptions()))
        assertIs<ShareResult.Cancelled>(fake.share(SharePayload.Text("2"), ShareOptions()))
        val third = fake.share(SharePayload.Text("3"), ShareOptions())
        assertIs<ShareResult.Failed>(third)
        assertEquals(ShareError.UserGestureMissing, third.cause)
    }

    // ---- All SharePayload subtypes accepted -----------------------------------------------

    @Test
    fun `accepts all SharePayload subtypes — Text Url Image File Multi`() = runTest {
        val fake = FakeShareLauncher().apply { repeat(5) { scriptResult(ShareResult.Completed) } }
        fake.share(SharePayload.Text("t"), ShareOptions())
        fake.share(SharePayload.Url("https://x"), ShareOptions())
        fake.share(SharePayload.Image(byteArrayOf(1), "image/png"), ShareOptions())
        fake.share(SharePayload.File("file:///tmp/x", "application/octet-stream"), ShareOptions())
        fake.share(SharePayload.Multi(listOf(SharePayload.Text("a"), SharePayload.Url("https://b"))), ShareOptions())

        assertEquals(5, fake.shareHistory.size)
        assertIs<SharePayload.Text>(fake.shareHistory[0].payload)
        assertIs<SharePayload.Url>(fake.shareHistory[1].payload)
        assertIs<SharePayload.Image>(fake.shareHistory[2].payload)
        assertIs<SharePayload.File>(fake.shareHistory[3].payload)
        assertIs<SharePayload.Multi>(fake.shareHistory[4].payload)
    }

    // ---- reset() ---------------------------------------------------------------------------

    @Test
    fun `reset clears history and script queue`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        fake.share(SharePayload.Text("x"), ShareOptions())
        assertEquals(1, fake.shareHistory.size)

        fake.reset()
        assertEquals(0, fake.shareHistory.size)
        val r = fake.share(SharePayload.Text("y"), ShareOptions())
        assertIs<ShareResult.Failed>(r)
        assertEquals(ShareError.UnsupportedPlatform, r.cause)
    }

    // ---- ShareOptions round-trip ----------------------------------------------------------

    @Test
    fun `ShareOptions defaults round-trip cleanly through shareHistory`() = runTest {
        val fake = FakeShareLauncher().apply { scriptResult(ShareResult.Completed) }
        fake.share(SharePayload.Text("x")) // default ShareOptions

        val opts = fake.shareHistory.single().options
        assertTrue(opts.chooserTitle == null)
        assertTrue(opts.excludedActivities.isEmpty())
    }
}
