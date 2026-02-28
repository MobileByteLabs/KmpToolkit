package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Contains information about an available update.
 *
 * @property isAvailable Whether an update is available
 * @property currentVersion The currently installed version
 * @property latestVersion The latest available version (null if unknown)
 * @property updateType The type/priority of the update
 * @property releaseNotes Release notes for the update (null if not available)
 * @property storeUrl Direct URL to the app store listing (null if not available)
 * @since 0.3.0
 */
data class UpdateInfo(
    val isAvailable: Boolean,
    val currentVersion: AppVersion,
    val latestVersion: AppVersion? = null,
    val updateType: UpdateType = UpdateType.NONE,
    val releaseNotes: String? = null,
    val storeUrl: String? = null,
) {
    companion object {
        /**
         * Creates an UpdateInfo indicating no update is available.
         *
         * @param currentVersion The currently installed version
         * @return UpdateInfo with isAvailable = false
         */
        fun noUpdate(currentVersion: AppVersion): UpdateInfo = UpdateInfo(
            isAvailable = false,
            currentVersion = currentVersion,
            updateType = UpdateType.NONE,
        )

        /**
         * Creates an UpdateInfo indicating an update is available.
         *
         * @param currentVersion The currently installed version
         * @param latestVersion The latest available version
         * @param updateType The type/priority of the update
         * @param releaseNotes Optional release notes
         * @param storeUrl Optional direct URL to the store
         * @return UpdateInfo with isAvailable = true
         */
        fun available(
            currentVersion: AppVersion,
            latestVersion: AppVersion,
            updateType: UpdateType = UpdateType.FLEXIBLE,
            releaseNotes: String? = null,
            storeUrl: String? = null,
        ): UpdateInfo = UpdateInfo(
            isAvailable = true,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            updateType = updateType,
            releaseNotes = releaseNotes,
            storeUrl = storeUrl,
        )
    }
}
