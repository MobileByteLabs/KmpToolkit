package com.mobilebytelabs.kmptoolkit.bubble

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

internal class AndroidBubblePermission : BubblePermission {

    override fun canShowBubble(): Boolean {
        val context = appContext ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Bubbles API — auto-managed by system, no special permission
            true
        } else {
            // Overlay fallback
            Settings.canDrawOverlays(context)
        }
    }

    override fun canShowNotification(): Boolean {
        val context = appContext ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override suspend fun requestBubblePermission(): Boolean {
        if (canShowBubble()) return true
        val context = appContext ?: return false
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (e: Exception) {
            return false
        }
        return false // User must grant in settings
    }

    override suspend fun requestNotificationPermission(): Boolean = canShowNotification()
}

actual fun createBubblePermission(): BubblePermission = AndroidBubblePermission()
