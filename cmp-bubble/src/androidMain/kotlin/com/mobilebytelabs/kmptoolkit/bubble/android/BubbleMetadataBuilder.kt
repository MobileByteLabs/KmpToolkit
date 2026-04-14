package com.mobilebytelabs.kmptoolkit.bubble.android

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Builds BubbleMetadata for Android 30+ notifications.
 */
internal object BubbleMetadataBuilder {

    @RequiresApi(Build.VERSION_CODES.R)
    fun build(
        context: Context,
        title: String,
        message: String,
        actions: String,
        desiredHeight: Int = 400,
        autoExpand: Boolean = true,
    ): NotificationCompat.BubbleMetadata {
        val intent = Intent(context, BubbleActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(BubbleActivity.EXTRA_TITLE, title)
            putExtra(BubbleActivity.EXTRA_MESSAGE, message)
            putExtra(BubbleActivity.EXTRA_ACTIONS, actions)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val icon = IconCompat.createWithResource(
            context,
            android.R.drawable.ic_popup_sync,
        )

        return NotificationCompat.BubbleMetadata.Builder(pendingIntent, icon)
            .setDesiredHeight(desiredHeight)
            .setAutoExpandBubble(autoExpand)
            .setSuppressNotification(false)
            .build()
    }
}
