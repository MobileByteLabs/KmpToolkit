/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

/**
 * Chains a JVM default uncaught-exception handler that mirrors the fatal crash to
 * [onUncaught] (→ GA4 `app_crash`) BEFORE delegating to whatever handler was already
 * installed (e.g. the platform/Crashlytics default), so native capture is preserved.
 */
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit): Boolean {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { onUncaught(throwable) }
        previous?.uncaughtException(thread, throwable)
    }
    return true
}
