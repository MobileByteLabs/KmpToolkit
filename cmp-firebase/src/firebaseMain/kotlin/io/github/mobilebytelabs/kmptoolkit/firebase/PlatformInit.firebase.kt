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

import co.touchlab.kermit.Logger
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
 * GitLive-native tier (android / ios / macos / tvos / js / wasmjs): configure the default
 * FirebaseApp programmatically. On Android the required `Context` is read from
 * [FirebaseNativeContext] (captured by `FirebaseInitProvider`); elsewhere it is
 * `null` and GitLive ignores it.
 */
internal actual fun platformInitializeFirebase(options: FirebaseOptions?) {
    val opts = options ?: return
    // runCatching, because FirebaseKit.initialize's contract is "it never throws (analytics must
    // not break the app)" and this call CAN throw. On Android, GitLive requires a non-null
    // Context; FirebaseNativeContext.value is populated by FirebaseInitProvider on a real device
    // but is null anywhere the ContentProvider has not run — a JVM host test, or an app whose
    // manifest merge dropped the provider — and the cast fails with
    // "null cannot be cast to non-null type android.content.Context".
    //
    // Degrading with a WARN keeps that a NoOp-analytics situation instead of a startup crash,
    // matching the non-Firebase tier. Note this cannot catch Apple's ObjC NSException for
    // malformed options — see FirebaseKit.initialize's KDoc.
    runCatching {
        Firebase.initialize(context = FirebaseNativeContext.value, options = opts.toGitLive())
    }.onFailure { cause ->
        Logger.w(TAG, cause) {
            "Native Firebase init failed — analytics degrades to NoOp. On Android this usually " +
                "means FirebaseInitProvider never ran (manifest merge dropped it, or this is a " +
                "JVM host test); elsewhere it means the supplied options were rejected."
        }
    }
}

private const val TAG = "FirebaseKit"
