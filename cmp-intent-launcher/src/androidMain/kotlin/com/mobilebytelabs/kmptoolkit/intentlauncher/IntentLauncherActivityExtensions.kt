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

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Android-only escape hatch for non-Composable callers (per ADR-03).
 *
 * MUST be called from the Activity's `onCreate` BEFORE the Activity reaches
 * `STARTED` state, per the `registerForActivityResult` contract. Calling later
 * throws `IllegalStateException`.
 *
 * Typical use:
 * ```kotlin
 * class MyActivity : ComponentActivity() {
 *     private lateinit var launcher: IntentLauncher
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         launcher = intentLauncher()  // ← MUST be here, before super-class STARTED
 *     }
 * }
 * ```
 *
 * Prefer the Composable [rememberIntentLauncher] for new code.
 */
@ExperimentalIntentLauncherApi
public fun ComponentActivity.intentLauncher(): IntentLauncher {
    var pending: CompletableDeferred<IntentResult>? = null
    val arl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val parsed = IntentLauncher.parseActivityResult(result.resultCode, result.data)
        pending?.complete(parsed)
        pending = null
    }
    return IntentLauncher(
        launcher = arl,
        pending = { pending },
        setPending = { pending = it },
    )
}
