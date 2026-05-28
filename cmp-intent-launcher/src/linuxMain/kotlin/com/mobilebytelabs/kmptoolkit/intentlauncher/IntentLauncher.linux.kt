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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system

/**
 * Linux `IntentLauncher` — v0.3 (inter-app-comms-real-native-impls Phase 3 T6):
 *
 * - PickImage / PickDocument / PickMultipleImages → `zenity --file-selection` subprocess
 *   (zenity is part of GNOME utilities; documented dep in module README)
 * - Arbitrary ACTION_VIEW → `xdg-open` (handles http/https/mailto/file URIs)
 * - PickContact → `UnsupportedPlatform` per ADR-09 (Linux has no canonical contact-picker)
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        return when (val contract = builder.resultContract) {
            ResultContracts.PickImage,
            ResultContracts.PickDocument -> zenityFilePicker(multiple = false)
            ResultContracts.PickMultipleImages -> zenityFilePicker(multiple = true)
            // ADR-09: Linux has no canonical contact-picker API
            ResultContracts.PickContact -> IntentResult.Failed(IntentError.UnsupportedPlatform)
            null -> arbitraryUrl(builder)
            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private fun zenityFilePicker(multiple: Boolean): IntentResult {
        val cmd = if (multiple) {
            "zenity --file-selection --multiple --separator=$'\\n' 2>/dev/null"
        } else {
            "zenity --file-selection 2>/dev/null"
        }
        val pipe = popen(cmd, "r") ?: return IntentResult.Failed(IntentError.NoHandler)
        return try {
            memScoped {
                val buf = allocArray<kotlinx.cinterop.ByteVar>(4096)
                val firstLine = fgets(buf, 4096, pipe)?.toKString()?.trim()
                if (firstLine.isNullOrBlank()) {
                    IntentResult.Cancelled
                } else {
                    IntentResult.Ok(IntentData(uri = "file://$firstLine"))
                }
            }
        } finally {
            pclose(pipe)
        }
    }

    private fun arbitraryUrl(builder: IntentBuilder): IntentResult {
        val uri = builder.data ?: return IntentResult.Failed(IntentError.NoHandler)
        val escaped = uri.replace("'", "'\\''")
        val rc = system("xdg-open '$escaped' >/dev/null 2>&1")
        return if (rc == 0) IntentResult.Ok(IntentData(uri = uri, mimeType = builder.type)) else IntentResult.Failed(IntentError.NoHandler)
    }
}
