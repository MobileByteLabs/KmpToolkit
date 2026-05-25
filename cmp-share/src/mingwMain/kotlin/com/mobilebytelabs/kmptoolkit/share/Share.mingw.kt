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
        is SharePayload.Text -> ShareResult.Failed(ShareError.UnsupportedPlatform)
        is SharePayload.Image -> ShareResult.Failed(ShareError.UnsupportedPlatform)
        is SharePayload.File -> winStart(payload.uri)
        is SharePayload.Multi -> ShareResult.Failed(ShareError.UnsupportedPlatform)
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
