package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * WasmWasi implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "wasmwasi"
}
