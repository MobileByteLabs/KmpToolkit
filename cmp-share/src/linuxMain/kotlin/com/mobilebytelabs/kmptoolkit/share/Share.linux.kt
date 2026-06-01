/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: full

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system
import kotlin.random.Random

/**
 * Linux `Share` — full coverage as of 2026-06-01 (cmp-intent-share-coverage-trueup sub-plan 02):
 *
 * - Text → `xclip -selection clipboard` (consumer paste-anywhere) per ADR-09
 * - Url → `xdg-open` (browser / mailto / sms handler)
 * - Image → POSIX `fopen`+`fwrite` to `$TMPDIR/cmp-share-{basename}-{rand}{.ext}`, then
 *   `xdg-open <path>` (registered image viewer / GUI app handles the share UX)
 * - File → `xdg-open <uri>` (if already materialized)
 * - Multi → first-success strategy across items
 *
 * **Dependencies:** `xdg-utils` (xdg-open) and `xclip`. Documented as required in module README;
 * absence returns `ShareResult.Failed(ShareError.NoHandler)` with a "is X installed?" hint.
 *
 * **Security:** all shell inputs single-quote-wrapped + embedded single quote escaped;
 * temp filenames are randomized + character-filtered (no shell metacharacters).
 *
 * **Temp file lifecycle:** files persist in `$TMPDIR`/`/tmp` until cleaned by the system
 * (`systemd-tmpfiles` default sweep). This is the platform-correct path — premature
 * deletion would race the launched GUI app's file read.
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
     * Materialize Image bytes to a /tmp file via POSIX `fopen`+`fwrite`, then `xdg-open`
     * — the registered image viewer / GUI app handles the share UX (Files → "Send to…",
     * Eye of GNOME → "Send via Email", etc.). Linux has no first-class share-sheet API;
     * delegating to the file's default-handler app is the platform-correct path.
     *
     * Cleanup: we leave the temp file in place. Linux distros' `systemd-tmpfiles` cleans
     * `/tmp` periodically (default: files unmodified for 10d are removed). The temp file
     * lives long enough for the launched app to read it. Documented behavior.
     *
     * 2026-06-01 — Replaces the v0.3 `Failed(UnsupportedPlatform)` stub
     * (per cmp-intent-share-coverage-trueup sub-plan 02 T2).
     */
    private fun imageShare(image: SharePayload.Image): ShareResult {
        val suffix = mimeToSuffix(image.mimeType)
        val basename = image.filename
            ?.substringBeforeLast('.', missingDelimiterValue = image.filename ?: "image")
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(64)
            ?: "image"
        val tmpdir = getenv("TMPDIR")?.toKString()?.takeIf { it.isNotEmpty() } ?: "/tmp"
        val rand = Random.nextLong().toULong().toString(16)
        val path = "$tmpdir/cmp-share-$basename-$rand$suffix"

        val file = fopen(path, "wb") ?: return ShareResult.Failed(
            ShareError.Unknown("fopen failed for $path (TMPDIR not writable?)"),
        )
        try {
            val written: ULong = image.bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1uL.convert(), image.bytes.size.convert(), file).convert()
            }
            if (written.toInt() != image.bytes.size) {
                return ShareResult.Failed(
                    ShareError.Unknown("fwrite short write: $written / ${image.bytes.size}"),
                )
            }
        } finally {
            fclose(file)
        }
        return xdgOpen(path)
    }

    /** Map common image MIME types to filename suffixes so xdg-open's content sniffer can route. */
    private fun mimeToSuffix(mime: String): String = when (mime.lowercase()) {
        "image/png" -> ".png"
        "image/jpeg", "image/jpg" -> ".jpg"
        "image/webp" -> ".webp"
        "image/gif" -> ".gif"
        "image/bmp" -> ".bmp"
        "image/tiff" -> ".tiff"
        "image/svg+xml" -> ".svg"
        else -> ".bin"
    }

    private fun multiShare(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r = when (item) {
                is SharePayload.Url -> xdgOpen(item.href)
                is SharePayload.Text -> xclipText(item.content)
                is SharePayload.File -> xdgOpen(uriFromFile(item.uri))
                is SharePayload.Image -> imageShare(item)
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
