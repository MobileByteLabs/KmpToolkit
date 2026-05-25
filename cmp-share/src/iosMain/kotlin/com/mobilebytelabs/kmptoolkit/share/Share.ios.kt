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

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

/**
 * iOS `UIActivityViewController` implementation.
 *
 * Anchored to the key window's root view controller (traverses to top-most presented VC).
 * Caller may override via `ShareOptions.presentingController` (cast to [UIViewController]).
 *
 * Completion bridged via `completionWithItemsHandler` → `suspendCancellableCoroutine`.
 */
@ExperimentalShareApi
public actual object Share {

    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        val items = buildActivityItems(payload)
        if (items.isEmpty()) {
            return ShareResult.Failed(ShareError.Unknown("Empty payload"))
        }

        val presenting = (options.presentingController as? UIViewController)
            ?: resolveTopViewController()
            ?: return ShareResult.Failed(ShareError.NoHandler)

        val vc = UIActivityViewController(activityItems = items, applicationActivities = null)
        if (options.excludedActivities.isNotEmpty()) {
            vc.excludedActivityTypes = options.excludedActivities
        }

        return suspendCancellableCoroutine { cont ->
            vc.completionWithItemsHandler = { _, completed, _, error ->
                val result = when {
                    error != null -> ShareResult.Failed(
                        ShareError.Unknown(error.localizedDescription ?: "iOS share error")
                    )
                    completed -> ShareResult.Completed
                    else -> ShareResult.Cancelled
                }
                if (cont.isActive) cont.resume(result)
            }
            presenting.presentViewController(vc, animated = true, completion = null)
            cont.invokeOnCancellation {
                vc.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun buildActivityItems(payload: SharePayload): List<Any> = when (payload) {
        is SharePayload.Text -> listOf(NSString.create(string = payload.content))
        is SharePayload.Url -> listOf(
            NSURL.URLWithString(payload.href) ?: NSString.create(string = payload.href)
        )
        is SharePayload.Image -> {
            val image = imageFromBytes(payload.bytes)
            if (image != null) listOf(image) else listOf(NSString.create(string = "<image>"))
        }
        is SharePayload.File -> listOf(
            NSURL.URLWithString(payload.uri) ?: NSString.create(string = payload.uri)
        )
        is SharePayload.Multi -> payload.items.flatMap { buildActivityItems(it) }
    }

    private fun imageFromBytes(bytes: ByteArray): UIImage? {
        if (bytes.isEmpty()) return null
        val data = bytes.toNSData() ?: return null
        return UIImage.imageWithData(data)
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
