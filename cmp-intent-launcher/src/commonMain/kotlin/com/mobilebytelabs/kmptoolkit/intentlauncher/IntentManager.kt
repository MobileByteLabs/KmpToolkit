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

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * Injectable entry point for launching system intents — the type to depend on from a ViewModel,
 * repository or composable.
 *
 * ## Why an interface when [IntentLauncher] already exists
 * [IntentLauncher] is an `expect class`, so code calling it directly cannot be substituted: no
 * fake in tests, no decorator for analytics or permission checks. [IntentManager] is the same
 * capability behind an injectable type, and it adds the [capabilities] probe that lets a caller
 * ask before it offers. [IntentLauncher] stays public and unchanged.
 *
 * ## Implementing
 * Only [capabilities] and [launch] are abstract; every other member derives from them, so a test
 * double is short:
 *
 * ```kotlin
 * class FakeIntentManager(
 *     override val capabilities: IntentCapabilities = IntentCapabilities.Full,
 * ) : IntentManager {
 *     override suspend fun launch(block: IntentBuilder.() -> Unit) = IntentResult.Cancelled
 * }
 * ```
 *
 * ## Using
 * ```kotlin
 * class AvatarViewModel(private val intents: IntentManager) : ViewModel() {
 *     val canPick = intents.supports(IntentOperation.PickImage)
 *
 *     fun choose() = viewModelScope.launch {
 *         when (val r = intents.pickImage()) {
 *             is IntentResult.Ok -> setAvatar(r.data?.uri)
 *             is IntentResult.Cancelled -> Unit
 *             is IntentResult.Failed -> showError(r.cause)
 *         }
 *     }
 * }
 * ```
 */
public interface IntentManager {

    /** What this target can do. See [platformIntentCapabilities] for the matrix. */
    public val capabilities: IntentCapabilities

    /** Launch an arbitrary intent. The single abstract operation — everything else routes here. */
    public suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult

    /** Hand [uri] to whatever handles it — a browser, mail client or file handler. */
    public suspend fun viewUri(uri: String): IntentResult = launch {
        action("VIEW")
        data(uri)
    }

    /** Let the user pick one image. [IntentResult.Ok] carries its URI in `data.uri`. */
    public suspend fun pickImage(): IntentResult = launch {
        type("image/*")
        result(ResultContracts.PickImage)
    }

    /** Let the user pick several images. URIs arrive in `data.extras["uris"]`. */
    public suspend fun pickMultipleImages(): IntentResult = launch {
        type("image/*")
        result(ResultContracts.PickMultipleImages)
    }

    /** Let the user pick a document, optionally narrowed by [mimeType]. */
    public suspend fun pickDocument(mimeType: String = "*/*"): IntentResult = launch {
        type(mimeType)
        result(ResultContracts.PickDocument)
    }

    /** Let the user pick a contact. */
    public suspend fun pickContact(): IntentResult = launch {
        result(ResultContracts.PickContact)
    }

    /** Open this app's settings screen. */
    public suspend fun openAppSettings(): IntentResult = SystemIntents.openAppSettings()

    /** Ask the user where to save a new file, and return the chosen location. */
    public suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        SystemIntents.createDocument(suggestedName, mimeType)

    /**
     * Whether [operation] can be performed here — ask BEFORE rendering an affordance for it,
     * rather than offering an action that fails once tapped.
     */
    public fun supports(operation: IntentOperation): Boolean = operation in capabilities
}

/**
 * The default [IntentLauncher] for this target, or `null` where one cannot exist without more
 * context than the library has.
 *
 * Android is the `null` case: its launcher is **Activity-scoped**, wrapping an
 * `ActivityResultLauncher` plus the pending-result plumbing, so there is nothing sensible to
 * construct from nowhere. Every other target's launcher is stateless.
 */
internal expect fun defaultIntentLauncher(): IntentLauncher?

/**
 * The one [IntentManager] — for every target.
 *
 * Holds an [IntentLauncher] and delegates; all per-target behaviour lives there.
 *
 * ## The Android caveat, made honest
 * On Android [defaultIntentLauncher] returns `null`, because a launcher needs an Activity. Rather
 * than fail at the call site, [capabilities] **drops the picker operations** in that state and
 * keeps only the two that genuinely work without an Activity — `openAppSettings` and
 * `createDocument`, which the library's own `IntentLauncherInitProvider` and
 * `CreateDocumentProxyActivity` service. So `supports()` stays truthful and a UI hides pickers it
 * cannot run, instead of offering a button that fails once tapped.
 *
 * Pass a real launcher to get the full surface:
 *
 * ```kotlin
 * // Android, inside a ComponentActivity
 * val manager = IntentManagerImpl(intentLauncher())
 *
 * // everywhere else — the default is already correct
 * val manager = IntentManagerImpl()
 * ```
 */
public class IntentManagerImpl(private val launcher: IntentLauncher? = defaultIntentLauncher()) : IntentManager {

    override val capabilities: IntentCapabilities
        get() {
            val platform = platformIntentCapabilities
            if (launcher != null) return platform
            // No launcher: only the SystemIntents-backed operations can still run.
            return platform.copy(
                viewUri = false,
                pickImage = false,
                pickMultipleImages = false,
                pickDocument = false,
                pickContact = false,
            )
        }

    override suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val target = launcher
        if (target == null) {
            // Worth its own event: on Android this is the misconfiguration that makes every
            // Activity-scoped operation fail, and it is otherwise only visible in the returned
            // error string a caller may well swallow.
            observeLifecycle(cmpMetadata(), "launch_no_launcher", emptyMap())
            return IntentResult.Failed(
                IntentError.Unknown(
                    "No IntentLauncher available. On Android a launcher is Activity-scoped: obtain " +
                        "one with ComponentActivity.intentLauncher() and pass it to IntentManagerImpl. " +
                        "openAppSettings() and createDocument() work without one.",
                ),
            )
        }
        // Result CLASS only — an intent's extras carry user content (picked file URIs, contacts).
        return target.launch(block).also {
            observeLifecycle(cmpMetadata(), "intent_launched", mapOf("result" to it::class.simpleName))
        }
    }
}
