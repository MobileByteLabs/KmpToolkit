/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

/**
 * Snapshot of an [IntentBuilder] state captured at the moment of a launch() call.
 * Tests can inspect this to assert that the consumer code built the expected intent.
 */
@ExperimentalIntentLauncherApi
public data class LaunchAttempt(
    val action: String?,
    val data: String?,
    val mimeType: String?,
    val extras: Map<String, Any?>,
    val resultContract: ResultContract<*>?,
)

/**
 * Test double for [IntentLauncher] + [SystemIntents]. Mirrors the production API
 * surface but does not extend the `expect` types (Kotlin Multiplatform `expect`
 * classes can only have one `actual` per target — Fakes live alongside, not
 * inside, the expect hierarchy).
 *
 * **Usage (commonTest):**
 * ```kotlin
 * val fake = FakeIntentLauncher().apply {
 *     scriptResult(IntentResult.Ok(IntentData("file:///pick.png", "image/png")))
 *     scriptCancellation()
 * }
 * val r1 = fake.launch { resultContract = ResultContracts.PickImage }
 * // assert r1 is Ok and fake.launchHistory[0].resultContract == PickImage
 * ```
 *
 * **Inspection surface:**
 * - [launchHistory] — every call to [launch], in order, with builder snapshot
 * - [systemIntentsCallHistory] — every call to [systemIntents] methods, in order
 *
 * **Scripting surface:**
 * - [scriptResult] — enqueue a typed [IntentResult] for the next launch/systemIntents call
 * - [scriptCancellation] — shortcut for [IntentResult.Cancelled]
 * - [scriptError] — shortcut for [IntentResult.Failed(cause)]
 *
 * When the script queue is empty, returns `Failed(UnsupportedPlatform)` — matches
 * the production fallback behavior on platforms without an OS primitive.
 *
 * Authored 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 03.
 */
@ExperimentalIntentLauncherApi
public class FakeIntentLauncher {
    public val launchHistory: MutableList<LaunchAttempt> = mutableListOf()
    public val systemIntentsCallHistory: MutableList<String> = mutableListOf()
    private val scriptQueue: ArrayDeque<IntentResult> = ArrayDeque()

    public fun scriptResult(result: IntentResult) {
        scriptQueue.addLast(result)
    }

    public fun scriptCancellation() {
        scriptQueue.addLast(IntentResult.Cancelled)
    }

    public fun scriptError(cause: IntentError) {
        scriptQueue.addLast(IntentResult.Failed(cause))
    }

    public fun reset() {
        launchHistory.clear()
        systemIntentsCallHistory.clear()
        scriptQueue.clear()
    }

    public suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        // IntentBuilder's constructor is `internal` — accessible from commonTest
        // because Kotlin Multiplatform treats commonTest as same-module for visibility.
        val builder = IntentBuilder().apply(block)
        launchHistory.add(
            LaunchAttempt(
                action = builder.action,
                data = builder.data,
                mimeType = builder.type,
                extras = builder.extras.toMap(),
                resultContract = builder.resultContract,
            ),
        )
        return scriptQueue.removeFirstOrNull()
            ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
    }

    public val systemIntents: FakeSystemIntents = FakeSystemIntents()

    public inner class FakeSystemIntents {
        public suspend fun openAppSettings(): IntentResult {
            systemIntentsCallHistory.add("openAppSettings")
            return scriptQueue.removeFirstOrNull()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }

        public suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
            systemIntentsCallHistory.add("createDocument:$mimeType:$suggestedName")
            return scriptQueue.removeFirstOrNull()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }
}
