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

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.FirebaseOptions as GitLiveFirebaseOptions

/**
 * Map the common [FirebaseOptions] superset to GitLive's native `FirebaseOptions`.
 *
 * Per-platform notes: Apple's native `FIROptions` constructor requires
 * `gcmSenderId` (supply it in [FirebaseOptions.gcmSenderId]); `gaTrackingId` is
 * dropped on Apple by GitLive. Web reads `authDomain`.
 */
internal fun FirebaseOptions.toGitLive(): GitLiveFirebaseOptions = GitLiveFirebaseOptions(
    applicationId = applicationId,
    apiKey = apiKey,
    databaseUrl = databaseUrl,
    gaTrackingId = gaTrackingId,
    storageBucket = storageBucket,
    projectId = projectId,
    gcmSenderId = gcmSenderId,
    authDomain = authDomain,
)

/**
 * GitLive-native tier (android / ios / macos / tvos / js): configure the default
 * FirebaseApp programmatically. On Android the required `Context` is read from
 * [FirebaseNativeContext] (captured by `FirebaseInitProvider`); elsewhere it is
 * `null` and GitLive ignores it.
 */
internal actual fun platformInitializeFirebase(options: FirebaseOptions?) {
    val opts = options ?: return
    Firebase.initialize(context = FirebaseNativeContext.value, options = opts.toGitLive())
}
