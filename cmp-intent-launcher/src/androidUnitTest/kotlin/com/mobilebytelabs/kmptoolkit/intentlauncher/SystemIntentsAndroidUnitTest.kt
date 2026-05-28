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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android local-JVM unit test (no instrumentation): when [IntentLauncherInitProvider]
 * has NOT run (which is always the case in a local Robolectric-less unit test, since
 * the Android ContentProvider boot order doesn't fire), both entry points must return
 * a `Failed` result with a clear initialization-error message rather than crashing.
 *
 * Instrumented test (real device / emulator) covering the proxy Activity SAF
 * round-trip is tracked as a follow-up — see Phase 13 SystemIntents sub-plan §Tests.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
class SystemIntentsAndroidUnitTest {

    @Test
    fun openAppSettings_withoutInitProvider_returnsFailedWithMessage() = runTest {
        val result = SystemIntents.openAppSettings()
        assertIs<IntentResult.Failed>(result)
        val cause = result.cause
        assertIs<IntentError.Unknown>(cause)
        assertNotNull(cause.message)
        assertTrue(
            cause.message.contains("IntentLauncherContext not initialized", ignoreCase = true),
            "Expected init-not-initialized message, was: ${cause.message}",
        )
    }

    @Test
    fun createDocument_withoutInitProvider_returnsFailedWithMessage() = runTest {
        val result = SystemIntents.createDocument("file.txt", "text/plain")
        assertIs<IntentResult.Failed>(result)
        val cause = result.cause
        assertIs<IntentError.Unknown>(cause)
        assertTrue(
            cause.message.contains("IntentLauncherContext not initialized", ignoreCase = true),
            "Expected init-not-initialized message, was: ${cause.message}",
        )
    }
}
