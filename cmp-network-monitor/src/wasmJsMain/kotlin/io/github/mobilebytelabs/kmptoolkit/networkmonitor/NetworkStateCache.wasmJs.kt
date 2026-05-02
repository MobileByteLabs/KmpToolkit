package io.github.mobilebytelabs.kmptoolkit.networkmonitor

internal actual fun loadCachedNetworkState(): CachedNetworkState? = null

internal actual fun saveCachedNetworkState(state: CachedNetworkState) {
    // Not supported in WasmJS
}
