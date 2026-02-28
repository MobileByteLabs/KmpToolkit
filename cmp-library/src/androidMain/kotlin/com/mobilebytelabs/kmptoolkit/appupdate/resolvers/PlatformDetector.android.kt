package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * Android implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "android"
}
