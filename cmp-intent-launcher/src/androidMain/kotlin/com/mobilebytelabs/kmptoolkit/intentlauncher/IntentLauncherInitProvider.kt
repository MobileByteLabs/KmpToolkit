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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Zero-config Application context auto-init for [SystemIntents] on Android.
 *
 * Android boots `ContentProvider`s before `Application.onCreate`, so by the time any
 * consumer calls [SystemIntents.openAppSettings] the context is captured.
 *
 * Declared in `AndroidManifest.xml` with `android:exported="false"`.
 * Pattern mirrors `cmp-share/ShareInitProvider.kt`.
 */
internal class IntentLauncherInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        IntentLauncherContext.context = ctx.applicationContext
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}
