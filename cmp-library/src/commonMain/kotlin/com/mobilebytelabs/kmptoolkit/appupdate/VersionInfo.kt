package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Version information returned by a [VersionResolver].
 *
 * Contains all the information needed to determine if an update is available
 * and where to download it.
 *
 * ## Example
 *
 * ```kotlin
 * val info = VersionInfo(
 *     version = "1.2.3",
 *     updateType = UpdateType.FLEXIBLE,
 *     releaseNotes = "Bug fixes and improvements",
 *     downloadUrl = "https://example.com/download/app-1.2.3.exe",
 * )
 * ```
 *
 * @property version Version string (e.g., "1.2.3")
 * @property updateType Type of update: FLEXIBLE or IMMEDIATE
 * @property releaseNotes Optional release notes or changelog
 * @property downloadUrl Direct download URL for the update
 * @property storeUrl Store page URL (e.g., GitHub releases page)
 * @property metadata Additional key-value metadata
 * @since 0.5.0
 */
data class VersionInfo(
    val version: String,
    val updateType: UpdateType = UpdateType.FLEXIBLE,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val storeUrl: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Parses the version string into an [AppVersion].
     *
     * @return Parsed [AppVersion] or null if parsing fails
     */
    fun toAppVersion(): AppVersion? = AppVersion.parse(version)

    companion object {
        /**
         * Creates a VersionInfo indicating no update is available.
         */
        fun noUpdate(currentVersion: String) = VersionInfo(
            version = currentVersion,
            updateType = UpdateType.NONE,
        )
    }
}
