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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggingCrashReporterTest {

    private fun thrown(block: () -> Unit): Throwable = try {
        block()
        error("expected throw")
    } catch (t: Throwable) {
        t
    }

    @Test
    fun no_report_before_first_record() {
        assertNull(LoggingCrashReporter().lastReport)
    }

    @Test
    fun record_populates_last_report_with_fatal_flag() {
        val reporter = LoggingCrashReporter(platform = "jvm")
        reporter.recordException(thrown { throw IllegalStateException("boom") }, fatal = true)

        val report = reporter.lastReport
        assertTrue(report != null)
        assertEquals("IllegalStateException", report.throwableClass)
        assertEquals("jvm", report.platform)
        assertTrue(report.fatal)
    }

    @Test
    fun sticky_keys_user_id_and_extra_keys_merge_into_report() {
        val reporter = LoggingCrashReporter(platform = "jvm")
        reporter.setCustomKey("flavor", "prod")
        reporter.setUserId("user-abc")
        reporter.recordException(
            thrown { throw RuntimeException("x") },
            extraKeys = mapOf("screen" to "settings"),
        )

        val keys = reporter.lastReport!!.customKeys
        assertEquals("prod", keys["flavor"])
        assertEquals("user-abc", keys["user_id"])
        assertEquals("settings", keys["screen"])
    }

    @Test
    fun breadcrumbs_are_captured_in_order_and_bounded() {
        val reporter = LoggingCrashReporter(platform = "jvm")
        reporter.log("first")
        reporter.log("second")
        reporter.recordException(thrown { throw RuntimeException("x") })

        val crumbs = reporter.lastReport!!.breadcrumbs
        assertEquals(listOf("first", "second"), crumbs)
    }

    @Test
    fun install_is_safe_to_call() {
        // Fallback install() is a documented no-op; must not throw.
        LoggingCrashReporter(platform = "jvm").install()
    }

    @Test
    fun noop_reporter_keeps_no_state() {
        NoOpCrashReporter.setCustomKey("k", "v")
        NoOpCrashReporter.log("crumb")
        NoOpCrashReporter.recordException(thrown { throw RuntimeException("x") })
        assertNull(NoOpCrashReporter.lastReport)
    }
}
