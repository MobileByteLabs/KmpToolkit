package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * JVM implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> "windows"
            osName.contains("mac") || osName.contains("darwin") -> "macos"
            osName.contains("linux") || osName.contains("nix") || osName.contains("nux") -> "linux"
            else -> "unknown"
        }
    }
}
