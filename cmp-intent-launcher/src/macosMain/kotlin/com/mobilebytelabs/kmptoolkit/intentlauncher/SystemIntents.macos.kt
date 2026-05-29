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

package com.mobilebytelabs.kmptoolkit.intentlauncher

import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSSavePanel
import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

/**
 * macOS `SystemIntents` actual.
 *
 * - `openAppSettings()` opens System Settings via `x-apple.systempreferences:`.
 * - `createDocument()` runs `NSSavePanel` modally on the main queue; returns the
 *   selected URL on `NSModalResponseOK`, `Cancelled` otherwise.
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult {
        val url = NSURL.URLWithString("x-apple.systempreferences:")
            ?: return IntentResult.Failed(IntentError.Unknown("Failed to construct System Preferences URL"))
        val opened = NSWorkspace.sharedWorkspace.openURL(url)
        return if (opened) {
            IntentResult.Ok(IntentData(uri = "x-apple.systempreferences:"))
        } else {
            IntentResult.Failed(IntentError.NoHandler)
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
        val panel = NSSavePanel.savePanel()
        panel.nameFieldStringValue = suggestedName
        val response = panel.runModal()
        return if (response == NSModalResponseOK) {
            val url = panel.URL
            if (url != null) {
                IntentResult.Ok(IntentData(uri = url.absoluteString, mimeType = mimeType))
            } else {
                IntentResult.Failed(IntentError.Unknown("NSSavePanel returned OK with null URL"))
            }
        } else {
            IntentResult.Cancelled
        }
    }
}
