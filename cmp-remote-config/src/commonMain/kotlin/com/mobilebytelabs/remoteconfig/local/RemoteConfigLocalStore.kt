package com.mobilebytelabs.remoteconfig.local

import com.russhwolf.settings.Settings

class RemoteConfigLocalStore(private val settings: Settings = Settings()) {
    fun getImpressions(configId: String): Int = settings.getInt("rc_${configId}_impressions", 0)

    fun incrementImpressions(configId: String, currentTimeMs: Long) {
        settings.putInt("rc_${configId}_impressions", getImpressions(configId) + 1)
        settings.putLong("rc_${configId}_last_shown", currentTimeMs)
    }

    fun getLastShownMs(configId: String): Long = settings.getLong("rc_${configId}_last_shown", 0L)

    fun getHoursSinceLastShown(configId: String, currentTimeMs: Long): Int {
        val lastShown = getLastShownMs(configId)
        if (lastShown == 0L) return Int.MAX_VALUE
        return ((currentTimeMs - lastShown) / (1000 * 60 * 60)).toInt()
    }

    fun isDismissed(configId: String): Boolean = settings.getBoolean("rc_${configId}_dismissed", false)

    fun markDismissed(configId: String) {
        settings.putBoolean("rc_${configId}_dismissed", true)
    }

    fun reset(configId: String) {
        settings.remove("rc_${configId}_impressions")
        settings.remove("rc_${configId}_last_shown")
        settings.remove("rc_${configId}_dismissed")
    }
}
