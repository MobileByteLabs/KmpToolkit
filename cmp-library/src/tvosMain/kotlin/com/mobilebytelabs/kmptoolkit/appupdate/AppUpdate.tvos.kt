package com.mobilebytelabs.kmptoolkit.appupdate

import platform.Foundation.NSBundle

/**
 * tvOS implementation of AppUpdate.
 *
 * In-app updates are not supported on tvOS as the platform
 * handles app updates through the tvOS App Store automatically.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as tvOS doesn't support in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not supported on tvOS. " +
                "Updates are handled automatically by the tvOS App Store.",
        )

    /**
     * Gets the currently installed app version from Info.plist.
     */
    actual fun getCurrentVersion(): AppVersion {
        val bundle = NSBundle.mainBundle
        val versionString = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val buildString = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String

        val versionName = versionString ?: "0.0.0"
        val versionCode = buildString?.toLongOrNull()

        return AppVersion.parse(versionName)?.copy(versionCode = versionCode)
            ?: AppVersion.UNKNOWN.copy(versionName = versionName, versionCode = versionCode)
    }

    /**
     * Returns NotSupported as tvOS doesn't support in-app updates.
     */
    actual suspend fun startUpdate(
        updateType: UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not supported on tvOS.",
        )

    /**
     * Returns false as tvOS doesn't support opening App Store for updates.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates are not supported on tvOS.
     */
    actual fun isSupported(): Boolean = false
}
