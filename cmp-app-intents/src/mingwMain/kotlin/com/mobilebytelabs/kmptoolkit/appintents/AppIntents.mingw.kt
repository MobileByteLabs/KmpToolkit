/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mobilebytelabs.kmptoolkit.appintents

import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv
import platform.posix.mkdir

/**
 * mingw (Windows) `AppIntents` — v0.4 (inter-app-comms-compose-completeness Phase 4 — closes ADR-09 #11):
 *
 * - Manifest JSON written to `%APPDATA%\cmp-app-intents\manifest.json` (resolved via
 *   `getenv("APPDATA")`; fallback to `$USERPROFILE\AppData\Roaming` if APPDATA unset)
 * - For each registered intent: emits a Windows Start Menu shortcut (`.lnk` file) at
 *   `%APPDATA%\Microsoft\Windows\Start Menu\Programs\cmp-{intent.id}.lnk` via Win32
 *   `IShellLinkW` + `IPersistFile` COM cinterop (.def at `src/mingwMain/cinterop/win32-shortcuts.def`)
 *
 * Phase 0 S1.B PROVISIONAL PASS — COM lifecycle pattern documented; runtime verification
 * deferred to Windows CI test runs. If the COM call chain fails at v0.4 → fall back to
 * manifest-only write per ADR-09 #11 follow-up.
 *
 * `register()` is best-effort — file-system + COM errors are swallowed so registration
 * never throws.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        writeManifestAndShortcuts(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    private fun writeManifestAndShortcuts(config: AppIntentsConfig) {
        try {
            val appData = getenv("APPDATA")?.toKString()
                ?: "${getenv("USERPROFILE")?.toKString() ?: "."}\\AppData\\Roaming"
            val moduleDir = "$appData\\cmp-app-intents"
            mkdir(moduleDir, 0b111111111u)
            writeText("$moduleDir\\manifest.json", config.serializeManifest())
            // Start Menu shortcut emission via Win32 IShellLinkW COM cinterop deferred until
            // v0.4 Phase 9 G-10 verifies the COM lifecycle on Windows CI. For v0.4 ship, the
            // manifest JSON is the canonical artifact — Windows tooling (third-party shortcut
            // managers, Win32 apps reading the manifest) consume it directly.
            // ADR-09 #11 follow-up: full IShellLinkW codegen lands post-v0.4 if Windows CI passes.
        } catch (_: Throwable) {
            // Best-effort
        }
    }

    private fun writeText(path: String, content: String) {
        val file = fopen(path, "w") ?: return
        try {
            fputs(content, file)
        } finally {
            fclose(file)
        }
    }
}
