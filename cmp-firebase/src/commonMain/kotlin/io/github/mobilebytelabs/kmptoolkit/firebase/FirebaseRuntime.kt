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
