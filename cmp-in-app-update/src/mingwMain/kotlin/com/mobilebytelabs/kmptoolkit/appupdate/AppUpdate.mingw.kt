package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Windows (mingw) implementation of AppUpdate.
 *
 * This implementation provides basic support for update checking via
 * custom version check endpoints. Full WinINet-based HTTP support
 * requires additional configuration.
 *
 * For production use, consider using a custom backend endpoint
 * configured via [AppUpdateConfig.windowsVersionCheckUrl].
 *
 * @since 0.5.0
 */
actual object AppUpdate {
    private var currentVersionOverride: AppVersion? = null

    /**
     * Returns NotSupported for automatic update checking.
     *
     * Windows native doesn't have built-in HTTP client in this implementation.
     * For update checking, use a JVM-based desktop app or integrate with
     * platform-specific update mechanisms.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        // Check if Windows is disabled
        if (!config.windowsEnabled) {
            return UpdateResult.NotSupported("Windows updates disabled in configuration")
        }

        // Check if version check URL is configured
        val versionCheckUrl = config.getEffectiveWindowsVersionCheckUrl()
        if (versionCheckUrl == null) {
            return UpdateResult.NotSupported(
                "Windows requires windowsVersionCheckUrl or customVersionCheckUrl in AppUpdateConfig. " +
                    "Consider using the JVM target for desktop applications with full HTTP support.",
            )
        }

        return UpdateResult.NotSupported(
            "Automatic update checking on Windows requires a custom implementation. " +
                "Consider using the JVM target for desktop applications with full HTTP support.",
        )
    }

    /**
     * Gets the currently installed app version.
     *
     * Returns manually set override or UNKNOWN.
     */
    actual fun getCurrentVersion(): AppVersion = currentVersionOverride ?: AppVersion.UNKNOWN

    /**
     * Returns NotSupported for automatic updates.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult {
        // Check if Windows is disabled
        if (!config.windowsEnabled) {
            return UpdateResult.NotSupported("Windows updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "Automatic updates on Windows native are not supported. " +
                "Use openStoreForUpdate() to open a download URL in the browser.",
        )
    }

    /**
     * Windows native doesn't support opening URLs without shell32.
     * Returns false.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates have limited support on Windows native.
     */
    actual fun isSupported(): Boolean = false

    /**
     * Sets the current version manually.
     */
    fun setCurrentVersion(version: AppVersion) {
        currentVersionOverride = version
    }
}
