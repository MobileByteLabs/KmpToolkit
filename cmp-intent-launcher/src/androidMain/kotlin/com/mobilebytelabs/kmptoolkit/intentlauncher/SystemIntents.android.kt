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
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CompletableDeferred

/**
 * Android `SystemIntents` actual.
 *
 * - `openAppSettings()` — `ACTION_APPLICATION_DETAILS_SETTINGS` with `FLAG_ACTIVITY_NEW_TASK`
 *   (no Activity needed; context auto-injected by [IntentLauncherInitProvider]).
 * - `createDocument()` — starts [CreateDocumentProxyActivity], awaits its
 *   `CompletableDeferred<IntentResult>` for the SAF callback.
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult {
        if (!IntentLauncherContext.isInitialized()) {
            return IntentResult.Failed(
                IntentError.Unknown(
                    "IntentLauncherContext not initialized — IntentLauncherInitProvider must be declared in AndroidManifest.xml",
                ),
            )
        }
        val ctx = IntentLauncherContext.context
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            IntentResult.Ok(IntentData(uri = intent.data?.toString()))
        } catch (e: Exception) {
            IntentResult.Failed(IntentError.Unknown(e.message ?: "Unknown Android settings error"))
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
        if (!IntentLauncherContext.isInitialized()) {
            return IntentResult.Failed(
                IntentError.Unknown(
                    "IntentLauncherContext not initialized — IntentLauncherInitProvider must be declared in AndroidManifest.xml",
                ),
            )
        }
        val ctx = IntentLauncherContext.context
        val requestId = CreateDocumentProxyActivity.newRequestId()
        val deferred = CompletableDeferred<IntentResult>()
        CreateDocumentProxyActivity.pendingRequests[requestId] = deferred

        return try {
            ctx.startActivity(
                CreateDocumentProxyActivity.buildLaunchIntent(ctx, requestId, suggestedName, mimeType),
            )
            deferred.await()
        } catch (e: Exception) {
            CreateDocumentProxyActivity.pendingRequests.remove(requestId)
            IntentResult.Failed(IntentError.Unknown(e.message ?: "createDocument dispatch failed"))
        }
    }
}
