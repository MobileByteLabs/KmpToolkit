package com.mobilebytelabs.kmptoolkit.bubble.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal object OverlayPermission {

    fun canDrawOverlays(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    fun createPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
}
