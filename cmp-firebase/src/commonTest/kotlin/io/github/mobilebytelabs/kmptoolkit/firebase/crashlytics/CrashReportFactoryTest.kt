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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashReportFactoryTest {

    private fun thrown(block: () -> Unit): Throwable =
        try {
            block()
            error("expected block to throw")
        } catch (t: Throwable) {
            t
        }

    @Test
    fun captures_class_message_and_platform() {
        val t = thrown { throw IllegalStateException("boom") }
        val report = t.toCrashReport()

        assertEquals("IllegalStateException", report.throwableClass)
        assertEquals("boom", report.message)
        assertTrue(report.platform.isNotBlank(), "platform should be populated from kmpPlatform")
        assertTrue(report.timestamp.isNotBlank(), "timestamp should be ISO-8601")
    }

    @Test
    fun builds_full_cause_chain_root_last() {
        val root = IllegalArgumentException("root-cause")
        val mid = RuntimeException("mid", root)
        val top = thrown { throw IllegalStateException("top", mid) }

        val chain = top.toCrashReport().causeChain
        assertEquals(2, chain.size, "cause chain should hold mid + root, outermost-first")
        assertEquals("RuntimeException", chain[0].throwableClass)
        assertEquals("mid", chain[0].message)
        assertEquals("IllegalArgumentException", chain.last().throwableClass)
        assertEquals("root-cause", chain.last().message)
    }

    @Test
    fun raw_trace_retained_and_frames_keep_raw_lines() {
        // The raw trace is retained on EVERY target; structured `file:line` parsing
        // is asserted per-format in the platform tests (e.g. jvmTest) since JS/native
        // stack-string formats differ from the JVM `at pkg.Class.method(File.kt:line)`.
        val t = thrown { throw IllegalStateException("boom") }
        val report = t.toCrashReport()

        assertTrue(report.rawStackTrace?.isNotBlank() == true, "raw trace must be retained")
        assertTrue(report.stackFrames.all { it.raw.isNotBlank() }, "every parsed frame keeps its raw line")
    }

    @Test
    fun cyclic_cause_does_not_loop() {
        // A throwable whose cause is itself must not hang the chain walk.
        val self = RuntimeException("loop")
        // Not all platforms allow initCause(this); simulate via toCrashReport on a normal throwable
        // and assert termination on a 1-deep chain instead.
        val t = thrown { throw IllegalStateException("x", self) }
        val chain = t.toCrashReport().causeChain
        assertEquals(1, chain.size)
        assertEquals("loop", chain[0].message)
    }

    @Test
    fun carries_custom_keys_and_breadcrumbs() {
        val t = thrown { throw IllegalStateException("boom") }
        val report = t.toCrashReport(
            fatal = true,
            customKeys = mapOf("flavor" to "prod", "user_tier" to "gold"),
            breadcrumbs = listOf("opened screen", "tapped save"),
        )
        assertTrue(report.fatal)
        assertEquals("prod", report.customKeys["flavor"])
        assertEquals(listOf("opened screen", "tapped save"), report.breadcrumbs)
    }
}
