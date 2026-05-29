/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi
import com.mobilebytelabs.kmptoolkit.share.Share

/**
 * Compose-scoped accessor for the [Share] object.
 *
 * Returns the platform's actual [Share] object wrapped in [remember] for stable identity across
 * recompositions. Use the returned handle to launch share operations from any composable scope:
 *
 * ```kotlin
 * @Composable
 * fun ShareScreen(payload: SharePayload) {
 *     val share = rememberShareLauncher()
 *     val scope = rememberCoroutineScope()
 *     Button(onClick = { scope.launch { share.share(payload) } }) { Text("Share") }
 * }
 * ```
 *
 * For drop-in styled UX, use [ShareSheet] (modal bottom sheet) or [ShareButton] (icon button)
 * instead of wiring [rememberShareLauncher] manually.
 *
 * **Platform reach:** 9 Compose-MP-supported targets (Android, JVM, iOS x3, macOS x2, JS, wasmJs)
 * per Phase 0 S1.A verdict. For tvOS/watchOS/Linux/mingw consumers, use core cmp-share's
 * imperative `Share` object directly (Compose Multiplatform doesn't support those targets).
 */
@Composable
@ExperimentalShareApi
public fun rememberShareLauncher(): Share = remember { Share }
