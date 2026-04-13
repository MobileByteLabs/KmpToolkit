package com.mobilebytelabs.kmptoolkit.clipboard.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build

/**
 * BroadcastReceiver that restarts the clipboard monitor service on device boot.
 *
 * Only restarts if the user previously had the monitor running (tracked via SharedPreferences).
 * The consumer app must opt-in by calling [ClipboardBootReceiver.setAutoStartEnabled].
 */
class ClipboardBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = getPrefs(context)
        val autoStart = prefs.getBoolean(KEY_AUTO_START, false)
        val wasRunning = prefs.getBoolean(KEY_WAS_RUNNING, false)

        if (autoStart && wasRunning) {
            val serviceIntent = Intent(context, ClipboardMonitorService::class.java).apply {
                action = ClipboardMonitorService.ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                // May fail if battery optimization kills it
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "kmptoolkit_clipboard_monitor"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_WAS_RUNNING = "was_running"

        internal fun getPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Enable or disable auto-start on boot. */
        fun setAutoStartEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()
        }

        /** Track whether the service was running (called by service start/stop). */
        internal fun setWasRunning(context: Context, running: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_WAS_RUNNING, running).apply()
        }
    }
}
