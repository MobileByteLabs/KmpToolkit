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
 * iOS `AppIntents` — split-responsibility design that keeps Kotlin's surface minimal
 * and pushes platform-rich behaviour (Spotlight indexing, App Shortcuts surfacing)
 * into the Swift bridge.
 *
 * **Kotlin responsibilities** (this file):
 * 1. Write `cmp-app-intents-manifest.json` to the app documents directory — single
 *    source of truth that the Swift bridge reads at app launch (ADR-04 ship-source-file
 *    pattern).
 * 2. Expose `AppIntentsCallback.shared` via `@ObjCName` so Swift's per-intent
 *    `@AppIntent.perform()` body can invoke the Kotlin DSL's `perform` lambda
 *    through `AppIntentsRuntime.invoke(id, params)`.
 *
 * **Swift-bridge responsibilities** (`swift/CmpAppIntentBridge.swift`):
 * - `loadManifest()` — decode the JSON manifest emitted here.
 * - `indexSpotlightItems()` — for every `searchable: true` manifest entry, build a
 *   `CSSearchableItem` (with full title + contentDescription via the native API,
 *   which K/N cannot surface because those attributes live in NSObject category
 *   extensions) and push into `CSSearchableIndex.defaultSearchableIndex()`.
 * - `AppShortcutsProvider` — consumer copies the bundled stub template per intent.
 *
 * This separation matches `cmp-deep-link` (Kotlin emits state, Swift consumes via
 * `@ObjCName` singleton + SwiftUI `.onOpenURL`) and is the canonical pattern for
 * Apple APIs whose surface K/N cannot reach (category-only properties, SwiftUI
 * modifiers, AppIntents macros).
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
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToURL(manifestUrl, atomically = true)
        } catch (_: Throwable) {
            // Best-effort; manifest persistence is non-fatal for in-process invocation.
        }
    }
}

/**
 * Singleton callback holder exposed to Swift via Kotlin/Native ObjC interop.
 *
 * Swift's `CmpAppIntentBridge.swift` calls `handler(id, params)` to route an iOS
 * App Intent invocation into the Kotlin DSL's `perform` lambda.
 *
 * Pattern mirrors `cmp-deep-link/swift/DeepLinkPlugin.swift` ↔ `DeepLinkAppleHelper.shared`.
 */
@ObjCName("CmpAppIntentsCallback")
@OptIn(ExperimentalAppIntentsApi::class)
public class AppIntentsCallback {
    public var handler: ((String, Map<String, Any>) -> AppIntentResult)? = null

    public companion object {
        @ObjCName("shared")
        public val shared: AppIntentsCallback = AppIntentsCallback()
    }
}
