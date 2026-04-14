package com.mobilebytelabs.kmptoolkit.clipboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.OverlayPermission

/**
 * Android implementation of ClipboardPermission.
 *
 * Checks runtime permissions for clipboard monitoring features:
 * - Clipboard access: Always granted on Android
 * - Overlay permission: SYSTEM_ALERT_WINDOW (requires Settings intent)
 * - Notification permission: POST_NOTIFICATIONS (Android 13+)
 */
internal class AndroidClipboardPermission : ClipboardPermission {

    override fun hasClipboardAccess(): Boolean = true

    override fun hasOverlayPermission(): Boolean {
        val context = appContext ?: return false
        return OverlayPermission.canDrawOverlays(context)
    }

    override fun hasNotificationPermission(): Boolean {
        val context = appContext ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-13 doesn't need runtime permission
        }
    }

    override suspend fun requestClipboardAccess(): Boolean = true

    override suspend fun requestOverlayPermission(): Boolean {
        val context = appContext ?: return false
        if (hasOverlayPermission()) return true
        // Launch settings intent — consumer app should handle the result
        try {
            val intent = OverlayPermission.createPermissionIntent(context)
            context.startActivity(intent)
        } catch (e: Exception) {
            return false
        }
        return false // User needs to grant in settings
    }

    override suspend fun requestNotificationPermission(): Boolean {
        // Consumer app must request this via ActivityResultLauncher
        // We can only check the current state
        return hasNotificationPermission()
    }
}

actual fun createClipboardPermission(): ClipboardPermission = AndroidClipboardPermission()
