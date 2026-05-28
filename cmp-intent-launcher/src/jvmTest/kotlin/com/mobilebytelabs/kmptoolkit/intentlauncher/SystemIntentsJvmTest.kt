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
import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * JVM Desktop smoke test for [SystemIntents].
 *
 * In headless CI the AWT `JFileChooser` constructor throws `HeadlessException`. The
 * impl catches via the surrounding coroutine + returns `IntentResult.Failed`. We
 * assert that contract — the picker is never run interactively from a test.
 *
 * `openAppSettings()` spawns `ProcessBuilder` to a non-existent shell command on
 * CI workers (no GUI shell handler registered), so it returns Failed too. We only
 * assert the return type is `IntentResult` — the per-environment exit code is
 * non-deterministic.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
class SystemIntentsJvmTest {

    @Test
    fun createDocument_headless_returnsFailedNotThrows() = runTest {
        if (!GraphicsEnvironment.isHeadless()) {
            // On a developer machine with a display, this would open a real dialog
            // and block the test. Skip — real-display verification is manual.
            return@runTest
        }
        val result = SystemIntents.createDocument("sample.pdf", "application/pdf")
        assertIs<IntentResult.Failed>(result)
        assertNotNull(result.cause)
    }

    @Test
    fun openAppSettings_returnsIntentResult() = runTest {
        val result = SystemIntents.openAppSettings()
        // Don't constrain to Ok vs Failed — depends on whether ProcessBuilder's target
        // shell command exists on the CI worker. Both are valid contract outcomes.
        assertNotNull(result)
        when (result) {
            is IntentResult.Ok -> assertNotNull(result.data)
            is IntentResult.Failed -> assertNotNull(result.cause)
            is IntentResult.Cancelled -> Unit // not expected but valid
        }
    }
}
