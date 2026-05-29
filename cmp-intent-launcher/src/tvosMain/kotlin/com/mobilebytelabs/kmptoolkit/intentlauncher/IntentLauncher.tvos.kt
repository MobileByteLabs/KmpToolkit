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

/**
 * tvOS `IntentLauncher` — **ADR-09 #5 WONTFIX (architectural)** per v0.4 docs refresh.
 *
 * tvOS lacks PHPicker, UIDocumentPicker, AND CNContactPicker — Apple has not shipped these
 * UIKit APIs to tvOS at the K/N binding layer (or at the underlying iOS framework layer for
 * non-photo pickers). This is an architectural OS limitation, NOT deferred work. The arbitrary
 * URL launch path (`UIApplication.openURL`) is also gated by tvOS-specific availability.
 *
 * No v0.5/v1.x plan to close this without Apple shipping the underlying UIKit APIs to tvOS.
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        builder.onUnsupportedHandler?.let { return it.invoke() }
        return IntentResult.Failed(IntentError.UnsupportedPlatform)
    }
}
