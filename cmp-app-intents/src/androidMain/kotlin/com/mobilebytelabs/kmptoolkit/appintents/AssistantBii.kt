/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * 2026 Android Built-in Intent (BII) mapping helper.
 *
 * Resolves an `AppIntentDef` to its `actions.intent.{NAME}` BII identifier, used by the
 * v0.4 candidate `generateShortcutsXml` Gradle task to emit `res/xml/cmp_app_intents_shortcuts.xml`
 * capability blocks consumer-side.
 *
 * Per SPIKE_FINDINGS_V0_3.md S0.B PIVOT verdict: BIIs (Built-in Intents in shortcuts.xml) are
 * the 2026-current canonical Android Assistant integration path — NOT the "Assistant Schema +
 * App Engagement APIs" approach v0.2 SPIKE_FINDINGS speculated. See developer.android.com/develop/devices/assistant/get-started.
 *
 * ADR-09 row: cmp-app-intents androidMain Gradle `generateShortcutsXml` task itself is deferred
 * to v0.4 Phase 4 polish — this `AssistantBii` helper ships now so consumers can call
 * `AssistantBii.resolveBii(def)` directly OR the v0.4 task can consume it.
 */
@OptIn(ExperimentalAppIntentsApi::class)
public object AssistantBii {

    /** Resolve the BII identifier for an intent definition; defers to [defaultBiiForIntent] when caller hasn't set one. */
    public fun resolveBii(def: AppIntentDef): String = def.bii ?: defaultBiiForIntent(def)

    private fun defaultBiiForIntent(def: AppIntentDef): String = when {
        def.searchable -> "actions.intent.GET_THING"
        else -> "actions.intent.OPEN_APP_FEATURE"
    }
}
