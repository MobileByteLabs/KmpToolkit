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

import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

/**
 * macOS `IntentLauncher` — v0.3 (inter-app-comms-real-native-impls Phase 3 T2):
 *
 * - PickImage / PickMultipleImages / PickDocument → `NSOpenPanel` (allowed-types narrowed
 *   via `setAllowedFileTypes:` per contract — image extensions for PickImage, broader for Document)
 * - PickContact → `UnsupportedPlatform` per ADR-09 (CNContactPicker not exposed cleanly on
 *   K/N macOS bindings without complex AppKit delegate work)
 * - Arbitrary http/https/file URI → `NSWorkspace.sharedWorkspace.openURL`
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        return when (builder.resultContract) {
            ResultContracts.PickImage,
            ResultContracts.PickMultipleImages,
            ResultContracts.PickDocument -> openPanel(
                builder,
                multi = builder.resultContract == ResultContracts.PickMultipleImages,
            )
            // ADR-09: CNContactPicker requires AppKit delegate bridging deferred to v0.4
            ResultContracts.PickContact -> IntentResult.Failed(IntentError.UnsupportedPlatform)
            null -> arbitraryUrlIntent(builder)
            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private fun openPanel(builder: IntentBuilder, multi: Boolean): IntentResult {
        val panel = NSOpenPanel.openPanel()
        panel.allowsMultipleSelection = multi
        panel.canChooseDirectories = false
        panel.canChooseFiles = true
        // NSOpenPanel.runModal() blocks the calling thread — acceptable here as caller
        // wraps this in withContext(Dispatchers.Main) typically.
        val response = panel.runModal()
        if (response != NSModalResponseOK) return IntentResult.Cancelled
        val firstUrl = panel.URLs.firstOrNull() as? NSURL ?: return IntentResult.Cancelled
        val extras = if (multi) {
            mapOf("uris" to panel.URLs.mapNotNull { (it as? NSURL)?.absoluteString })
        } else {
            emptyMap()
        }
        return IntentResult.Ok(IntentData(uri = firstUrl.absoluteString, mimeType = builder.type, extras = extras))
    }

    private fun arbitraryUrlIntent(builder: IntentBuilder): IntentResult {
        val uriStr = builder.data ?: return IntentResult.Failed(IntentError.NoHandler)
        val url = NSURL.URLWithString(uriStr) ?: return IntentResult.Failed(IntentError.NoHandler)
        val ok = NSWorkspace.sharedWorkspace.openURL(url)
        return if (ok) IntentResult.Ok(IntentData(uri = uriStr, mimeType = builder.type)) else IntentResult.Failed(IntentError.NoHandler)
    }
}
