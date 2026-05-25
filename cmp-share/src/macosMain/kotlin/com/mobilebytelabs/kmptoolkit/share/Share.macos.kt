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
import platform.AppKit.NSApplication
import platform.AppKit.NSImage
import platform.AppKit.NSSharingServicePicker
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create

// NSMinYEdge constant — Kotlin/Native AppKit bindings expose this as a raw UInt enum.
// NSMinXEdge = 0, NSMinYEdge = 1, NSMaxXEdge = 2, NSMaxYEdge = 3
private const val NS_MIN_Y_EDGE: ULong = 1uL

/**
 * macOS `NSSharingServicePicker` implementation.
 *
 * Anchored to the key window's `contentView` (Phase 0 TS5 — `NSApplication.keyWindow`
 * traversal pattern, NOT Compose-MP `LocalView`).
 *
 * Note: NSSharingServicePicker is NOT async — it returns immediately and the user
 * selects a service which fires asynchronously. We return [ShareResult.Completed] on
 * successful popover present; for richer completion tracking, consumers should set
 * `NSSharingServicePickerDelegate` themselves (out of v0.1 scope).
 */
@ExperimentalShareApi
public actual object Share {

    @OptIn(ExperimentalForeignApi::class)
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        val items = buildItems(payload)
        if (items.isEmpty()) {
            return ShareResult.Failed(ShareError.Unknown("Empty payload"))
        }

        val anchorView = resolveAnchorView()
            ?: return ShareResult.Failed(ShareError.NoHandler)

        return try {
            val picker = NSSharingServicePicker(items = items)
            picker.showRelativeToRect(
                rect = CGRectMake(0.0, 0.0, 0.0, 0.0),
                ofView = anchorView,
                preferredEdge = NS_MIN_Y_EDGE,
            )
            ShareResult.Completed
        } catch (e: Throwable) {
            ShareResult.Failed(ShareError.Unknown(e.message ?: "macOS share error"))
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun buildItems(payload: SharePayload): List<Any> = when (payload) {
        is SharePayload.Text -> listOf(NSString.create(string = payload.content))

        is SharePayload.Url -> listOf(
            NSURL.URLWithString(payload.href) ?: NSString.create(string = payload.href),
        )

        is SharePayload.Image -> {
            val img = imageFromBytes(payload.bytes)
            if (img != null) listOf(img) else listOf(NSString.create(string = "<image>"))
        }

        is SharePayload.File -> listOf(
            NSURL.URLWithString(payload.uri) ?: NSString.create(string = payload.uri),
        )

        is SharePayload.Multi -> payload.items.flatMap { buildItems(it) }
    }

    private fun imageFromBytes(bytes: ByteArray): NSImage? {
        if (bytes.isEmpty()) return null
        val data = bytes.toNSData() ?: return null
        return NSImage(data = data)
    }

    private fun resolveAnchorView(): NSView? {
        val app = NSApplication.sharedApplication
        return app.keyWindow?.contentView
            ?: app.mainWindow?.contentView
            ?: firstVisibleWindowContentView(app)
    }

    @Suppress("UNCHECKED_CAST")
    private fun firstVisibleWindowContentView(app: NSApplication): NSView? {
        val windows = app.windows as List<NSWindow>
        return windows.firstOrNull { it.visible }?.contentView
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
