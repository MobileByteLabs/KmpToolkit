package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import java.io.File

private val cacheFile = File(System.getProperty("user.home"), ".cmp-network-monitor-cache")

internal actual fun loadCachedNetworkState(): CachedNetworkState? = try {
    if (!cacheFile.exists()) return null
    val lines = cacheFile.readLines()
    if (lines.size < 4) return null
    CachedNetworkState(
        wasOnline = lines[0].toBooleanStrictOrNull() ?: return null,
        networkTypeName = lines[1],
        isMetered = lines[2].toBooleanStrictOrNull() ?: return null,
        timestampMs = lines[3].toLongOrNull() ?: return null,
    )
} catch (_: Exception) {
    null
}

internal actual fun saveCachedNetworkState(state: CachedNetworkState) {
    try {
        cacheFile.writeText("${state.wasOnline}\n${state.networkTypeName}\n${state.isMetered}\n${state.timestampMs}")
    } catch (_: Exception) {
        // Best-effort — don't crash on write failure
    }
}
