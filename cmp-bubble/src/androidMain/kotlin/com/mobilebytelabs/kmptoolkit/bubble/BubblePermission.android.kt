package com.mobilebytelabs.kmptoolkit.bubble

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

internal class AndroidBubblePermission : BubblePermission {

    override fun canShowBubble(): Boolean {
        val context = appContext ?: return false
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Check if bubbles are allowed for this app
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.areBubblesAllowed() ?: true
            }

            else -> {
                // Overlay fallback
                com.mobilebytelabs.kmptoolkit.bubble.android.OverlayPermission.canDrawOverlays(context)
            }
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
        val context = appContext ?: return false
        return try {
            // Opens overlay settings — user can enable or disable
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            canShowBubble()
        } catch (e: Exception) {
            Log.w("KmpBubble", "Could not open overlay settings: ${e.message}")
            canShowBubble()
        }
    }

    override suspend fun requestNotificationPermission(): Boolean {
        val context = appContext ?: return false
        return try {
            // Opens notification settings — user can enable or disable
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            canShowNotification()
        } catch (e: Exception) {
            Log.w("KmpBubble", "Could not open notification settings: ${e.message}")
            canShowNotification()
        }
    }
}

actual fun createBubblePermission(): BubblePermission = AndroidBubblePermission()
