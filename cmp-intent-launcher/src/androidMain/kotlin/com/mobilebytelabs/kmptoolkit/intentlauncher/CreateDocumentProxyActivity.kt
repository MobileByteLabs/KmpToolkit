/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Invisible proxy Activity used by [SystemIntents.createDocument] so a SAF
 * `ACTION_CREATE_DOCUMENT` round-trip can complete without an in-Composable
 * `ActivityResultLauncher`. Lifecycle:
 *
 * 1. `SystemIntents.createDocument()` allocates a request id, registers a
 *    `CompletableDeferred<IntentResult>` in [pendingRequests], and starts this Activity
 *    with `FLAG_ACTIVITY_NEW_TASK` from Application Context (via
 *    `IntentLauncherContext.context`).
 * 2. `onCreate` registers a `CreateDocument(mimeType)` contract launcher, immediately
 *    launches it with the suggested filename.
 * 3. On callback, the captured URI (or null cancellation) is delivered to the deferred
 *    via [pendingRequests], the entry is removed, and `finish()` closes the proxy.
 *
 * Translucent theme + `excludeFromRecents` so the Activity never visibly appears.
 */
internal class CreateDocumentProxyActivity : ComponentActivity() {

    private var requestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getStringExtra(EXTRA_REQUEST_ID)
        val suggestedName = intent.getStringExtra(EXTRA_SUGGESTED_NAME).orEmpty()
        val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "*/*"

        if (id == null) {
            finish()
            return
        }
        requestId = id

        val launcher = registerForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
            val deferred = pendingRequests.remove(id)
            if (deferred != null) {
                val result = if (uri == null) {
                    IntentResult.Cancelled
                } else {
                    IntentResult.Ok(IntentData(uri = uri.toString(), mimeType = mimeType))
                }
                deferred.complete(result)
            }
            finish()
        }
        try {
            launcher.launch(suggestedName)
        } catch (e: Exception) {
            pendingRequests.remove(id)?.complete(
                IntentResult.Failed(IntentError.Unknown(e.message ?: "createDocument launch failed")),
            )
            finish()
        }
    }

    override fun onDestroy() {
        // If the Activity is killed mid-flight (config change abort, OOM, user backs out
        // without picking) ensure the deferred unblocks the caller's coroutine.
        requestId?.let { id ->
            pendingRequests.remove(id)?.complete(IntentResult.Cancelled)
        }
        super.onDestroy()
    }

    internal companion object {
        internal const val EXTRA_REQUEST_ID: String = "com.mobilebytelabs.kmptoolkit.intentlauncher.request_id"
        internal const val EXTRA_SUGGESTED_NAME: String = "com.mobilebytelabs.kmptoolkit.intentlauncher.suggested_name"
        internal const val EXTRA_MIME_TYPE: String = "com.mobilebytelabs.kmptoolkit.intentlauncher.mime_type"

        internal val pendingRequests: ConcurrentHashMap<String, CompletableDeferred<IntentResult>> = ConcurrentHashMap()

        internal fun newRequestId(): String = UUID.randomUUID().toString()

        internal fun buildLaunchIntent(
            applicationContext: android.content.Context,
            requestId: String,
            suggestedName: String,
            mimeType: String,
        ): Intent = Intent(applicationContext, CreateDocumentProxyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_SUGGESTED_NAME, suggestedName)
            putExtra(EXTRA_MIME_TYPE, mimeType)
        }
    }
}
