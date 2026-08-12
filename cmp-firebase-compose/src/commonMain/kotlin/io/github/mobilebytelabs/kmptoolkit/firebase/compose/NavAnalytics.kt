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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper

/**
 * Auto-tracks navigation for the whole app: install once next to your `NavHost` and every
 * destination change emits a `screen_view` **and** a screen-flow `screen_transition{from,to}`.
 * No per-screen `TrackScreenView` calls needed.
 *
 * ```kotlin
 * val nav = rememberNavController()
 * nav.trackScreenViews()
 * NavHost(nav, startDestination = "home") { /* ... */ }
 * ```
 */
@Composable
fun NavController.trackScreenViews(analytics: AnalyticsHelper = rememberAnalyticsHelper()) {
    val entry by currentBackStackEntryAsState()
    val previous = remember { arrayOfNulls<String>(1) }
    val dest = entry?.destination?.route ?: return

    LaunchedEffect(dest) {
        val from = previous[0]
        analytics.logScreenView(dest, sourceScreen = from)
        if (from != null && from != dest) {
            analytics.logEvent("screen_transition", "from" to from, "to" to dest)
        }
        previous[0] = dest
    }
}
