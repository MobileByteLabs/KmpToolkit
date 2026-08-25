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
 * No-op: native Firebase Crashlytics (Apple) already owns the crash path and
 * Kotlin/Native's one-shot exception hook would fight it; Linux/mingw fatal
 * auto-capture is a documented non-goal. Route your handler to
 * `FirebaseKit.crashReporter.recordException(t, fatal = true)` if you need it.
 */
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit): Boolean = false
