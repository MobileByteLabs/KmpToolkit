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

// LD-2-coverage: full

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIModalPresentationFormSheet
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS `SystemIntents` actual.
 *
 * - `openAppSettings()` — `UIApplication.openURL(UIApplicationOpenSettingsURLString)`.
 * - `createDocument()` — writes a zero-byte placeholder file with [suggestedName] to
 *   `NSTemporaryDirectory()`, presents `UIDocumentPickerViewController` in
 *   `UIDocumentPickerModeExportToService` from the top-most presented VC, captures the
 *   exported URL via `UIDocumentPickerDelegateProtocol`. Delegate pinned via the
 *   shared [SystemIntentsDelegatePin] for the lifetime of the presentation (UIKit
 *   stores `delegate` as a weak reference; without a strong pin K/N's shadow drops
 *   before the callback fires).
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    @Suppress("DEPRECATION") // UIApplication.openURL functional on every supported iOS version
    public actual suspend fun openAppSettings(): IntentResult {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            ?: return IntentResult.Failed(IntentError.Unknown("Failed to construct Settings URL"))
        val opened = UIApplication.sharedApplication.openURL(url)
        return if (opened) {
            IntentResult.Ok(IntentData(uri = UIApplicationOpenSettingsURLString))
        } else {
            IntentResult.Failed(IntentError.NoHandler)
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
        val presenting = resolveTopViewController()
            ?: return IntentResult.Failed(IntentError.Unknown("No root view controller available"))

        val tmpPath = NSTemporaryDirectory() + suggestedName
        val tmpURL = NSURL.fileURLWithPath(tmpPath)
        // Use NSFileManager to write an empty placeholder — avoids the deprecated
        // NSData.writeToURL:atomically: K/N member binding.
        NSFileManager.defaultManager.createFileAtPath(
            path = tmpPath,
            contents = NSData(),
            attributes = null,
        )

        return suspendCancellableCoroutine { cont ->
            // iOS 14+ convenience initializer — defaults to ExportToService mode.
            // Picks a destination folder; iOS copies the placeholder temp file to it
            // and the delegate callback returns the destination URL.
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(tmpURL))
            val delegate = SystemIntentsDocumentPickerDelegate { exportedURL ->
                picker.dismissViewControllerAnimated(true, completion = null)
                cleanupTemp(tmpURL)
                if (cont.isActive) {
                    cont.resume(
                        if (exportedURL == null) {
                            IntentResult.Cancelled
                        } else {
                            IntentResult.Ok(IntentData(uri = exportedURL, mimeType = mimeType))
                        },
                    )
                }
            }
            picker.delegate = delegate
            picker.modalPresentationStyle = UIModalPresentationFormSheet
            SystemIntentsDelegatePin.retain(delegate)
            cont.invokeOnCancellation {
                SystemIntentsDelegatePin.release(delegate)
                picker.dismissViewControllerAnimated(true, completion = null)
                cleanupTemp(tmpURL)
            }
            presenting.presentViewController(picker, animated = true, completion = null)
        }
    }

    private fun cleanupTemp(tmpURL: NSURL) {
        try {
            NSFileManager.defaultManager.removeItemAtURL(tmpURL, null)
        } catch (_: Throwable) {
            // Best effort — OS purges NSTemporaryDirectory periodically anyway.
        }
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

/**
 * Strong-ref pin for the SystemIntents document-picker delegate.
 *
 * UIKit stores `delegate` as a weak ref; without a strong pin held here, ARC drops
 * the Kotlin/Native shadow before the delegate callback fires. Mirrors the
 * `DelegatePin` pattern used by `IntentLauncher.ios.kt`. iOS UI runs on main only
 * so a plain mutable set is safe.
 */
private object SystemIntentsDelegatePin {
    private val pinned: MutableSet<Any> = mutableSetOf()
    fun retain(delegate: Any) {
        pinned.add(delegate)
    }
    fun release(delegate: Any) {
        pinned.remove(delegate)
    }
}

private class SystemIntentsDocumentPickerDelegate(private val onComplete: (exportedURL: String?) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
        SystemIntentsDelegatePin.release(this)
        onComplete(didPickDocumentAtURL.absoluteString)
    }

    @ObjCSignatureOverride
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        SystemIntentsDelegatePin.release(this)
        @Suppress("UNCHECKED_CAST")
        val urls = didPickDocumentsAtURLs as List<NSURL>
        onComplete(urls.firstOrNull()?.absoluteString)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        SystemIntentsDelegatePin.release(this)
        onComplete(null)
    }
}
