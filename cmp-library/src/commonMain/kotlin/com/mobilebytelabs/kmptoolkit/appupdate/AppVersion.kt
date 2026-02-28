package com.mobilebytelabs.kmptoolkit.appupdate

/*
 * Represents an application version with semantic versioning support.
 *
 * This class provides structured version information and comparison capabilities
 * for determining if updates are available.
 *
 * @since 0.3.0
 */

/**
 * Represents an application version.
 *
 * @property major The major version number (breaking changes)
 * @property minor The minor version number (new features)
 * @property patch The patch version number (bug fixes)
 * @property versionName The full version string (e.g., "1.2.3")
 * @property versionCode Optional numeric version code (Android: versionCode, iOS: CFBundleVersion)
 * @since 0.3.0
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val versionName: String,
    val versionCode: Long? = null,
) : Comparable<AppVersion> {

    /**
     * Compares this version with another version.
     *
     * @param other The version to compare with
     * @return negative if this < other, zero if equal, positive if this > other
     */
    override fun compareTo(other: AppVersion): Int {
        val majorDiff = major.compareTo(other.major)
        if (majorDiff != 0) return majorDiff

        val minorDiff = minor.compareTo(other.minor)
        if (minorDiff != 0) return minorDiff

        return patch.compareTo(other.patch)
    }

    /**
     * Checks if this version is older than another version.
     */
    fun isOlderThan(other: AppVersion): Boolean = this < other

    /**
     * Checks if this version is newer than another version.
     */
    fun isNewerThan(other: AppVersion): Boolean = this > other

    override fun toString(): String = versionName

    companion object {
        /**
         * Parses a version string into an AppVersion.
         *
         * Supports formats:
         * - "1.2.3"
         * - "v1.2.3"
         * - "1.2" (patch defaults to 0)
         * - "1" (minor and patch default to 0)
         *
         * @param versionString The version string to parse
         * @return AppVersion or null if parsing fails
         * @since 0.3.0
         */
        fun parse(versionString: String): AppVersion? {
            val cleaned = versionString.trim().removePrefix("v").removePrefix("V")
            val parts = cleaned.split(".")

            return try {
                val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
                val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

                AppVersion(
                    major = major,
                    minor = minor,
                    patch = patch,
                    versionName = "$major.$minor.$patch",
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Creates an AppVersion from individual components.
         *
         * @param major Major version number
         * @param minor Minor version number
         * @param patch Patch version number
         * @param versionCode Optional numeric version code
         * @return AppVersion instance
         * @since 0.3.0
         */
        fun of(major: Int, minor: Int, patch: Int, versionCode: Long? = null): AppVersion =
            AppVersion(
                major = major,
                minor = minor,
                patch = patch,
                versionName = "$major.$minor.$patch",
                versionCode = versionCode,
            )

        /**
         * An unknown/invalid version placeholder.
         */
        val UNKNOWN = AppVersion(0, 0, 0, "0.0.0")
    }
}
