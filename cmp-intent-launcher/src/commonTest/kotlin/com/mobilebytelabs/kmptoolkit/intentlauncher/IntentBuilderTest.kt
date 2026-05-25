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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * commonMain DSL builder + ResultContracts tests.
 * Per-platform `.launch()` invocations tested via instrumentation in sub-plan 07.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
class IntentBuilderTest {

    @Test
    fun builder_chains_andCollectsState() {
        val builder = IntentBuilder().apply {
            action("android.intent.action.VIEW")
            data("https://example.com")
            type("text/plain")
            category("android.intent.category.BROWSABLE")
            flag(0x1)
            extra("k1", "v1")
            extra("k2", 42)
            packageName("com.example.app")
            result(ResultContracts.PickImage)
        }
        assertEquals("android.intent.action.VIEW", builder.action)
        assertEquals("https://example.com", builder.data)
        assertEquals("text/plain", builder.type)
        assertEquals(listOf("android.intent.category.BROWSABLE"), builder.categories)
        assertEquals(0x1, builder.flags)
        assertEquals("v1", builder.extras["k1"])
        assertEquals(42, builder.extras["k2"])
        assertEquals("com.example.app", builder.packageName)
        assertEquals(ResultContracts.PickImage, builder.resultContract)
    }

    @Test
    fun builder_onUnsupported_callback_isStored() {
        var fired = false
        val builder = IntentBuilder().apply {
            onUnsupported {
                fired = true
                IntentResult.Failed(IntentError.UnsupportedPlatform)
            }
        }
        assertNotNull(builder.onUnsupportedHandler)
        val res = builder.onUnsupportedHandler?.invoke()
        assertEquals(true, fired)
        assertIs<IntentResult.Failed>(res)
    }

    @Test
    fun builder_emptyByDefault() {
        val builder = IntentBuilder()
        assertNull(builder.action)
        assertNull(builder.data)
        assertNull(builder.type)
        assertEquals(emptyList(), builder.categories)
        assertEquals(0, builder.flags)
        assertEquals(emptyMap(), builder.extras)
        assertNull(builder.packageName)
        assertNull(builder.resultContract)
        assertNull(builder.onUnsupportedHandler)
    }

    @Test
    fun resultContracts_pickImage_parsesUri() {
        val data = IntentData(uri = "content://media/image/123", mimeType = "image/png")
        val parsed = ResultContracts.PickImage.parse(data)
        assertEquals("content://media/image/123", parsed)
    }

    @Test
    fun resultContracts_pickImage_nullData_returnsNull() {
        assertNull(ResultContracts.PickImage.parse(null))
    }

    @Test
    fun resultContracts_custom_parsesViaLambda() {
        val contract = ResultContracts.Custom<Int>(
            parse = { data -> (data?.extras?.get("count") as? Int) ?: 0 },
            resultType = Int::class,
        )
        val data = IntentData(extras = mapOf("count" to 7))
        assertEquals(7, contract.parse(data))
        assertEquals(0, contract.parse(null))
    }

    @Test
    fun intentResult_sealedShapes_areDistinct() {
        val ok = IntentResult.Ok(IntentData(uri = "x"))
        val cancelled: IntentResult = IntentResult.Cancelled
        val failed: IntentResult = IntentResult.Failed(IntentError.NoHandler)
        assertIs<IntentResult.Ok>(ok)
        assertIs<IntentResult.Cancelled>(cancelled)
        assertIs<IntentResult.Failed>(failed)
    }
}
