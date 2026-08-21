/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

/**
 * Platform-agnostic Firebase project identifiers — the common superset of the
 * fields GitLive's native `FirebaseOptions` needs across Android / Apple / Web.
 *
 * These are **client identifiers, not secrets** (the same values shipped inside
 * `google-services.json` / `GoogleService-Info.plist`); they are safe to hold in
 * commonMain source. The genuinely-sensitive Measurement-Protocol `apiSecret`
 * lives on [io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig],
 * not here, and must be loaded from a secrets store at runtime.
 *
 * ### Per-platform required fields (Firebase constraints)
 * - **Android**: `applicationId` + `apiKey` (+ `projectId` recommended).
 * - **Apple (iOS/macOS/tvOS)**: `applicationId` + `apiKey` + **`gcmSenderId`**
 *   (the native `FIROptions` constructor requires GCMSenderID). `gaTrackingId`
 *   is ignored on Apple.
 * - **Web (JS)**: `applicationId` + `apiKey` + `projectId`; `authDomain` is
 *   Auth-specific. (GitLive's web options cannot carry a GA4 `measurementId`.)
 *
 * @property applicationId the per-platform Firebase App ID (a.k.a. googleAppID). Required.
 * @property apiKey the Firebase API key. Required.
 * @property projectId the Firebase/GCP project id.
 * @property gcmSenderId messaging sender id; required on Apple.
 * @property storageBucket Cloud Storage bucket.
 * @property databaseUrl Realtime Database URL.
 * @property authDomain web Auth domain (JS only).
 * @property gaTrackingId legacy GA tracking id (dropped on Apple).
 */
public data class FirebaseOptions(
    val applicationId: String,
    val apiKey: String,
    val projectId: String? = null,
    val gcmSenderId: String? = null,
    val storageBucket: String? = null,
    val databaseUrl: String? = null,
    val authDomain: String? = null,
    val gaTrackingId: String? = null,
) {
    init {
        require(applicationId.isNotBlank()) { "FirebaseOptions.applicationId cannot be blank" }
        require(apiKey.isNotBlank()) { "FirebaseOptions.apiKey cannot be blank" }
    }
}
