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
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * commonMain contract tests for [SystemIntents].
 *
 * Verifies the cross-platform surface shape — that both entry points exist, are
 * suspend-callable from commonMain without per-platform imports, and return an
 * [IntentResult] of the documented sealed hierarchy.
 *
 * Per-platform behaviour (Android proxy Activity, iOS UIDocumentPickerViewController,
 * JVM JFileChooser, JS showSaveFilePicker) is exercised by per-platform test source
 * sets — see [SystemIntentsJvmTest] for the headless-JVM smoke check.
 */
@OptIn(ExperimentalIntentLauncherApi::class)
class SystemIntentsContractTest {

    @Test
    fun systemIntents_exposesObject() {
        // Compile-time check: the expect object resolves from commonMain.
        val obj: SystemIntents = SystemIntents
        assertNotNull(obj)
    }

    @Test
    fun intentResult_okData_carriesUriAndMimeType() {
        val sample = IntentResult.Ok(IntentData(uri = "file:///tmp/x.pdf", mimeType = "application/pdf"))
        assertIs<IntentResult.Ok>(sample)
        assertNotNull(sample.data)
        assertNotNull(sample.data!!.uri)
    }

    @Test
    fun intentResult_failed_unsupportedPlatform_isSentinel() {
        val sample = IntentResult.Failed(IntentError.UnsupportedPlatform)
        assertIs<IntentResult.Failed>(sample)
        assertIs<IntentError.UnsupportedPlatform>(sample.cause)
    }

    @Test
    fun intentResult_cancelled_isSingleton() {
        val a = IntentResult.Cancelled
        val b = IntentResult.Cancelled
        // Object singletons must be referentially identical.
        kotlin.test.assertSame(a, b)
    }
}
