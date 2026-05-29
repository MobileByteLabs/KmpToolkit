/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * JVM Desktop `AppIntents` — **ADR-09 #9 WONTFIX (architectural)** per v0.4 docs refresh.
 *
 * Desktop has no canonical OS-level intent / App Actions abstraction. Windows Cortana,
 * GNOME GLib Actions, macOS App Shortcuts on Desktop are all platform-specific JNI work
 * without a unified API. `register()` is intentionally no-op; `invokeForTesting` works for
 * dev/test. Consumers wanting per-OS Desktop integration must use the platform actual
 * modules (cmp-app-intents/src/{linux,mingw,macos}Main) directly via the appropriate K/N target.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        // No OS surface to register against; consumer can use invokeForTesting.
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
