package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * tvOS implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "tvos"
}
