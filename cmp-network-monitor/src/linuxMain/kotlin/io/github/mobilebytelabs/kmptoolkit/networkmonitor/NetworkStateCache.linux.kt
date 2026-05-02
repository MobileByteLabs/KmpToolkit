@file:OptIn(ExperimentalForeignApi::class)

package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv

private fun cacheFilePath(): String? {
    val home = getenv("HOME")?.toKString() ?: return null
    return "$home/.cmp-network-monitor-cache"
}

internal actual fun loadCachedNetworkState(): CachedNetworkState? = try {
    val path = cacheFilePath() ?: return null
    val file = fopen(path, "r") ?: return null
    try {
        memScoped {
            val buf = allocArray<ByteVar>(512)
            val content = fgets(buf, 512, file)?.toKString() ?: return null
            val lines = content.split("\n")
            if (lines.size < 4) return null
            CachedNetworkState(
                wasOnline = lines[0].toBooleanStrictOrNull() ?: return null,
                networkTypeName = lines[1],
                isMetered = lines[2].toBooleanStrictOrNull() ?: return null,
                timestampMs = lines[3].toLongOrNull() ?: return null,
            )
        }
    } finally {
        fclose(file)
    }
} catch (_: Exception) {
    null
}

internal actual fun saveCachedNetworkState(state: CachedNetworkState) {
    try {
        val path = cacheFilePath() ?: return
        val file = fopen(path, "w") ?: return
        fputs("${state.wasOnline}\n${state.networkTypeName}\n${state.isMetered}\n${state.timestampMs}", file)
        fclose(file)
    } catch (_: Exception) {
        // Best-effort
    }
}
