package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Linux implementation of AppUpdate.
 *
 * This implementation provides basic support for update checking.
 * Linux native doesn't have built-in HTTP client, so automatic
 * update checking requires external tools or custom implementation.
 *
 * For production use, consider using the JVM target for desktop
 * applications with full HTTP support.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    private var currentVersionOverride: AppVersion? = null

    /**
     * Returns NotSupported for automatic update checking.
     *
     * Linux native doesn't have built-in HTTP client in this implementation.
     * For update checking, use a JVM-based desktop app or integrate with
     * platform-specific update mechanisms (apt, flatpak, snap, etc.).
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult =
        UpdateResult.NotSupported(
            "Automatic update checking on Linux native requires a custom implementation. " +
                "Consider using the JVM target for desktop applications with full HTTP support, " +
                "or integrate with your distribution's package manager.",
        )

    /**
     * Gets the currently installed app version.
     *
     * Returns manually set override or UNKNOWN.
     */
    actual fun getCurrentVersion(): AppVersion {
        return currentVersionOverride ?: AppVersion.UNKNOWN
    }

    /**
     * Returns NotSupported for automatic updates.
     */
    actual suspend fun startUpdate(
        updateType: UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult =
        UpdateResult.NotSupported(
            "Automatic updates on Linux native are not supported. " +
                "Consider using system package managers for updates.",
        )

    /**
     * Linux native doesn't support opening URLs without additional tools.
     * Returns false.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates have limited support on Linux native.
     */
    actual fun isSupported(): Boolean = false

    /**
     * Sets the current version manually.
     */
    fun setCurrentVersion(version: AppVersion) {
        currentVersionOverride = version
    }
}
