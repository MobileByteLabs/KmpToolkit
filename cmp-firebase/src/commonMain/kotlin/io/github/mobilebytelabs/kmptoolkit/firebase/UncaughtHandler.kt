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
 * Platform hook that installs a process-global uncaught-exception handler which
 * forwards the fatal throwable to [onUncaught], **chaining to any existing handler**
 * so it is additive (never replaces native Crashlytics / the JVM default).
 *
 * Returns `true` iff a handler was actually installed on this platform:
 * - **JVM / Android** → `true` (java.lang.Thread default handler, chained).
 * - **native (iOS/macOS/tvOS/Linux/mingw), JS, wasmJs** → `false` (no-op): on the
 *   Apple targets native Firebase Crashlytics already owns the crash path, and
 *   Kotlin/Native's one-shot `setUnhandledExceptionHook` would fight it; JS/wasm
 *   have no reliable process-global fatal hook for a library to claim.
 *
 * Internal — consumers call [FirebaseKit.installUncaughtHandler].
 */
internal expect fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit): Boolean
