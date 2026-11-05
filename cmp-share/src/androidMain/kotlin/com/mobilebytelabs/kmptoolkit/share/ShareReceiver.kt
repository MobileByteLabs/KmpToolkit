/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import android.content.Intent

/**
 * Reads what another app shared into this one. Call it with the launch Intent and from `onNewIntent`:
 *
 * ```kotlin
 * ShareReceiver.fromIntent(intent)?.let { shared -> viewModel.onShared(shared) }
 * ```
 *
 * The Activity needs an `ACTION_SEND` intent filter for the types it accepts (e.g. `text/plain`, `image/\*`).
 */
public object ShareReceiver {
    /** The shared content in [intent], or null when it is not a share (a normal launch, a deep link). */
    public fun fromIntent(intent: Intent?): SharedContent? {
        if (intent == null) return null
        // The stream is read from ClipData: Android copies EXTRA_STREAM into it for every ACTION_SEND it
        // starts (Intent.migrateExtraStreamToClipData), and it is the only non-deprecated read on every API.
        val stream = intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        return SharedContent.parse(
            action = intent.action,
            mimeType = intent.type,
            text = intent.getStringExtra(Intent.EXTRA_TEXT),
            subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
            streamUri = stream?.toString(),
        )
    }
}
