/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.analytics.mp

import com.russhwolf.settings.Settings
import kotlin.random.Random

private const val CLIENT_ID_KEY = "io.github.mobilebytelabs.kmptoolkit.analytics.mp.client_id"

/**
 * Loads (or generates and persists) a stable per-device client_id for
 * Firebase Measurement Protocol.
 *
 * The MP `client_id` is a stable per-device identifier used to attribute
 * events to a single user-session lineage. Persistence is REQUIRED — without
 * it, every event looks like a brand-new device, breaking unique-user counts.
 *
 * Storage uses [com.russhwolf.settings.Settings] which provides cross-platform
 * key-value persistence:
 * - Android: SharedPreferences
 * - JVM: java.util.prefs.Preferences
 * - iOS / macOS / tvOS / watchOS: NSUserDefaults
 * - JS: localStorage
 * - Linux native / mingwX64: in-memory only (TODO: file-backed in 1.4+)
 * - wasmJs: localStorage
 * - wasmWasi: in-memory only (no persistent KV today)
 *
 * On platforms without persistent storage, the client_id is regenerated each
 * cold start. Events still flow; the unique-user count just over-reports.
 * Document this limitation in your app's privacy policy if material.
 */
internal fun loadOrCreateClientId(settings: Settings): String {
    val existing = settings.getStringOrNull(CLIENT_ID_KEY)
    if (existing != null) return existing
    val fresh = generateClientId()
    settings.putString(CLIENT_ID_KEY, fresh)
    return fresh
}

/**
 * Generates a fresh client_id matching GA4's expected format
 * (`{32-bit-int}.{Unix-timestamp-seconds}`).
 *
 * GA4 accepts UUIDs too, but the legacy format is what GitLive emits and
 * matches what Firebase native SDK uses, so events from MP and GitLive
 * have identical client_id shape in BigQuery.
 */
private fun generateClientId(): String {
    val rand = Random.nextInt().toLong() and 0xFFFFFFFFL
    val ts = currentEpochSeconds()
    return "$rand.$ts"
}

/**
 * Current Unix-epoch seconds. Pure expect for cross-platform reliability —
 * `kotlinx.datetime.Clock.System.now()` is the simplest portable answer and
 * is already a transitive dep via commonMain.
 */
private fun currentEpochSeconds(): Long = kotlinx.datetime.Clock.System.now().epochSeconds
