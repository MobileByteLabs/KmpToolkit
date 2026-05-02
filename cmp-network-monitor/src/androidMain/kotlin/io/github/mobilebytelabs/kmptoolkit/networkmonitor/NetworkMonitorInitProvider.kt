package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Auto-initializes the library's application context at app startup.
 *
 * Declared in AndroidManifest.xml with `initOrder="99"`. This provider
 * does not serve any content — it exists solely to capture
 * [Context.getApplicationContext] before user code runs.
 *
 * If ContentProvider-based init is disabled (e.g., via tools:node="remove"),
 * call [setApplicationContext] manually from Application.onCreate().
 */
internal class NetworkMonitorInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.applicationContext?.let { appContext = it }
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

/** Application context captured by [NetworkMonitorInitProvider]. */
internal var appContext: Context? = null
    private set

/**
 * Manual fallback for setting the application context.
 * Call from `Application.onCreate()` if the ContentProvider is removed.
 */
fun setApplicationContext(context: Context) {
    appContext = context.applicationContext
}
