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

import platform.posix.system

/**
 * mingw (Windows) `Share` — `cmd /c start` for URL share + `cmd /c "echo TEXT | clip"`
 * for text. Image / File / Multi binary payloads → `UnsupportedPlatform`.
 *
 * **Win32 cinterop status**: `win32-clipboard.def` (CF_DIB binary clipboard, Phase 2
 * ADR-09 #3 closure) compiles → klib successfully on macOS-arm64 K/N hosts but the
 * generated bindings do NOT expose the Win32 SDK types (`OpenClipboard`, `CF_DIB`,
 * `SetClipboardData`, etc.) as Kotlin symbols — K/N cinterop's parser drops the SDK
 * struct types when run on a non-Windows host (only `static inline` helper functions
 * end up in the klib).
 *
 * Real binary-clipboard round-trip needs either:
 * 1. A Windows CI host running the cinterop step (where `windows.h` resolves natively)
 * 2. A cinterop wrapper layer that exposes clipboard state through helper functions
 *    only, never as SDK struct refs (rewrite the .def to be Win32-SDK-opaque)
 *
 * Both deferred to post-v0.4. ADR-09 #3 audit-log updated: WONTFIX-PROVISIONAL remains
 * in effect; `win32-clipboard.def` ships as a future-use artifact.
 *
 * **Security:** URL is double-quote-wrapped + embedded quotes escaped to prevent
 * cmd injection.
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Url -> winStart(payload.href)

        is SharePayload.Text -> winClipText(payload.content)

        is SharePayload.Image,
        is SharePayload.File,
        -> ShareResult.Failed(ShareError.UnsupportedPlatform)

        is SharePayload.Multi -> multi(payload)
    }

    /** Dispatch URL via Windows shell resolver (`start ""` consumes a title arg). */
    private fun winStart(href: String): ShareResult {
        val escaped = href.replace("\"", "\\\"")
        val rc = system("cmd /c start \"\" \"$escaped\"")
        return if (rc == 0) ShareResult.Completed else ShareResult.Failed(ShareError.Unknown("cmd start exit=$rc"))
    }

    /** Copy text to clipboard via Windows built-in `clip.exe` (since Windows XP). */
    private fun winClipText(text: String): ShareResult {
        // Newlines in text would break the single-line command — replace with a placeholder
        // and re-emit via echo's `^M` continuation? Simpler: only single-line text copied;
        // multi-line falls back to NoHandler. Production consumers wanting binary-safe
        // clipboard should ship their own clip-replacement.
        if (text.contains('\n') || text.contains('"')) {
            return ShareResult.Failed(ShareError.Unknown("clip.exe path limited to single-line non-quoted text"))
        }
        val rc = system("cmd /c \"echo $text | clip\"")
        return if (rc == 0) ShareResult.Completed else ShareResult.Failed(ShareError.Unknown("clip exit=$rc"))
    }

    private fun multi(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r = when (item) {
                is SharePayload.Text -> winClipText(item.content)
                is SharePayload.Url -> winStart(item.href)
                else -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            }
            if (r is ShareResult.Completed) return r
        }
        return ShareResult.Failed(ShareError.NoHandler)
    }
}
