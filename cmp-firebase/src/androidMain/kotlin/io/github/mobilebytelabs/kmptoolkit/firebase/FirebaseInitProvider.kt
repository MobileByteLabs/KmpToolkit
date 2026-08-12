/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Auto-runs [FirebaseKit.initialize] at app startup so Android consumers need
 * **zero setup code** — the "setup stays in the library" contract for Android.
 *
 * Declared in `AndroidManifest.xml` and merged into the consuming app. It serves
 * no content; it exists solely to enable Crashlytics collection once Firebase's
 * own auto-init (from `google-services.json`) has run. Init is wrapped so a
 * not-yet-configured Firebase can never crash app startup.
 *
 * To opt out, remove it from the merged manifest with
 * `tools:node="remove"` and call [FirebaseKit.initialize] yourself.
 */
internal class FirebaseInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        runCatching { FirebaseKit.initialize() }
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
