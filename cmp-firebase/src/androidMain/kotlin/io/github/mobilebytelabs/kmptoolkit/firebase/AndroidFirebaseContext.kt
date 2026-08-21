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

import android.content.Context

/**
 * Android application-Context holder for programmatic Firebase init.
 *
 * `FirebaseInitProvider` (a `ContentProvider`, runs before `Application.onCreate`)
 * captures the application `Context` here, so `FirebaseKit.initialize(config)`
 * stays a pure commonMain call — the consumer never passes a Context.
 *
 * If the provider is removed from the merged manifest (`tools:node="remove"`),
 * call [setApplicationContext] manually before `FirebaseKit.initialize(config)`.
 */
public object AndroidFirebaseContext {

    /** The captured application [Context], or `null` if not yet set. */
    val app: Context?
        get() = FirebaseNativeContext.value as? Context

    /** Manually supply the application [Context] (provider-removed fallback). */
    public fun setApplicationContext(context: Context) {
        FirebaseNativeContext.value = context.applicationContext
    }
}
