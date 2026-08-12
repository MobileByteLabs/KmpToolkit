/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.compose

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.NoOpAnalyticsHelper

/**
 * Provides the app's [AnalyticsHelper] to the composition. Set it once near the root:
 * ```kotlin
 * CompositionLocalProvider(LocalAnalyticsHelper provides analytics) { App() }
 * ```
 * Defaults to [NoOpAnalyticsHelper] so previews/tests never emit.
 */
val LocalAnalyticsHelper = staticCompositionLocalOf<AnalyticsHelper> { NoOpAnalyticsHelper }

/** The [AnalyticsHelper] from the current composition. */
@Composable
fun rememberAnalyticsHelper(): AnalyticsHelper = LocalAnalyticsHelper.current

/**
 * Logs a `screen_view` for [screenName] when this composable enters the composition.
 * ```kotlin
 * @Composable fun ProfileScreen() { TrackScreenView("profile"); /* ... */ }
 * ```
 */
@Composable
fun TrackScreenView(screenName: String, sourceScreen: String? = null) {
    val analytics = rememberAnalyticsHelper()
    LaunchedEffect(screenName) { analytics.logScreenView(screenName, sourceScreen) }
}

/**
 * Logs component enter/exit lifecycle events for [componentName] — useful for measuring
 * time-on-component or drop-off.
 */
@Composable
fun TrackComposableLifecycle(componentName: String) {
    val analytics = rememberAnalyticsHelper()
    DisposableEffect(componentName) {
        analytics.logEvent("component_enter", "component" to componentName)
        onDispose { analytics.logEvent("component_exit", "component" to componentName) }
    }
}

/**
 * Adds click tracking to any composable — logs a `button_click` with [label] (and [screen])
 * then invokes [onClick].
 * ```kotlin
 * Text("Save", Modifier.trackClick("save", analytics, "settings") { viewModel.save() })
 * ```
 */
fun Modifier.trackClick(
    label: String,
    analytics: AnalyticsHelper,
    screen: String? = null,
    onClick: () -> Unit = {},
): Modifier = composed {
    clickable {
        analytics.logButtonClick(label, screen)
        onClick()
    }
}
