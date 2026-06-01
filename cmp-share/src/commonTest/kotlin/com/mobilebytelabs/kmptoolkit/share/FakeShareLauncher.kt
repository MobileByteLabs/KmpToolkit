/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * Captured share() call (payload + options snapshot).
 */
@ExperimentalShareApi
public data class ShareAttempt(val payload: SharePayload, val options: ShareOptions)

/**
 * Test double for the [Share] object. Mirrors the production API surface but
 * does not extend the `expect object Share` (Kotlin Multiplatform `expect`
 * objects can only have one `actual` per target — Fakes live alongside, not
 * inside, the expect hierarchy).
 *
 * **Usage (commonTest):**
 * ```kotlin
 * val fake = FakeShareLauncher().apply {
 *     scriptResult(ShareResult.Completed)
 *     scriptResult(ShareResult.Cancelled)
 * }
 * val r1 = fake.share(SharePayload.Text("hi"), ShareOptions())
 * // assert r1 is Completed and fake.shareHistory[0].payload is Text("hi")
 * ```
 *
 * **Inspection surface:**
 * - [shareHistory] — every call to [share], in order, with (payload, options) snapshot
 *
 * **Scripting surface:**
 * - [scriptResult] — enqueue a typed [ShareResult] for the next share() call
 * - [scriptCancellation] — shortcut for [ShareResult.Cancelled]
 * - [scriptError] — shortcut for [ShareResult.Failed(cause)]
 *
 * When the script queue is empty, returns `Failed(UnsupportedPlatform)` — matches
 * the production fallback on platforms without a share UI (tvOS, etc.).
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 03.
 */
@ExperimentalShareApi
public class FakeShareLauncher {
    public val shareHistory: MutableList<ShareAttempt> = mutableListOf()
    private val scriptQueue: ArrayDeque<ShareResult> = ArrayDeque()

    public fun scriptResult(result: ShareResult) {
        scriptQueue.addLast(result)
    }

    public fun scriptCancellation() {
        scriptQueue.addLast(ShareResult.Cancelled)
    }

    public fun scriptError(cause: ShareError) {
        scriptQueue.addLast(ShareResult.Failed(cause))
    }

    public fun reset() {
        shareHistory.clear()
        scriptQueue.clear()
    }

    public suspend fun share(payload: SharePayload, options: ShareOptions = ShareOptions()): ShareResult {
        shareHistory.add(ShareAttempt(payload, options))
        return scriptQueue.removeFirstOrNull()
            ?: ShareResult.Failed(ShareError.UnsupportedPlatform)
    }
}
