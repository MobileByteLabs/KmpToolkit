package com.mobilebytelabs.kmptoolkit.clipboard.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Handles SYSTEM_ALERT_WINDOW permission for floating overlay on Android.
 */
internal object OverlayPermission {

    /**
     * Checks if the app has overlay permission.
     *
     * @param context Application context.
     * @return true if overlay drawing is permitted, false otherwise.
     */
    fun canDrawOverlays(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true // Pre-M doesn't need this permission
    }

    /**
     * Creates an Intent to open the overlay permission settings for this app.
     *
     * @param context Application context.
     * @return Intent that opens the overlay settings screen.
     */
    fun createPermissionIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
