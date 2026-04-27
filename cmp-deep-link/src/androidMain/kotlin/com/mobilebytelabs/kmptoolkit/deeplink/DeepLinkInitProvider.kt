package com.mobilebytelabs.kmptoolkit.deeplink

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * ContentProvider auto-init: registers [DeepLinkLifecycleObserver] on the application's
 * activity lifecycle without requiring any manual setup from the consumer.
 *
 * Declared in the library's AndroidManifest.xml with `android:exported="false"`.
 * Merged automatically into the consumer app manifest via manifest merger.
 *
 * The same ContentProvider auto-init pattern is used by cmp-clipboard and Jetpack libraries.
 */
internal class DeepLinkInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? android.app.Application ?: return false
        app.registerActivityLifecycleCallbacks(DeepLinkActivityCallbacks())
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
