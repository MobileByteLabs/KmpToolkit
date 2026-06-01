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
 * - File → caller-provided URI → grant `FLAG_GRANT_READ_URI_PERMISSION` + `EXTRA_STREAM`
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
            val intent = buildIntent(payload, options)
            val chooser = Intent.createChooser(intent, options.chooserTitle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(chooser)
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
            putExtra(Intent.EXTRA_STREAM, Uri.parse(payload.uri))
        }

        is SharePayload.Multi -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            val uris = ArrayList<Uri>()
            val texts = ArrayList<String>()
            for (item in payload.items) {
                when (item) {
                    is SharePayload.Text -> texts.add(item.content)
                    is SharePayload.Url -> texts.add(item.href)
                    is SharePayload.Image -> uris.add(writeBytesToCache(item.bytes, item.mimeType, item.filename))
                    is SharePayload.File -> uris.add(Uri.parse(item.uri))
                    is SharePayload.Multi -> { /* skip nested Multi — flatten was done by caller */ }
                }
            }
            type = if (uris.isNotEmpty()) "*/*" else "text/plain"
            if (uris.isNotEmpty()) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (texts.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, texts.joinToString("\n"))
        }
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
