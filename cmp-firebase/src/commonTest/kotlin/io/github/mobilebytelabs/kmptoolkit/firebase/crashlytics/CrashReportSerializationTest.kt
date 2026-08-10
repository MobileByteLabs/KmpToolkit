/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashReportSerializationTest {

    private val report = CrashReport(
        throwableClass = "IllegalStateException",
        message = "boom",
        fatal = true,
        platform = "jvm",
        timestamp = "2026-08-10T00:00:00Z",
        causeChain = listOf(CrashCause("IllegalArgumentException", "root")),
        stackFrames = listOf(
            StackFrame(
                declaringClass = "com.example.Foo",
                method = "bar",
                file = "Foo.kt",
                line = 42,
                raw = "at com.example.Foo.bar(Foo.kt:42)",
            ),
        ),
        customKeys = mapOf("flavor" to "prod"),
        breadcrumbs = listOf("tapped save"),
        rawStackTrace = "IllegalStateException: boom\n\tat com.example.Foo.bar(Foo.kt:42)",
    )

    @Test
    fun json_is_ai_feedable_and_self_describing() {
        val json = report.toJson()
        // The keys an AI needs to reason about the crash must be present.
        assertTrue("throwableClass" in json)
        assertTrue("causeChain" in json)
        assertTrue("stackFrames" in json)
        assertTrue("Foo.kt" in json && "42" in json, "file:line must survive serialization")
    }

    @Test
    fun round_trips_without_loss() {
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(
            CrashReport.serializer(),
            report.toJson(),
        )
        assertEquals(report.throwableClass, decoded.throwableClass)
        assertEquals(report.message, decoded.message)
        assertEquals(report.fatal, decoded.fatal)
        assertEquals(report.causeChain, decoded.causeChain)
        assertEquals(report.stackFrames, decoded.stackFrames)
        assertEquals(report.customKeys, decoded.customKeys)
    }

    @Test
    fun pretty_and_compact_carry_same_data() {
        val compact = Json { ignoreUnknownKeys = true }
            .decodeFromString(CrashReport.serializer(), report.toJson(pretty = false))
        val pretty = Json { ignoreUnknownKeys = true }
            .decodeFromString(CrashReport.serializer(), report.toJson(pretty = true))
        assertEquals(compact, pretty)
    }
}
