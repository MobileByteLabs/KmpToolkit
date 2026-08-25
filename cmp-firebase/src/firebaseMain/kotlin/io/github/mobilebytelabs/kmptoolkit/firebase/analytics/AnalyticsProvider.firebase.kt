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

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.FirebaseAnalyticsHelper

/**
 * Firebase-tier actual: Android · JVM · iOS (×3) · macOS (×2) · tvOS (×3) · JS.
 *
 * Returns a [FirebaseAnalyticsHelper] backed by GitLive's `Firebase.analytics`.
 *
 * Setup prerequisites:
 * - **Android**: `google-services.json` + `com.google.gms.google-services` plugin applied
 * - **iOS / macOS / tvOS**: `GoogleService-Info.plist` + `FirebaseApp.configure()` in AppDelegate
 *   (or `@main App.init`). Pod dependencies bundled by GitLive.
 * - **JS**: Firebase config object passed during app init
 * - **JVM**: limited support; falls back to GitLive's stub if not configured
 */
internal actual fun createPlatformAnalyticsHelper(): AnalyticsHelper = FirebaseAnalyticsHelper(Firebase.analytics)
