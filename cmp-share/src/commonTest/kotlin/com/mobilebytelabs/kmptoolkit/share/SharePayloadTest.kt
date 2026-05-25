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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * commonMain DSL + payload-shape tests. Per-platform `share()` invocations are
 * tested via instrumentation in sub-plan 07.
 */
@OptIn(ExperimentalShareApi::class)
class SharePayloadTest {

    @Test
    fun text_payload_defaultMimeType_isPlainText() {
        val payload = SharePayload.Text("hello")
        assertEquals("text/plain", payload.mimeType)
        assertEquals("hello", payload.content)
    }

    @Test
    fun url_payload_holdsHref() {
        val payload = SharePayload.Url("https://example.com")
        assertEquals("https://example.com", payload.href)
    }

    @Test
    fun image_payload_equality_isContentBased() {
        val a = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png")
        val b = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png")
        val c = SharePayload.Image(byteArrayOf(1, 2, 4), "image/png")
        assertEquals(a, b, "Equal content → equal")
        assertNotEquals(a, c, "Different content → not equal")
    }

    @Test
    fun image_payload_hashCode_isStableForEqualContent() {
        val a = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png", "a.png")
        val b = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png", "a.png")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun image_payload_toString_includesByteCount() {
        val payload = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png", "x.png")
        assertTrue(payload.toString().contains("3 bytes"))
    }

    @Test
    fun multi_payload_canNest() {
        val inner = SharePayload.Multi(listOf(SharePayload.Text("a"), SharePayload.Text("b")))
        val outer = SharePayload.Multi(listOf(inner, SharePayload.Url("https://example.com")))
        assertEquals(2, outer.items.size)
        assertIs<SharePayload.Multi>(outer.items[0])
    }

    @Test
    fun shareOptions_defaults_areEmpty() {
        val opts = ShareOptions()
        assertEquals(null, opts.chooserTitle)
        assertTrue(opts.excludedActivities.isEmpty())
        assertEquals(null, opts.presentingController)
    }

    @Test
    fun shareError_singletons_compareByIdentity() {
        assertEquals(ShareError.NoHandler, ShareError.NoHandler)
        assertEquals(ShareError.UnsupportedPlatform, ShareError.UnsupportedPlatform)
        assertEquals(ShareError.UserGestureMissing, ShareError.UserGestureMissing)
        assertNotEquals(ShareError.NoHandler as ShareError, ShareError.Unknown("x") as ShareError)
    }

    @Test
    fun shareResult_failed_carriesCause() {
        val failure = ShareResult.Failed(ShareError.NoHandler)
        assertIs<ShareResult.Failed>(failure)
        assertEquals(ShareError.NoHandler, failure.cause)
    }
}
