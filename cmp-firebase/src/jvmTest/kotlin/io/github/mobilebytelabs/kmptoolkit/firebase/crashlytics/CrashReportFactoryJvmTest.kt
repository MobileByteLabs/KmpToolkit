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
import kotlin.test.assertTrue

/**
 * JVM guarantees the `at pkg.Class.method(File.kt:line)` trace format, so here we
 * assert the parser recovers real `file:line` — the exact thing that lets an AI
 * point at the crashing source line.
 */
class CrashReportFactoryJvmTest {

    @Test
    fun jvm_frames_resolve_file_and_line() {
        val t = try {
            throw IllegalStateException("boom")
        } catch (e: Throwable) {
            e
        }
        val frames = t.toCrashReport().stackFrames

        assertTrue(frames.isNotEmpty(), "JVM trace should yield parsed frames")
        val top = frames.first()
        assertTrue(top.declaringClass?.contains("CrashReportFactoryJvmTest") == true, "class resolved: $top")
        assertTrue(top.method != null, "method resolved: $top")
        assertTrue(top.file?.endsWith(".kt") == true, "file resolved: $top")
        assertTrue((top.line ?: 0) > 0, "line resolved: $top")
    }
}
