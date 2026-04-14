package com.mobilebytelabs.kmptoolkit.bubble.android

import android.content.Context
import android.content.Intent
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Creates and manages dynamic shortcuts required by the Android Bubbles API.
 */
internal object BubbleShortcut {

    private const val SHORTCUT_ID = "kmptoolkit_bubble_shortcut"

    fun getOrCreateShortcut(context: Context, title: String): String {
        val person = Person.Builder()
            .setName(title)
            .setImportant(true)
            .build()

        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setLongLived(true)
            .setShortLabel(title)
            .setPerson(person)
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_popup_sync))
            .setIntent(
                Intent(context, BubbleActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                },
            )
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        return SHORTCUT_ID
    }
}
