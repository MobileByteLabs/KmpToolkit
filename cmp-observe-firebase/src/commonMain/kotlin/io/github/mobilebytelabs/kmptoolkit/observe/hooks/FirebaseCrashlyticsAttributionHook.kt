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
import dev.gitlive.firebase.crashlytics.crashlytics
import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook

/**
 * T0 hook: tags every library init with a Crashlytics custom_key.
 *
 * Key format: `library:{name}` → `{version}` per GOAL.md D15 + AC #11.
 *
 * Any future crash whose stack passes through the library is automatically
 * attributed via the custom_key in the issue metadata, enabling library
 * maintainers to query "show me every crash whose stack passes through cmp-share".
 *
 * Init failures additionally call recordException + set `library:{name}:init_failed=true`.
 *
 * SDK availability: if Firebase Crashlytics isn't initialized yet (rare), the calls
 * are silently swallowed — the hook contract requires no exceptions to propagate.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T4.
 */
public class FirebaseCrashlyticsAttributionHook : LibraryObservationHook {
    override fun onInitStart(meta: CmpMetadata) {
        runCatching {
            Firebase.crashlytics.setCustomKey("library:${meta.name}", meta.version)
        }
    }

    override fun onInitComplete(meta: CmpMetadata) {
        // No-op — onInitStart already set the attribution key.
    }

    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {
        runCatching {
            Firebase.crashlytics.recordException(throwable)
            Firebase.crashlytics.setCustomKey("library:${meta.name}:init_failed", "true")
        }
    }

    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
        // No-op — lifecycle events are not T0 surface.
    }

    override fun onClose(meta: CmpMetadata) {
        // No-op — Crashlytics keys persist for the session by design.
    }
}
