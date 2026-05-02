package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "cmp_network_monitor_cache"
private const val KEY_WAS_ONLINE = "was_online"
private const val KEY_NETWORK_TYPE = "network_type"
private const val KEY_IS_METERED = "is_metered"
private const val KEY_TIMESTAMP = "timestamp"

/**
 * Application context holder — set by [NetworkMonitorInitProvider] on process start.
 */
internal var applicationContextHolder: Context? = null

private fun getPrefs(): SharedPreferences? =
    applicationContextHolder?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

internal actual fun loadCachedNetworkState(): CachedNetworkState? = try {
    val prefs = getPrefs() ?: return null
    if (!prefs.contains(KEY_WAS_ONLINE)) return null
    CachedNetworkState(
        wasOnline = prefs.getBoolean(KEY_WAS_ONLINE, false),
        networkTypeName = prefs.getString(KEY_NETWORK_TYPE, "Unknown") ?: "Unknown",
        isMetered = prefs.getBoolean(KEY_IS_METERED, false),
        timestampMs = prefs.getLong(KEY_TIMESTAMP, 0L),
    )
} catch (_: Exception) {
    null
}

@SuppressLint("ApplySharedPref")
internal actual fun saveCachedNetworkState(state: CachedNetworkState) {
    try {
        getPrefs()?.edit()
            ?.putBoolean(KEY_WAS_ONLINE, state.wasOnline)
            ?.putString(KEY_NETWORK_TYPE, state.networkTypeName)
            ?.putBoolean(KEY_IS_METERED, state.isMetered)
            ?.putLong(KEY_TIMESTAMP, state.timestampMs)
            ?.apply()
    } catch (_: Exception) {
        // Best-effort
    }
}
