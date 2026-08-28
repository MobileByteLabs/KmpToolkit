package com.mobilebytelabs.remoteconfig

import com.mobilebytelabs.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytelabs.remoteconfig.model.DeviceImpression
import com.mobilebytelabs.remoteconfig.model.RemoteConfig
import io.ktor.util.date.GMTDate

class RemoteConfigEvaluator(
    private val localStore: RemoteConfigLocalStore,
    /**
     * Lazy fallback source for the host app version, wired from `remoteConfig { appVersion = … }`.
     * Used when a caller of [evaluate] doesn't pass an explicit `appVersion`. Invoked here (not at
     * DI-build time) so a platform version source that inits after Koin is ready by evaluate time.
     */
    private val appVersionProvider: (() -> String?)? = null,
) {
    fun evaluate(
        configs: List<RemoteConfig>,
        serverImpressions: Map<String, DeviceImpression> = emptyMap(),
        currentTimeMs: Long = GMTDate().timestamp,
        appVersion: String? = null,
    ): RemoteConfig? {
        val effectiveVersion = appVersion ?: appVersionProvider?.invoke()
        return configs
            .filter { it.isEnabled }
            .filter { passesDateRange(it, currentTimeMs) }
            .filter { passesVersionRange(it, effectiveVersion) }
            .filter { passesImpressionLimit(it, serverImpressions) }
            .filter { !isDismissed(it, serverImpressions) }
            .filter { passesCooldown(it, currentTimeMs) }
            .sortedByDescending { it.priority }
            .firstOrNull()
    }

    /**
     * min_app_version / max_app_version gate — both INCLUSIVE, both optional. Activates the
     * (previously dormant) [RemoteConfig.minAppVersion] / [RemoteConfig.maxAppVersion] columns as a
     * real audience filter so a server row can target ONLY a version window:
     *
     * - `min_app_version = "2.0.0"` → shown only to builds >= 2.0.0.
     * - `max_app_version = "2026.7.99"` → shown only to builds <= 2026.7.99 (the canonical
     *   "please update" shape: nag everything BELOW 2026.8.0, and stop the moment the user is on a
     *   supported build).
     *
     * Behavior by case:
     * - Config has NO version bounds → always passes (every existing version-unaware config keeps
     *   today's behavior; a blank bound is treated as absent).
     * - Config HAS a bound but [appVersion] is unknown (null/blank — e.g. a platform that doesn't
     *   report a Play-Store version) → FAIL-CLOSED (excluded). A version-TARGETED row must never
     *   show where we can't confirm the device is inside the window, so a Play-Store update gate
     *   never leaks onto desktop/web/iOS.
     * - Config has a bound and [appVersion] is known → windowed compare (see [VersionCompare]).
     */
    private fun passesVersionRange(config: RemoteConfig, appVersion: String?): Boolean {
        val min = config.minAppVersion?.takeIf { it.isNotBlank() }
        val max = config.maxAppVersion?.takeIf { it.isNotBlank() }
        if (min == null && max == null) return true
        if (appVersion.isNullOrBlank()) return false
        min?.let { if (VersionCompare.compare(appVersion, it) < 0) return false }
        max?.let { if (VersionCompare.compare(appVersion, it) > 0) return false }
        return true
    }

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
