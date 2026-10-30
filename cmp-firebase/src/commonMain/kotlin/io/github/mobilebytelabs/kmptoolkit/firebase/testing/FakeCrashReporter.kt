/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.testing

import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.CrashReport
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.CrashReporter

/** One recorded exception. */
public data class RecordedCrash(
    public val throwable: Throwable,
    public val fatal: Boolean,
    public val extraKeys: Map<String, String>,
)

/**
 * Recording [CrashReporter] for tests — shipped in the main artifact.
 *
 * Error-handling paths are the ones most worth testing and the hardest to observe: the assertion
 * you want is "this failure was *reported*, and reported as non-fatal", which a no-op reporter
 * cannot tell you.
 *
 * ```kotlin
 * val crashes = FakeCrashReporter()
 * SyncWorker(crashes).run()          // network throws internally
 *
 * val recorded = crashes.recorded.single()
 * assertFalse(recorded.fatal, "a retryable sync failure must not be reported as fatal")
 * assertEquals("sync", recorded.extraKeys["stage"])
 * ```
 *
 * Breadcrumbs, custom keys and the user id are recorded separately, so a test can also assert that
 * **no PII** was attached:
 *
 * ```kotlin
 * assertFalse(crashes.customKeys.values.any { it.contains("@") })
 * ```
 */
public class FakeCrashReporter : CrashReporter {

    /** Every exception recorded, in order. */
    public val recorded: MutableList<RecordedCrash> = mutableListOf()

    /** Every breadcrumb, in order. */
    public val breadcrumbs: MutableList<String> = mutableListOf()

    /** Sticky custom keys, last write wins — matching the real reporter's semantics. */
    public val customKeys: MutableMap<String, String> = mutableMapOf()

    /** The user id last set, or `null` if never set. */
    public var userId: String? = null
        private set

    /** Whether [install] was called. */
    public var installed: Boolean = false
        private set

    /** Every [setCollectionEnabled] call, in order — assert a consent change reached the reporter. */
    public val collectionChanges: MutableList<Boolean> = mutableListOf()

    override fun recordException(throwable: Throwable, fatal: Boolean, extraKeys: Map<String, String>) {
        recorded += RecordedCrash(throwable, fatal, extraKeys)
    }

    override fun log(message: String) {
        breadcrumbs += message
    }

    override fun setCustomKey(key: String, value: String) {
        customKeys[key] = value
    }

    override fun setUserId(userId: String) {
        this.userId = userId
    }

    override fun install() {
        installed = true
    }

    /**
     * Always `null`: [CrashReport] is built by the real reporter from platform state this fake has
     * no access to. Assert against [recorded] instead, which carries everything a caller supplied.
     */
    override fun setCollectionEnabled(enabled: Boolean) {
        collectionChanges += enabled
    }

    override val lastReport: CrashReport? = null

    /** Forget everything recorded. */
    public fun reset() {
        recorded.clear()
        breadcrumbs.clear()
        customKeys.clear()
        userId = null
        installed = false
        collectionChanges.clear()
    }
}
