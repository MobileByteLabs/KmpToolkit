/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memcpy
import kotlinx.cinterop.usePinned
import platform.posix.system
import win32clipboard.CF_DIB
import win32clipboard.CloseClipboard
import win32clipboard.EmptyClipboard
import win32clipboard.GMEM_MOVEABLE
import win32clipboard.OpenClipboard
import win32clipboard.SetClipboardData
import win32clipboard.spike_alloc_global
import win32clipboard.spike_lock_global
import win32clipboard.spike_unlock_global

/**
 * mingw (Windows) `Share` — `cmd /c start` for URL share; everything else Unsupported.
 *
 * `start` resolves the URL/file against the Windows registry and launches the
 * configured handler (Edge for http, mailto: handler for mailto:, default opener
 * for files).  This is the closest Win32-portable equivalent of `xdg-open`
 * without taking a shell32.dll cinterop dependency.
 *
 * Note: image/file binary payloads and Multi remain Unsupported. Win32 clipboard
 * (OpenClipboard / SetClipboardData) is a v0.3 candidate — needs Win32 cinterop.
 *
 * **Security:** the URL is double-quote-wrapped (Windows uses double quotes for
 * cmd args) and embedded double quotes are escaped to prevent injection.
 *
 * v0.2 sub-plan 10.B.
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Url -> winStart(payload.href)

        // v0.3 (Phase 2 T5): `clip.exe` is a Windows built-in (since XP) — copies stdin to clipboard
        is SharePayload.Text -> winClipText(payload.content)

        // v0.4 (inter-app-comms-compose-completeness Phase 2 — closes ADR-09 #3):
        // CF_DIB binary clipboard write via Win32 cinterop (win32-clipboard.def from v0.3 spike).
        // Image.bytes is expected to be in DIB/BMP format minus the 14-byte BITMAPFILEHEADER.
        // Note: caller responsibility to strip the BMP file header before passing — most image
        // libraries (Skia, ImageIO) provide DIB-format export directly.
        is SharePayload.Image -> winClipboardImageDib(payload.bytes)

        is SharePayload.File -> winStart(payload.uri)

        is SharePayload.Multi -> multiShare(payload)
    }

    /** Pipe text into the Windows built-in `clip.exe` utility. Returns Completed on rc==0. */
    private fun winClipText(content: String): ShareResult {
        // Escape embedded characters that `cmd /c echo ... | clip` would interpret.
        // Use `echo|set/p=` trick to avoid trailing newline.
        val escaped = content
            .replace("^", "^^")
            .replace("&", "^&")
            .replace("|", "^|")
            .replace("<", "^<")
            .replace(">", "^>")
            .replace("\"", "\\\"")
        val cmd = "cmd /c \"echo|set/p=\"$escaped\" | clip\""
        val rc = system(cmd)
        return if (rc == 0) {
            ShareResult.Completed
        } else {
            ShareResult.Failed(ShareError.Unknown("clip.exe exit=$rc"))
        }
    }

    private fun multiShare(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r = when (item) {
                is SharePayload.Url -> winStart(item.href)
                is SharePayload.Text -> winClipText(item.content)
                is SharePayload.File -> winStart(item.uri)
                else -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            }
            if (r is ShareResult.Completed) return r
        }
        return ShareResult.Failed(ShareError.NoHandler)
    }

    /**
     * Write image bytes (DIB format) to Win32 clipboard as CF_DIB.
     * Caller bytes MUST be DIB (BITMAPINFOHEADER + pixel data) — NOT a full BMP file (no file header).
     * Returns Completed on Win32 success; NoHandler if OpenClipboard fails.
     */
    private fun winClipboardImageDib(bytes: ByteArray): ShareResult {
        if (OpenClipboard(null) == 0) {
            return ShareResult.Failed(ShareError.NoHandler)
        }
        return try {
            EmptyClipboard()
            val h = spike_alloc_global(bytes.size.convert())
                ?: return ShareResult.Failed(ShareError.Unknown("GlobalAlloc failed"))
            val locked = spike_lock_global(h)
                ?: return ShareResult.Failed(ShareError.Unknown("GlobalLock failed"))
            bytes.usePinned { pinned ->
                memcpy(locked, pinned.addressOf(0), bytes.size.convert())
            }
            spike_unlock_global(h)
            SetClipboardData(CF_DIB.convert(), h)
            ShareResult.Completed
        } finally {
            CloseClipboard()
        }
    }

    private fun winStart(rawTarget: String): ShareResult {
        val target = rawTarget.replace("\"", "\\\"")
        // `start` needs an empty title arg ("") when the target is quoted.
        val cmd = "cmd /c start \"\" \"$target\""
        val rc = system(cmd)
        return if (rc == 0) {
            ShareResult.Completed
        } else {
            ShareResult.Failed(ShareError.Unknown("cmd start exit=$rc"))
        }
    }
}
