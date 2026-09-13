/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe

/*
 * Helpers every `cmp-*` module uses to report itself, so 22 modules do not each hand-roll the
 * notify shape.
 *
 * ## Why these exist
 * Before this, exactly ONE module of the 23 that generate `CmpMetadata` actually reported anything —
 * `cmp-network-monitor`, in one Android file. Two things kept it there:
 *
 *  1. `cmp-observe` shipped 15 targets while most modules ship 21, so depending on it from
 *     `commonMain` would have capped them. Fixed 2026-09-13: it now ships all 21.
 *  2. The correct init shape — notify, run, notify-complete, notify-failure on throw, rethrow — is
 *     eight lines of ceremony per call site, and easy to get subtly wrong (swallowing the throwable,
 *     or reporting complete after a failure).
 *
 * [observeInit] collapses that to one call. [LibraryObservation] already isolates hook exceptions, so
 * nothing here can break a caller.
 */

/**
 * Run [block] as this library's initialisation, reporting it to every registered hook.
 *
 * Emits `init` before, `initComplete` after, or `initFailure` if [block] throws — and **rethrows**,
 * because observability must not change control flow. Returns whatever [block] returns.
 *
 * ```kotlin
 * override fun onCreate(): Boolean = observeInit(cmpMetadata()) {
 *     appContext = context?.applicationContext
 *     true
 * }
 * ```
 */
public inline fun <T> observeInit(meta: CmpMetadata, block: () -> T): T {
    LibraryObservation.notifyInit(meta)
    return try {
        val result = block()
        LibraryObservation.notifyInitComplete(meta)
        result
    } catch (t: Throwable) {
        LibraryObservation.notifyInitFailure(meta, t)
        throw t
    }
}

/**
 * Report a named thing this library just did — a share dispatched, a URL refused, a review requested.
 *
 * This is the signal a consumer's hook is usually after, and it had **zero** call sites anywhere in
 * the toolkit before 2026-09-13: the observability surface could only ever say "a library started".
 *
 * Keep [event] a stable snake_case verb phrase; it is a key a consumer will match on, not prose.
 * [payload] carries the detail worth recording — never PII, since hooks forward to Crashlytics and
 * Analytics.
 *
 * ```kotlin
 * observeLifecycle(cmpMetadata(), "share_dispatched", mapOf("kind" to "text"))
 * ```
 */
public fun observeLifecycle(meta: CmpMetadata, event: String, payload: Map<String, Any?> = emptyMap()) {
    LibraryObservation.notifyLifecycle(meta, event, payload)
}

/**
 * Report that this library released its resources — a monitor stopped, an observer unregistered.
 *
 * Pairs with [observeInit]; a hook counting starts against closes is how a consumer finds a leak.
 */
public fun observeClose(meta: CmpMetadata) {
    LibraryObservation.notifyClose(meta)
}
