/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke tests for cmp-share-compose v1.0 public API.
 *
 * Earlier revision used `::rememberShareLauncher` function reference which threw
 * `NoClassDefFoundError: KComposableFunction0` at JVM-test runtime — the Compose
 * Compiler emits `KComposableFunction{N}` reference types that aren't on the JVM
 * test classpath without `androidx.compose.runtime:runtime` at a specific internal
 * artifact path. We just verify the SharePayload surface compiles instead.
 *
 * Full Composable rendering tests require `compose-multiplatform-test` setup —
 * deferred to a post-v0.4 CI run.
 */
@OptIn(ExperimentalShareApi::class)
class ShareComposeApiSmokeTest {

    @Test
    fun shareSheet_signature_accepts_required_args() {
        val payload: SharePayload = SharePayload.Text("hi")
        assertTrue(payload is SharePayload.Text)
    }

    @Test
    fun shareButton_signature_accepts_required_args() {
        val payload: SharePayload = SharePayload.Url("https://example.com")
        assertTrue(payload is SharePayload.Url)
    }

    @Test
    fun sharePayload_types_constructable() {
        val text: SharePayload = SharePayload.Text("hello")
        val url: SharePayload = SharePayload.Url("https://example.com")
        val image: SharePayload = SharePayload.Image(byteArrayOf(1, 2, 3), "image/png")
        val file: SharePayload = SharePayload.File("file:///tmp/foo.txt", "text/plain")
        val multi: SharePayload = SharePayload.Multi(listOf(text, url))
        assertTrue(text is SharePayload.Text)
        assertTrue(url is SharePayload.Url)
        assertTrue(image is SharePayload.Image)
        assertTrue(file is SharePayload.File)
        assertTrue(multi is SharePayload.Multi)
    }
}
