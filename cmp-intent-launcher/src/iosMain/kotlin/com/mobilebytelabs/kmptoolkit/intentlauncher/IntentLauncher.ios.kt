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
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Contacts.CNContact
import platform.ContactsUI.CNContactPickerDelegateProtocol
import platform.ContactsUI.CNContactPickerViewController
import platform.Foundation.NSURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS `IntentLauncher` — picker contracts route through native UIKit pickers via
 * delegate bridges; arbitrary actions fall back to `onUnsupported` / `Failed(UnsupportedPlatform)`.
 *
 * **Contract bindings (v0.2+)**:
 * - `ResultContracts.PickImage` → `PHPickerViewController` (single image, iOS 14+)
 * - `ResultContracts.PickMultipleImages` → `PHPickerViewController` (selectionLimit=0)
 * - `ResultContracts.PickDocument` → `UIDocumentPickerViewController` (open-mode)
 * - `ResultContracts.PickContact` → `CNContactPickerViewController`
 * - `ResultContracts.Custom<R>` / arbitrary action → caller's `onUnsupported` lambda or
 *   `IntentResult.Failed(UnsupportedPlatform)`
 *
 * Delegate lifecycle: the launcher pins a strong reference to the delegate for the
 * lifetime of the in-flight presentation; `suspendCancellableCoroutine` is resumed
 * exactly once from the delegate callback (success / cancel / error). On coroutine
 * cancellation the presented VC is dismissed.
 *
 * Presenting controller resolution: traverses `UIApplication.keyWindow.rootViewController`
 * to the top-most presented VC. Apps that present launchers from a non-key scene should
 * provide an explicit `presentingController` extra (deferred to v0.3 — same hook the
 * cmp-share API exposes today via `ShareOptions.presentingController`).
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {

    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        val contract = builder.resultContract
        val presenting = resolveTopViewController()
            ?: return builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.NoHandler)

        return when (contract) {
            ResultContracts.PickImage -> presentPhotoPicker(presenting, multi = false)

            ResultContracts.PickMultipleImages -> presentPhotoPicker(presenting, multi = true)

            ResultContracts.PickDocument -> presentDocumentPicker(presenting, builder)

            ResultContracts.PickContact -> presentContactPicker(presenting)

            // v0.3 (Phase 3 T1): arbitrary intents dispatch via UIApplication.openURL for URL-shaped
            // data (ACTION_VIEW external-app launch). UIDocumentInteractionController for file URIs
            // deferred to v0.4 (ADR-09: needs full UIDocumentInteractionController lifecycle bridge).
            null -> arbitraryUrlIntent(builder)

            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    /** Dispatch http/https/mailto/tel URL via UIApplication.sharedApplication.openURL. */
    private fun arbitraryUrlIntent(builder: IntentBuilder): IntentResult {
        val uriStr = builder.data ?: return IntentResult.Failed(IntentError.NoHandler)
        val url = NSURL.URLWithString(uriStr) ?: return IntentResult.Failed(IntentError.NoHandler)
        UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any?>(), null)
        return IntentResult.Ok(IntentData(uri = uriStr, mimeType = builder.type))
    }

    private suspend fun presentPhotoPicker(presenting: UIViewController, multi: Boolean): IntentResult =
        suspendCancellableCoroutine { cont ->
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter()
                selectionLimit = if (multi) 0L else 1L
            }
            val vc = PHPickerViewController(configuration = config)
            val delegate = PhotoPickerDelegate { uris ->
                vc.dismissViewControllerAnimated(true, completion = null)
                if (cont.isActive) {
                    cont.resume(
                        when {
                            uris == null -> IntentResult.Failed(IntentError.Unknown("PHPicker error"))

                            uris.isEmpty() -> IntentResult.Cancelled

                            multi -> IntentResult.Ok(
                                IntentData(uri = uris.firstOrNull(), extras = mapOf("uris" to uris)),
                            )

                            else -> IntentResult.Ok(IntentData(uri = uris.first()))
                        },
                    )
                }
            }
            vc.delegate = delegate
            DelegatePin.retain(delegate)
            cont.invokeOnCancellation {
                DelegatePin.release(delegate)
                vc.dismissViewControllerAnimated(true, completion = null)
            }
            presenting.presentViewController(vc, animated = true, completion = null)
        }

    private suspend fun presentDocumentPicker(presenting: UIViewController, builder: IntentBuilder): IntentResult =
        suspendCancellableCoroutine { cont ->
            val utis = utisFromMimeType(builder.type)
            val vc = UIDocumentPickerViewController(
                documentTypes = utis,
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
            )
            val delegate = DocumentPickerDelegate { uri ->
                vc.dismissViewControllerAnimated(true, completion = null)
                if (cont.isActive) {
                    cont.resume(
                        if (uri == null) {
                            IntentResult.Cancelled
                        } else {
                            IntentResult.Ok(IntentData(uri = uri, mimeType = builder.type))
                        },
                    )
                }
            }
            vc.delegate = delegate
            DelegatePin.retain(delegate)
            cont.invokeOnCancellation {
                DelegatePin.release(delegate)
                vc.dismissViewControllerAnimated(true, completion = null)
            }
            presenting.presentViewController(vc, animated = true, completion = null)
        }

    private suspend fun presentContactPicker(presenting: UIViewController): IntentResult =
        suspendCancellableCoroutine { cont ->
            val vc = CNContactPickerViewController()
            val delegate = ContactPickerDelegate { identifier ->
                vc.dismissViewControllerAnimated(true, completion = null)
                if (cont.isActive) {
                    cont.resume(
                        if (identifier == null) {
                            IntentResult.Cancelled
                        } else {
                            IntentResult.Ok(IntentData(uri = identifier))
                        },
                    )
                }
            }
            vc.delegate = delegate
            DelegatePin.retain(delegate)
            cont.invokeOnCancellation {
                DelegatePin.release(delegate)
                vc.dismissViewControllerAnimated(true, completion = null)
            }
            presenting.presentViewController(vc, animated = true, completion = null)
        }

    private fun utisFromMimeType(mime: String?): List<String> = when {
        mime == null -> listOf("public.data")
        mime.startsWith("image/") -> listOf("public.image")
        mime.startsWith("application/pdf") -> listOf("com.adobe.pdf")
        mime.startsWith("text/") -> listOf("public.text")
        else -> listOf("public.data")
    }

    private fun resolveTopViewController(): UIViewController? {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
        var top = rootVc
        while (true) {
            val presented = top.presentedViewController ?: break
            top = presented
        }
        return top
    }
}

