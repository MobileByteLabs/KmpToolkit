package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import platform.Foundation.NSUserDefaults

private const val SUITE = "io.github.mobilebytelabs.kmptoolkit.networkmonitor"
private const val KEY_WAS_ONLINE = "was_online"
private const val KEY_NETWORK_TYPE = "network_type"
private const val KEY_IS_METERED = "is_metered"
private const val KEY_TIMESTAMP = "timestamp"

private fun defaults(): NSUserDefaults? = NSUserDefaults(suiteName = SUITE)

internal actual fun loadCachedNetworkState(): CachedNetworkState? = try {
    val ud = defaults() ?: return null
    val typeName = ud.stringForKey(KEY_NETWORK_TYPE) ?: return null
    CachedNetworkState(
        wasOnline = ud.boolForKey(KEY_WAS_ONLINE),
        networkTypeName = typeName,
        isMetered = ud.boolForKey(KEY_IS_METERED),
        timestampMs = ud.doubleForKey(KEY_TIMESTAMP).toLong(),
    )
} catch (_: Exception) {
    null
}

internal actual fun saveCachedNetworkState(state: CachedNetworkState) {
    try {
        val ud = defaults() ?: return
        ud.setBool(state.wasOnline, KEY_WAS_ONLINE)
        ud.setObject(state.networkTypeName, KEY_NETWORK_TYPE)
        ud.setBool(state.isMetered, KEY_IS_METERED)
        ud.setDouble(state.timestampMs.toDouble(), KEY_TIMESTAMP)
    } catch (_: Exception) {
        // Best-effort
    }
}
