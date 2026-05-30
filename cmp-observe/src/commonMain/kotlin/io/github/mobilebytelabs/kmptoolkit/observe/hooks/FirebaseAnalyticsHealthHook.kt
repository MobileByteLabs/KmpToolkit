/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe.hooks

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook

/**
 * T1 hook: emits `lib_init_success` / `lib_init_failure` events to Firebase Analytics
 * with `library_name` + `library_version` params (+ `failure_reason` + truncated
 * `failure_message` on failure).
 *
 * Powers /lib-observe's "init failures/24h" column per AC #25.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T5.
 */
public class FirebaseAnalyticsHealthHook : LibraryObservationHook {
    override fun onInitStart(meta: CmpMetadata) {
        // No-op — emit only on success/failure to keep event volume low.
    }

    override fun onInitComplete(meta: CmpMetadata) {
        runCatching {
            Firebase.analytics.logEvent("lib_init_success") {
                param("library_name", meta.name)
                param("library_version", meta.version)
            }
        }
    }

    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {
        runCatching {
            Firebase.analytics.logEvent("lib_init_failure") {
                param("library_name", meta.name)
                param("library_version", meta.version)
                param("failure_reason", throwable::class.simpleName ?: "unknown")
                param("failure_message", throwable.message?.take(100) ?: "unknown")
            }
        }
    }

    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
        // No-op — lifecycle events go to T2 (SupabaseEventsHook), not Firebase Analytics.
    }

    override fun onClose(meta: CmpMetadata) {
        // No-op.
    }
}
