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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.system

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
        // ADR-09: Win32 clipboard CF_DIB binary write requires GlobalAlloc/SetClipboardData cinterop
        // marshalling; spike .def at cinterop/win32-clipboard.def proves binding generation; the
        // Kotlin/Native pointer round-trip for binary data deferred to v0.4.
        is SharePayload.Image -> ShareResult.Failed(ShareError.UnsupportedPlatform)
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
