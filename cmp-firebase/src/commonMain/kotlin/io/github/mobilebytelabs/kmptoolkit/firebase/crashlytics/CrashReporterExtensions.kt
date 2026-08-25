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
fun CrashReporter.asCoroutineExceptionHandler(): CoroutineExceptionHandler = asCoroutineExceptionHandler(fatal = false)

/**
 * As [asCoroutineExceptionHandler] but records with the given [fatal] flag — pass
 * `true` for a top-level/app scope so the GA4 `app_crash` `fatal` dimension reflects
 * reality (the bridge otherwise only ever sees `fatal=false`).
 *
 * (Separate overload rather than a defaulted param so the original zero-arg
 * signature stays binary-compatible for already-published consumers.)
 */
fun CrashReporter.asCoroutineExceptionHandler(fatal: Boolean): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, throwable -> recordException(throwable, fatal = fatal) }

/**
 * Run [block], recording any thrown exception to this reporter, then rethrow.
 * Handy for wrapping a risky call site without changing its control flow.
 */
inline fun <T> CrashReporter.recording(block: () -> T): T = recording(fatal = false, block = block)

/**
 * As [recording] but records with the given [fatal] flag. Pass `true` at a
 * top-level/app scope so the GA4 `app_crash` `fatal` dimension reflects reality.
 *
 * (Separate overload rather than a defaulted param so the original single-lambda
 * signature stays binary-compatible for already-published consumers.)
 */
inline fun <T> CrashReporter.recording(fatal: Boolean, block: () -> T): T = try {
    block()
} catch (t: Throwable) {
    recordException(t, fatal = fatal)
    throw t
}
