package com.mobilebytelabs.kmptoolkit.appupdate

import platform.Foundation.NSBundle

/**
 * watchOS implementation of AppUpdate.
 *
 * In-app updates are not supported on watchOS as the platform
 * handles app updates through the paired iPhone or automatically.
 *
 * @since 0.5.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as watchOS doesn't support in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        // Check if watchOS is disabled in config
        if (!config.watchosEnabled) {
            return UpdateResult.NotSupported("watchOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on watchOS. " +
                "Updates are handled through the paired iPhone or automatically.",
        )
    }

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
     * Returns NotSupported as watchOS doesn't support in-app updates.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult {
        // Check if watchOS is disabled in config
        if (!config.watchosEnabled) {
            return UpdateResult.NotSupported("watchOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on watchOS.",
        )
    }

    /**
     * Returns false as watchOS doesn't support opening App Store for updates.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates are not supported on watchOS.
     */
    actual fun isSupported(): Boolean = false
}
