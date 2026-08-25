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

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import kotlin.concurrent.Volatile

/**
 * Internal holder for the active [FirebaseConfig] set by [FirebaseKit.initialize].
 *
 * The Measurement-Protocol analytics factory (`provideAnalyticsHelper()` on the
 * non-GitLive tier) reads [config] to auto-wire the MP helper from the same
 * single config the consumer passed at init — no separate per-platform DI.
 */
internal object FirebaseRuntime {
    @Volatile
    var config: FirebaseConfig? = null

    /**
     * Process-wide memoized [AnalyticsHelper] returned by `provideAnalyticsHelper()`.
     * A SINGLE instance is shared by the app's DI and the crash→GA4 bridge so consent
     * ([AnalyticsHelper.setCollectionEnabled]) and the MP `client_id` are authoritative
     * across both — without this the bridge built its own helper and a user opt-out on
     * the DI helper never reached crash mirroring (consent leak on the MP tier).
     */
    @Volatile
    var analyticsHelper: AnalyticsHelper? = null
}

/**
 * Platform-init Context carrier, typed [Any] so it lives in commonMain.
 *
 * On Android, `FirebaseInitProvider` (a ContentProvider that runs before
 * `Application.onCreate`) writes the application `Context` here; GitLive's
 * `Firebase.initialize(context, options)` requires it on Android. On every other
 * platform it stays `null` (GitLive ignores the argument). This keeps the
 * consumer's `FirebaseKit.initialize(config)` call 100% commonMain.
 */
internal object FirebaseNativeContext {
    @Volatile
    var value: Any? = null
}
