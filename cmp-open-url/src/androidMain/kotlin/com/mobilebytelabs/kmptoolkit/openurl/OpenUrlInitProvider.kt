package com.mobilebytelabs.kmptoolkit.openurl

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * ContentProvider-based auto-initialiser for cmp-open-url.
 *
 * Merged into the host app's manifest automatically via manifest-merger.
 * Captures the application [android.content.Context] before any library call
 * is made, so callers never need to pass a context explicitly.
 *
 * This pattern mirrors [androidx.startup.InitializationProvider] but has no
 * extra dependency — the provider is the only artefact needed.
 */
internal class OpenUrlInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        OpenUrlContext.init(context!!.applicationContext)
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

/**
 * Singleton holding the application context captured by [OpenUrlInitProvider].
 */
internal object OpenUrlContext {
    private var _context: android.content.Context? = null

    fun init(ctx: android.content.Context) {
        _context = ctx
    }

    val context: android.content.Context
        get() = _context
            ?: error(
                "cmp-open-url: OpenUrlInitProvider was not initialised. " +
                    "Ensure the library is on the compile classpath so manifest-merger " +
                    "can include the provider declaration automatically.",
            )
}
