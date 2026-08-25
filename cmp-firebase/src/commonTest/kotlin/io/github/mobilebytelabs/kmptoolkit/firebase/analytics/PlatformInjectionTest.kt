/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the single-analytics-dashboard invariant: every event carries a
 * [ParamKeys.PLATFORM] (`kmp_platform`) tag, and a manually-set platform wins.
 * Regression guard for the auto-injection all Firebase/MP/Stub helpers rely on.
 */
class PlatformInjectionTest {

    @Test
    fun with_platform_injects_kmp_platform_param() {
        val tagged = AnalyticsEvent(EventTypes.SCREEN_VIEW).withPlatform("android")
        assertEquals("android", tagged.extras.first { it.key == ParamKeys.PLATFORM }.value)
    }

    @Test
    fun with_platform_respects_manual_override() {
        val manual = AnalyticsEvent(EventTypes.BUTTON_CLICK)
            .withParam(ParamKeys.PLATFORM, "android-tv")
            .withPlatform("android")
        // manual value must not be overwritten by auto-injection
        val values = manual.extras.filter { it.key == ParamKeys.PLATFORM }.map { it.value }
        assertEquals(listOf("android-tv"), values)
    }
}
