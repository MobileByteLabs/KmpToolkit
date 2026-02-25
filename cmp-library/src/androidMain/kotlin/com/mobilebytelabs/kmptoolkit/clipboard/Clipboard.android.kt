package com.mobilebytelabs.kmptoolkit.clipboard

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build

/**
 * Android implementation of clipboard operations using ClipboardManager.
 *
 * This implementation automatically initializes itself using a ContentProvider,
 * so users can call clipboard functions directly without any setup.
 *
 * ## Zero Configuration
 *
 * No initialization required! Simply use the functions:
 *
 * ```kotlin
 * // Just works - no setup needed
 * copyToClipboard("Hello, World!")
 * val text = getFromClipboard()
 * ```
 *
 * ## How It Works
 *
 * The library uses a ContentProvider to automatically obtain the application
 * context when the app starts. This is a common pattern used by Firebase,
 * WorkManager, and other Android libraries.
 */

/**
 * Application context obtained automatically via ContentProvider.
 * This is set before any user code runs.
 */
@SuppressLint("StaticFieldLeak")
private var appContext: Context? = null

/**
 * Internal function to set the application context.
 * Called automatically by [ClipboardInitProvider].
 */
internal fun setApplicationContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * ContentProvider that auto-initializes the clipboard module.
 *
 * This provider is registered in the AndroidManifest and runs before
 * any application code, ensuring the context is always available.
 *
 * Users don't need to interact with this class - it's purely internal.
 */
class ClipboardInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { setApplicationContext(it) }
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

/**
 * Gets the ClipboardManager service.
 * @return ClipboardManager or null if context not available.
 */
private fun getClipboardManager(): ClipboardManager? {
    val context = appContext ?: return null
    return context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
}

actual fun copyToClipboard(text: String): Boolean {
    val clipboard = getClipboardManager() ?: return false
    return try {
        val clip = ClipData.newPlainText("KmpToolkit", text)
        clipboard.setPrimaryClip(clip)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun getFromClipboard(): String? {
    val clipboard = getClipboardManager() ?: return null
    return try {
        clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    } catch (e: Exception) {
        null
    }
}

actual fun hasClipboardText(): Boolean {
    val clipboard = getClipboardManager() ?: return false
    return try {
        clipboard.hasPrimaryClip() &&
            clipboard.primaryClipDescription?.hasMimeType("text/*") == true
    } catch (e: Exception) {
        false
    }
}

actual fun clearClipboard() {
    val clipboard = getClipboardManager() ?: return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            // For older Android versions, set empty clip
            val emptyClip = ClipData.newPlainText("", "")
            clipboard.setPrimaryClip(emptyClip)
        }
    } catch (e: Exception) {
        // Silently ignore errors
    }
}