// -----------------------------------------------------------------------------
// Delegate bridges — subclass NSObject + implement Apple's delegate protocols.
// -----------------------------------------------------------------------------

private class PhotoPickerDelegate(private val onComplete: (List<String>?) -> Unit) :
    NSObject(),
    PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        @Suppress("UNCHECKED_CAST")
        val results = didFinishPicking as List<PHPickerResult>
        if (results.isEmpty()) {
            onComplete(emptyList())
            return
        }
        // PHPickerResult exposes itemProvider; we surface assetIdentifier when available.
        // For richer asset access (PHAsset / file URL), the consumer can extend via
        // their own NSItemProvider load — v0.3 may expose a higher-level extras map.
        val uris = results.mapNotNull { it.assetIdentifier?.let { id -> "ph://$id" } }
        onComplete(uris.ifEmpty { results.map { "ph://anonymous-${results.indexOf(it)}" } })
    }
}

private class DocumentPickerDelegate(private val onComplete: (String?) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
        onComplete(didPickDocumentAtURL.absoluteString)
    }

    @ObjCSignatureOverride
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        @Suppress("UNCHECKED_CAST")
        val urls = didPickDocumentsAtURLs as List<NSURL>
        onComplete(urls.firstOrNull()?.absoluteString)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onComplete(null)
    }
}

private class ContactPickerDelegate(private val onComplete: (String?) -> Unit) :
    NSObject(),
    CNContactPickerDelegateProtocol {

    override fun contactPicker(picker: CNContactPickerViewController, didSelectContact: CNContact) {
        onComplete(didSelectContact.identifier)
    }

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
        onComplete(null)
    }
}

/**
 * Strong-ref pin for delegate instances during in-flight presentations.
 *
 * Apple's UIViewController stores `delegate` as a weak reference; without a strong
 * ref held elsewhere, ARC would drop the Kotlin/Native shadow before the delegate
 * callback fires. We pin into a singleton set, release on completion or cancellation.
 */
private object DelegatePin {
    // iOS UI presentation always runs on the main thread, so a plain set is safe.
    private val pinned: MutableSet<Any> = mutableSetOf()

    fun retain(delegate: Any) {
        pinned.add(delegate)
    }

    fun release(delegate: Any) {
        pinned.remove(delegate)
    }
}
