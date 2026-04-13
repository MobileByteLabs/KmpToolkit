package com.mobilebytesensei.remoteconfig

import com.mobilebytesensei.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytesensei.remoteconfig.model.DeviceImpression
import com.mobilebytesensei.remoteconfig.model.RemoteConfig
import io.ktor.util.date.GMTDate

class RemoteConfigEvaluator(private val localStore: RemoteConfigLocalStore) {
    fun evaluate(
        configs: List<RemoteConfig>,
        serverImpressions: Map<String, DeviceImpression> = emptyMap(),
        currentTimeMs: Long = GMTDate().timestamp,
    ): RemoteConfig? = configs
        .filter { it.isEnabled }
        .filter { passesDateRange(it, currentTimeMs) }
        .filter { passesImpressionLimit(it, serverImpressions) }
        .filter { !isDismissed(it, serverImpressions) }
        .filter { passesCooldown(it, currentTimeMs) }
        .sortedByDescending { it.priority }
        .firstOrNull()

    private fun passesDateRange(config: RemoteConfig, currentTimeMs: Long): Boolean {
        val startMs = config.startAt?.parseIsoToMs() ?: 0L
        val endMs = config.endAt?.parseIsoToMs()
        if (startMs > 0 && currentTimeMs < startMs) return false
        if (endMs != null && endMs > 0 && currentTimeMs > endMs) return false
        return true
    }

    private fun passesImpressionLimit(
        config: RemoteConfig,
        serverImpressions: Map<String, DeviceImpression>,
    ): Boolean {
        if (config.maxImpressions == 0) return true
        val count = serverImpressions[config.id]?.impressions
            ?: localStore.getImpressions(config.id)
        return count < config.maxImpressions
    }

    private fun isDismissed(config: RemoteConfig, serverImpressions: Map<String, DeviceImpression>): Boolean =
        (serverImpressions[config.id]?.dismissed ?: false) ||
            localStore.isDismissed(config.id)

    private fun passesCooldown(config: RemoteConfig, currentTimeMs: Long): Boolean {
        if (config.cooldownHours == 0) return true
        return localStore.getHoursSinceLastShown(config.id, currentTimeMs) >= config.cooldownHours
    }
}

private fun String.parseIsoToMs(): Long? {
    return try {
        val cleaned = this.replace("Z", "+00:00").substringBefore("+").substringBefore(".")
        val parts = cleaned.split("T")
        if (parts.size != 2) return null
        val d = parts[0].split("-")
        val t = parts[1].split(":")
        if (d.size != 3 || t.size < 2) return null
        val year = d[0].toInt()
        val month = d[1].toInt()
        val day = d[2].toInt()
        val hour = t[0].toInt()
        val min = t[1].toInt()
        val sec = t.getOrNull(2)?.toIntOrNull() ?: 0
        val md = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val mDays = if (month in 1..12) md[month - 1] else 0
        val leap = if (month > 2 && year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 1 else 0
        val daysSinceEpoch = (year - 1970) * 365L + (year - 1969) / 4 + mDays + leap + day - 1
        (daysSinceEpoch * 86400 + hour * 3600 + min * 60 + sec) * 1000L
    } catch (_: Exception) {
        null
    }
}
