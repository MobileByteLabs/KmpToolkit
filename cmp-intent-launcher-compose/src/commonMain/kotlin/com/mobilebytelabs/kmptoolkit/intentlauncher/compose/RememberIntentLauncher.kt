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

import androidx.compose.runtime.Composable
import com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentLauncher

/**
 * Compose-scoped intent launcher.
 *
 * Per-platform binding (see core module SPEC + ADR-03):
 * - **Android**: wraps `androidx.activity.compose.rememberLauncherForActivityResult`;
 *   lifecycle-bound to the enclosing Composable.
 * - **iOS**: returns a fresh [IntentLauncher] remembered across recompositions.
 * - **macOS / JVM Desktop / JS / wasmJs**: same — `remember { IntentLauncher() }`.
 *
 * **Migration from v0.2 (inter-app-comms-real-native-impls Phase 1 — 2026-05-28)**: this
 * `rememberIntentLauncher()` moved here from the cmp-intent-launcher core module so the core
 * can reach 19 KMP targets without Compose Compiler. Consumers add `cmp-intent-launcher-compose`
 * dep alongside `cmp-intent-launcher`; same import path under `.compose` subpackage.
 *
 * **HARD CONSTRAINT (Phase 0 TS6)** for JS/wasmJs: `.launch()` MUST be called from within a
 * user-gesture handler call stack. Returns `IntentResult.Failed(IntentError.UserGestureMissing)`
 * on browser block.
 */
@ExperimentalIntentLauncherApi
@Composable
public expect fun rememberIntentLauncher(): IntentLauncher
