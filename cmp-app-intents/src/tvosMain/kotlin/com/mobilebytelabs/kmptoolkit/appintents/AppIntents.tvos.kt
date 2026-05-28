/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(
    kotlin.experimental.ExperimentalObjCName::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.mobilebytelabs.kmptoolkit.appintents

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

/**
 * tvOS `AppIntents` — v0.4 (inter-app-comms-compose-completeness Phase 4 — closes ADR-09 #11):
 *
 * Manifest write to NSDocumentDirectory + AppIntentsCallback Swift bridge handoff — same pattern
 * as iOS. tvOS 14+ supports a subset of App Intents (Siri Suggestions on Apple TV); the Swift
 * bridge's `#if canImport(CoreSpotlight)` guard means CSSearchableIndex calls silently no-op
 * (CoreSpotlight is not available on tvOS) while manifest registration + App Shortcuts continues
 * to work where the OS permits.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        AppIntentsCallback.shared.handler = { id, params ->
            kotlinx.coroutines.runBlocking {
                AppIntentsRuntime.invoke(id, params) ?: AppIntentResult.Failed("Unknown intent: $id")
            }
        }
        writeManifest(config.serializeManifest())
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    private fun writeManifest(json: String) {
        try {
            val fm = NSFileManager.defaultManager
            val docsUrl: NSURL = fm.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            ) ?: return
            val manifestUrl = docsUrl.URLByAppendingPathComponent("cmp-app-intents-manifest.json") ?: return
            val bytes = json.encodeToByteArray()
            val data: NSData = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToURL(manifestUrl, atomically = true)
        } catch (_: Throwable) {
            // Best-effort
        }
    }
}
