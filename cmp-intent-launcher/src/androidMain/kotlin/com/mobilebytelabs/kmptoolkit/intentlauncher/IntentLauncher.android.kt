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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CompletableDeferred

/**
 * Android `IntentLauncher` — wraps `rememberLauncherForActivityResult` via
 * `ActivityResultContracts.StartActivityForResult`. Lifecycle bound to the
 * enclosing Composable.
 *
 * For Activity-extension escape hatch (non-Compose Android callers), see
 * [intentLauncher] in [IntentLauncherActivityExtensions.kt].
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher internal constructor(
    private val launcher: ActivityResultLauncher<Intent>,
    private val pending: () -> CompletableDeferred<IntentResult>?,
    private val setPending: (CompletableDeferred<IntentResult>?) -> Unit,
) {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        return try {
            val intent = buildIntent(builder)
            val deferred = CompletableDeferred<IntentResult>()
            setPending(deferred)
            launcher.launch(intent)
            deferred.await()
        } catch (e: Exception) {
            IntentResult.Failed(IntentError.Unknown(e.message ?: "Android intent launch failed"))
        }
    }

    internal companion object {
        internal fun buildIntent(builder: IntentBuilder): Intent {
            val intent = Intent()
            builder.action?.let { intent.action = it }
            builder.data?.let { intent.data = Uri.parse(it) }
            builder.type?.let { intent.type = it }
            for (cat in builder.categories) intent.addCategory(cat)
            if (builder.flags != 0) intent.addFlags(builder.flags)
            builder.packageName?.let { intent.setPackage(it) }
            applyExtras(intent, builder.extras)
            return intent
        }

        internal fun applyExtras(intent: Intent, extras: Map<String, Any?>) {
            for ((key, value) in extras) {
                when (value) {
                    null -> { /* skip */ }
                    is String -> intent.putExtra(key, value)
                    is Int -> intent.putExtra(key, value)
                    is Long -> intent.putExtra(key, value)
                    is Boolean -> intent.putExtra(key, value)
                    is Float -> intent.putExtra(key, value)
                    is Double -> intent.putExtra(key, value)
                    is ByteArray -> intent.putExtra(key, value)
                    is Array<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        if (value.isArrayOf<String>()) intent.putExtra(key, value as Array<String>)
                    }
                    else -> { /* unsupported extra type — silently skip */ }
                }
            }
        }

        internal fun parseActivityResult(resultCode: Int, data: Intent?): IntentResult {
            if (resultCode == Activity.RESULT_CANCELED) return IntentResult.Cancelled
            val intentData = if (data != null) {
                IntentData(
                    uri = data.dataString,
                    mimeType = data.type,
                    extras = data.extras?.let { bundle ->
                        bundle.keySet().associateWith { k -> bundle.get(k) }
                    } ?: emptyMap(),
                )
            } else null
            return IntentResult.Ok(intentData)
        }
    }
}

@ExperimentalIntentLauncherApi
@Composable
public actual fun rememberIntentLauncher(): IntentLauncher {
    var pending: CompletableDeferred<IntentResult>? = null
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val parsed = IntentLauncher.parseActivityResult(result.resultCode, result.data)
        pending?.complete(parsed)
        pending = null
    }
    return remember(launcher) {
        IntentLauncher(
            launcher = launcher,
            pending = { pending },
            setPending = { pending = it },
        )
    }
}
