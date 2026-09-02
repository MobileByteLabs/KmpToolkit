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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.FileOutputStream
import java.io.File as JavaFile

/**
 * Android `Intent.ACTION_SEND` implementation.
 *
 * - Text / Url → `EXTRA_TEXT` (URL shares as text on Android)
 * - Image → write bytes to module cache dir + `FileProvider` URI + `EXTRA_STREAM`
 * - File → caller URI resolved to a shareable one (see [Share.shareableUri]: `content://` passes
 *   through; `file://` is FileProvider-wrapped, staged via the module cache dir when it sits
 *   outside the declared provider paths) → grant `FLAG_GRANT_READ_URI_PERMISSION` + `EXTRA_STREAM`
 * - Multi → `ACTION_SEND_MULTIPLE` + array of URIs (text-only items get flattened to a join)
 *
 * Wrapped in `Intent.createChooser`. Note that `startActivity` is fire-and-forget;
 * Android doesn't surface "user picked + completed" — we return [ShareResult.Completed]
 * immediately after the chooser is started. (Real completion detection would require
 * `IntentSender` callbacks added in API 22; deferred to a future enhancement.)
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        if (!ShareContext.isInitialized()) {
            return ShareResult.Failed(
                ShareError.Unknown(
                    "ShareContext not initialized — ShareInitProvider must be declared in AndroidManifest.xml",
                ),
            )
        }
        val ctx = ShareContext.context

        return try {
            fun Intent.withShareFlags() = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(buildIntent(payload, options), options.chooserTitle)
                .withShareFlags()

            val target = options.targetPackage?.takeIf { it.isNotBlank() }
            if (target != null) {
                // Direct-to-app: route the payload straight to the target package (e.g. WhatsApp,
                // Instagram) with no chooser. If that package isn't installed / can't handle the
                // payload — or isn't visible under Android 11+ package-visibility — fall back to the
                // normal chooser instead of failing.
                val direct = buildIntent(payload, options).apply { `package` = target }.withShareFlags()
                try {
                    ctx.startActivity(direct)
                } catch (e: ActivityNotFoundException) {
                    ctx.startActivity(chooser)
                }
            } else {
                ctx.startActivity(chooser)
            }
            ShareResult.Completed
        } catch (e: Exception) {
            ShareResult.Failed(ShareError.Unknown(e.message ?: "Unknown Android share error"))
        }
    }

    private fun buildIntent(payload: SharePayload, options: ShareOptions): Intent = when (payload) {
        is SharePayload.Text -> Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
            putExtra(Intent.EXTRA_TEXT, payload.content)
        }

        is SharePayload.Url -> Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, payload.href)
        }

        is SharePayload.Image -> Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
            val uri = writeBytesToCache(payload.bytes, payload.mimeType, payload.filename)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        is SharePayload.File -> Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
            putExtra(Intent.EXTRA_STREAM, shareableUri(payload.uri))
        }

        is SharePayload.Multi -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            val uris = ArrayList<Uri>()
            val texts = ArrayList<String>()
            for (item in payload.items) {
                when (item) {
                    is SharePayload.Text -> texts.add(item.content)
                    is SharePayload.Url -> texts.add(item.href)
                    is SharePayload.Image -> uris.add(writeBytesToCache(item.bytes, item.mimeType, item.filename))
                    is SharePayload.File -> uris.add(shareableUri(item.uri))
                    is SharePayload.Multi -> { /* skip nested Multi — flatten was done by caller */ }
                }
            }
            type = if (uris.isNotEmpty()) "*/*" else "text/plain"
            if (uris.isNotEmpty()) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (texts.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, texts.joinToString("\n"))
        }
    }

    /**
     * Resolve a caller-supplied file URI into one another app can actually read.
     *
     * `ACTION_SEND` rejects a `file://` URI on Android 7+ (`FileUriExposedException`), so passing
     * the caller's string through verbatim made every `file://` share fail. Because [share] wraps
     * everything in `try/catch → ShareResult.Failed`, a caller that ignores the returned
     * [ShareResult] saw the share button simply do nothing — a silent, hard-to-diagnose failure.
     *
     * Resolution order:
     *  1. Anything already non-`file` (`content://`, `http(s)://`, …) is returned untouched — the
     *     previous behaviour for the payloads that already worked.
     *  2. A `file://` URI (or a bare filesystem path) is wrapped through this module's own
     *     FileProvider, which yields a `content://` URI. Combined with the
     *     `FLAG_GRANT_READ_URI_PERMISSION` already set in [share], the receiving app gets scoped,
     *     per-URI read access — no storage permission is granted to it.
     *  3. If the file lives outside the paths declared in `cmp_share_paths.xml`,
     *     `getUriForFile` throws `IllegalArgumentException`; we then COPY it into this module's own
     *     cache dir (already declared, and the same place image shares use) and wrap that. Costs a
     *     copy, but shares a file the consumer app can read from anywhere without every consumer
     *     having to widen its own FileProvider paths.
     *  4. Only if all of that fails do we fall back to the original URI, so behaviour is never
     *     worse than before.
     */
    private fun shareableUri(uriString: String): Uri {
        val original = Uri.parse(uriString)
        // content:// / http(s):// / anything already shareable — leave exactly as-is.
        if (original.scheme != null && original.scheme != "file") return original

        val ctx = ShareContext.context
        val file = original.path?.let { JavaFile(it) } ?: return original
        if (!file.exists()) return original

        val authority = "${ctx.packageName}.cmp-share.fileprovider"
        runCatching { return FileProvider.getUriForFile(ctx, authority, file) }

        // Outside the declared provider paths — stage a copy in our own cache dir and share that.
        return runCatching {
            val staged = JavaFile(ctx.cacheDir, "cmp-share/${file.name}").apply { parentFile?.mkdirs() }
            file.inputStream().use { input -> FileOutputStream(staged).use { input.copyTo(it) } }
            FileProvider.getUriForFile(ctx, authority, staged)
        }.getOrDefault(original)
    }

    private fun writeBytesToCache(bytes: ByteArray, mimeType: String, filename: String?): Uri {
        val ctx = ShareContext.context
        val name = filename ?: "share_${System.currentTimeMillis()}.${guessExtension(mimeType)}"
        val outFile = JavaFile(ctx.cacheDir, "cmp-share/$name").apply {
            parentFile?.mkdirs()
        }
        FileOutputStream(outFile).use { it.write(bytes) }
        val authority = "${ctx.packageName}.cmp-share.fileprovider"
        return FileProvider.getUriForFile(ctx, authority, outFile)
    }

    private fun guessExtension(mimeType: String): String = when {
        mimeType.endsWith("/png") -> "png"
        mimeType.endsWith("/jpeg") || mimeType.endsWith("/jpg") -> "jpg"
        mimeType.endsWith("/webp") -> "webp"
        mimeType.endsWith("/gif") -> "gif"
        mimeType.endsWith("/pdf") -> "pdf"
        else -> "bin"
    }
}
