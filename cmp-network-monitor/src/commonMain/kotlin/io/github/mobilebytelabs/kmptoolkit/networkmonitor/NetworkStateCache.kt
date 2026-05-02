package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Persists last-known network state to disk for cold-start optimization.
 *
 * When a monitor starts, it can read the cached state to provide an
 * immediate best-guess before the platform APIs respond. This eliminates
 * the brief "unknown" state on app cold-start.
 *
 * Platform implementations:
 * - Android: SharedPreferences
 * - Apple: UserDefaults
 * - JVM/Linux/MinGW: File-based (~/.cmp-network-monitor-cache)
 * - JS/WasmJS/WasmWASI: Not supported (returns null)
 */
internal expect fun loadCachedNetworkState(): CachedNetworkState?

internal expect fun saveCachedNetworkState(state: CachedNetworkState)

/**
 * Minimal serializable snapshot of last-known network state.
 */
internal data class CachedNetworkState(
    val wasOnline: Boolean,
    val networkTypeName: String,
    val isMetered: Boolean,
    val timestampMs: Long,
) {
    fun toNetworkInfo(): NetworkInfo = NetworkInfo(
        type = NetworkType.entries.firstOrNull { it.name == networkTypeName } ?: NetworkType.Unknown,
        isMetered = isMetered,
    )

    companion object {
        fun from(status: NetworkStatus): CachedNetworkState? = when (status) {
            is NetworkStatus.Available -> CachedNetworkState(
                wasOnline = true,
                networkTypeName = status.info.type.name,
                isMetered = status.info.isMetered,
                timestampMs = currentTimeMillis(),
            )

            is NetworkStatus.CaptivePortal -> CachedNetworkState(
                wasOnline = false,
                networkTypeName = status.info.type.name,
                isMetered = status.info.isMetered,
                timestampMs = currentTimeMillis(),
            )

            is NetworkStatus.Unavailable -> CachedNetworkState(
                wasOnline = false,
                networkTypeName = NetworkType.Unknown.name,
                isMetered = false,
                timestampMs = currentTimeMillis(),
            )
        }

        /** Cache is stale after 5 minutes. */
        internal const val MAX_AGE_MS = 5 * 60 * 1000L
    }
}
