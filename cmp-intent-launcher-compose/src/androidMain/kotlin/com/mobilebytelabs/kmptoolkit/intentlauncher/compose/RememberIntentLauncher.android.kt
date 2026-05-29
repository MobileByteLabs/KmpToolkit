/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentLauncher
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import kotlinx.coroutines.CompletableDeferred

@ExperimentalIntentLauncherApi
@Composable
public actual fun rememberIntentLauncher(): IntentLauncher {
    var pending: CompletableDeferred<IntentResult>? = null
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val parsed = IntentLauncher.parseActivityResult(result.resultCode, result.data)
        pending?.complete(parsed)
        pending = null
    }
    return remember(launcher) {
        IntentLauncher(
            launcher = launcher,
            pending = { pending },
            setPending = { pending = it },
        )
    }
}
