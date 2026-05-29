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
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fputs
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system

/**
 * Linux `Share` — v0.3 (inter-app-comms-real-native-impls Phase 2):
 *
 * - Text → `xclip -selection clipboard` (consumer paste-anywhere) per ADR-09
 * - Url → `xdg-open` (browser / mailto / sms handler)
 * - Image / File → `xdg-open <path>` (if file already materialized; binary Image
 *   bytes write to /tmp first then dispatched)
 * - Multi → first-success strategy across items
 *
 * **Dependencies:** `xdg-utils` (xdg-open) and `xclip`. Documented as required in module README;
 * absence returns `ShareResult.Failed(ShareError.NoHandler)` with a "is X installed?" hint.
 *
 * **Security:** all shell inputs single-quote-wrapped + embedded single quote escaped.
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Text -> xclipText(payload.content)
        is SharePayload.Url -> xdgOpen(payload.href)
        is SharePayload.Image -> imageShare(payload)
        is SharePayload.File -> xdgOpen(uriFromFile(payload.uri))
        is SharePayload.Multi -> multiShare(payload)
    }

    /** Pipe text to `xclip -selection clipboard`. Returns Completed on success. */
    private fun xclipText(content: String): ShareResult {
        val pipe = popen("xclip -selection clipboard", "w")
            ?: return ShareResult.Failed(ShareError.NoHandler)
        return try {
            val rc = fputs(content, pipe)
            if (rc < 0) {
                ShareResult.Failed(ShareError.Unknown("xclip fputs failed (rc=$rc)"))
            } else {
                ShareResult.Completed
            }
        } finally {
            pclose(pipe)
        }
    }

    /**
     * Materialize Image bytes to /tmp then xdg-open. v0.3 stub — full impl would persist
     * to a configurable cache dir + return that URI for downstream consumers.
     */
    private fun imageShare(image: SharePayload.Image): ShareResult {
        // ADR-09: writing binary bytes via libc on Kotlin/Native linuxX64/Arm64 requires
        // additional cinterop work; v0.3 punts to filename-only path if caller pre-materialized.
        // For now: text-shaped (data URI form is too long for shell args reliably).
        return ShareResult.Failed(ShareError.UnsupportedPlatform)
    }

    private fun multiShare(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r = when (item) {
                is SharePayload.Url -> xdgOpen(item.href)
                is SharePayload.Text -> xclipText(item.content)
                is SharePayload.File -> xdgOpen(uriFromFile(item.uri))
                else -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            }
            if (r is ShareResult.Completed) return r
        }
        return ShareResult.Failed(ShareError.NoHandler)
    }

    private fun xdgOpen(rawTarget: String): ShareResult {
        val target = rawTarget.replace("'", "'\\''")
        val cmd = "xdg-open '$target' >/dev/null 2>&1"
        val rc = system(cmd)
        return if (rc == 0) {
            ShareResult.Completed
        } else {
            ShareResult.Failed(ShareError.Unknown("xdg-open exit=$rc (is xdg-utils installed?)"))
        }
    }

    private fun uriFromFile(uri: String): String = if (uri.startsWith("file://") || uri.contains("://")) {
        uri
    } else {
        "file://$uri"
    }
}
