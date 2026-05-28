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
import platform.posix.getenv
import platform.posix.mkdir

/**
 * Linux `AppIntents` — v0.4 (inter-app-comms-compose-completeness Phase 4 — closes ADR-09 #11):
 *
 * - Manifest JSON written to `$XDG_DATA_HOME/cmp-app-intents/manifest.json` (fallback to
 *   `~/.local/share/cmp-app-intents/manifest.json` when XDG_DATA_HOME unset, per XDG Base Dir spec)
 * - For each registered intent: emits a `.desktop` action handler at
 *   `$XDG_DATA_HOME/applications/cmp-{intent.id}.desktop` so xdg-desktop-portal and GNOME
 *   Shell can surface the intents as application actions.
 *
 * `register()` is best-effort — file-system errors are swallowed so registration never throws.
 * `invokeForTesting` works for dev/test as a fallback (manifest reading is consumer/system
 * tooling's responsibility on Linux).
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        writeManifestAndDesktopFiles(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    private fun writeManifestAndDesktopFiles(config: AppIntentsConfig) {
        try {
            val xdgDataHome = getenv("XDG_DATA_HOME")?.toKString()
                ?: "${getenv("HOME")?.toKString() ?: "."}/.local/share"
            val moduleDir = "$xdgDataHome/cmp-app-intents"
            val appsDir = "$xdgDataHome/applications"
            // mkdir -p (ignore errors — directory may already exist)
            mkdir(moduleDir, 0b111111111u)  // 0777 octal
            mkdir(appsDir, 0b111111111u)
            // Write manifest JSON
            writeText("$moduleDir/manifest.json", config.serializeManifest())
            // Emit per-intent .desktop action handlers
            config.intents.forEach { intent ->
                val desktopPath = "$appsDir/cmp-${intent.id}.desktop"
                writeText(desktopPath, buildDesktopFile(intent))
            }
        } catch (_: Throwable) {
            // Best-effort
        }
    }

    private fun buildDesktopFile(intent: AppIntentDef): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=${intent.title}")
        appendLine("Comment=${intent.description}")
        appendLine("Exec=xdg-open cmp-app-intent://${intent.id}")
        appendLine("NoDisplay=true")  // hide from main menu; surface only via action handler
        appendLine("Actions=${intent.id}")
        appendLine()
        appendLine("[Desktop Action ${intent.id}]")
        appendLine("Name=${intent.title}")
        appendLine("Exec=xdg-open cmp-app-intent://${intent.id}")
    }

    private fun writeText(path: String, content: String) {
        val file = platform.posix.fopen(path, "w") ?: return
        try {
            platform.posix.fputs(content, file)
        } finally {
            platform.posix.fclose(file)
        }
    }
}
