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

import platform.Foundation.NSURL
import platform.WatchKit.WKExtension

/**
 * watchOS `IntentLauncher` — v0.3 (inter-app-comms-real-native-impls Phase 3 T5):
 *
 * - Arbitrary http/https/mailto URL → `WKExtension.sharedExtension.openSystemURL` (real impl)
 * - All picker contracts → `UnsupportedPlatform` per ADR-09 (watchOS has no PHPicker /
 *   UIDocumentPicker / CNContactPicker)
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        val uri = builder.data
        val contract = builder.resultContract
        // ADR-09: watchOS has no canonical picker surface
        if (contract != null) return IntentResult.Failed(IntentError.UnsupportedPlatform)
        // Arbitrary ACTION_VIEW for http/https/mailto
        if (uri != null && (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("mailto:"))) {
            val url = NSURL.URLWithString(uri) ?: return IntentResult.Failed(IntentError.NoHandler)
            WKExtension.sharedExtension().openSystemURL(url)
            return IntentResult.Ok(IntentData(uri = uri))
        }
        return builder.onUnsupportedHandler?.invoke()
            ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
    }
}
