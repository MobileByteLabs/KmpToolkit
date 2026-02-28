package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Cross-platform in-app update manager.
 *
 * Provides a unified API for checking and triggering app updates across
 * all supported platforms.
 *
 * ## Platform Support
 *
 * | Platform | Implementation |
 * |----------|----------------|
 * | Android | Google Play In-App Updates API |
 * | iOS | iTunes Lookup API + App Store |
 * | macOS | iTunes Lookup API + App Store |
 * | JVM | Custom version check endpoint |
 * | Linux | Custom version check endpoint |
 * | Windows | Custom version check endpoint |
 * | tvOS | Not supported (returns NotSupported) |
 * | watchOS | Not supported (returns NotSupported) |
 * | JS/Wasm | Not supported (returns NotSupported) |
 *
 * ## Usage
 *
 * ```kotlin
 * // Check for updates
 * val result = AppUpdate.checkForUpdate()
 * when (result) {
 *     is UpdateResult.Success -> {
 *         if (result.updateInfo.isAvailable) {
 *             // Handle available update
 *         }
 *     }
 *     is UpdateResult.Error -> {
 *         // Handle error
 *     }
 *     is UpdateResult.NotSupported -> {
 *         // Platform doesn't support in-app updates
 *     }
 *     is UpdateResult.Cancelled -> {
 *         // User cancelled the update
 *     }
 * }
 *
 * // Get current app version
 * val version = AppUpdate.getCurrentVersion()
 *
 * // Open store for manual update
 * AppUpdate.openStoreForUpdate()
 * ```
 *
 * @since 0.3.0
 */
expect object AppUpdate {
    /**
     * Checks if an update is available.
     *
     * This is a suspend function that performs network requests on supported
     * platforms. On platforms that don't support in-app updates, it returns
     * [UpdateResult.NotSupported].
     *
     * @param config Optional configuration for update checking
     * @return UpdateResult indicating the check outcome
     */
    suspend fun checkForUpdate(config: AppUpdateConfig = AppUpdateConfig.Default): UpdateResult

    /**
     * Gets the currently installed app version.
     *
     * @return The current app version, or [AppVersion.UNKNOWN] if unavailable
     */
    fun getCurrentVersion(): AppVersion

    /**
     * Starts the in-app update flow.
     *
     * On Android, this triggers the Play In-App Updates flow.
     * On iOS/macOS, this opens the App Store to the app's page.
     * On desktop platforms, this opens the custom update URL if configured.
     *
     * @param updateType The type of update to perform (FLEXIBLE or IMMEDIATE)
     * @param config Optional configuration
     * @return UpdateResult indicating the operation outcome
     */
    suspend fun startUpdate(
        updateType: UpdateType = UpdateType.FLEXIBLE,
        config: AppUpdateConfig = AppUpdateConfig.Default,
    ): UpdateResult

    /**
     * Opens the app store page for manual update.
     *
     * @param config Optional configuration containing store identifiers
     * @return true if the store was opened, false otherwise
     */
    fun openStoreForUpdate(config: AppUpdateConfig = AppUpdateConfig.Default): Boolean

    /**
     * Checks if in-app updates are supported on the current platform.
     *
     * @return true if in-app updates are supported, false otherwise
     */
    fun isSupported(): Boolean
}
