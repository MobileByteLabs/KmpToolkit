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
 * Linux `Share` — `xdg-open` for URL share; everything else returns Unsupported.
 *
 * `xdg-open` is part of `xdg-utils` (installed by default on most desktop distros).
 * For URL/text payloads we hand a `mailto:` / `https://` / `sms:` URL to the user's
 * configured handler.  Image/file payloads with binary data and the multi-payload
 * variant remain unsupported (no portable Linux share-sheet exists).
 *
 * **Security:** the URL is single-quote-wrapped and any embedded single quote is
 * escaped to prevent shell injection.  Caller-supplied URLs are NOT trusted to be
 * shell-safe.
 *
 * v0.2 sub-plan 10.B — xdg-utils dependency documented in module README.
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        when (payload) {
            is SharePayload.Url -> xdgOpen(payload.href)
            is SharePayload.Text -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            is SharePayload.Image -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            is SharePayload.File -> xdgOpen(uriFromFile(payload.uri))
            is SharePayload.Multi -> ShareResult.Failed(ShareError.UnsupportedPlatform)
        }

    private fun xdgOpen(rawTarget: String): ShareResult {
        val target = rawTarget.replace("'", "'\\''")
        val cmd = "xdg-open '$target' >/dev/null 2>&1"
        val rc = system(cmd)
        return if (rc == 0) ShareResult.Completed
        else ShareResult.Failed(ShareError.Unknown("xdg-open exit=$rc (is xdg-utils installed?)"))
    }

    private fun uriFromFile(uri: String): String =
        if (uri.startsWith("file://") || uri.contains("://")) uri
        else "file://$uri"
}
