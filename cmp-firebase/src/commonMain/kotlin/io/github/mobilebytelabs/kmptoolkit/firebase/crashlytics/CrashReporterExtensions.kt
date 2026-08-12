/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * A [CoroutineExceptionHandler] that records uncaught coroutine failures to this
 * reporter. Wire it into your app/UI scope for automatic-ish crash capture on
 * ALL platforms — especially the fallback tier where native Crashlytics can't
 * auto-install a signal handler.
 *
 * ```kotlin
 * val scope = CoroutineScope(SupervisorJob() + FirebaseKit.crashReporter.asCoroutineExceptionHandler())
 * ```
 */
fun CrashReporter.asCoroutineExceptionHandler(): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    recordException(throwable, fatal = false)
}

/**
 * Run [block], recording any thrown exception to this reporter, then rethrow.
 * Handy for wrapping a risky call site without changing its control flow.
 */
inline fun <T> CrashReporter.recording(block: () -> T): T = try {
    block()
} catch (t: Throwable) {
    recordException(t, fatal = false)
    throw t
}
