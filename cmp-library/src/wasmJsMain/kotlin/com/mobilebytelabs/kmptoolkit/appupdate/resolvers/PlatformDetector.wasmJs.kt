package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * WasmJs implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "wasmjs"
}
