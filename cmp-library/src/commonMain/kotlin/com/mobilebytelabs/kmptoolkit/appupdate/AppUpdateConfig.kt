package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Configuration for app update checking.
 *
 * Provides platform-specific identifiers and optional custom endpoints
 * for version checking.
 *
 * @property packageName Android package name (e.g., "com.example.app")
 * @property appStoreId iOS App Store ID (numeric, e.g., "123456789")
 * @property customVersionCheckUrl Optional custom URL for version checking
 * @property countryCode Country code for App Store lookup (default: "us")
 * @since 0.3.0
 */
data class AppUpdateConfig(
    val packageName: String? = null,
    val appStoreId: String? = null,
    val customVersionCheckUrl: String? = null,
    val countryCode: String = "us",
) {
    /**
     * Builder for creating AppUpdateConfig with platform-specific settings.
     */
    class Builder {
        private var packageName: String? = null
        private var appStoreId: String? = null
        private var customVersionCheckUrl: String? = null
        private var countryCode: String = "us"

        /**
         * Sets the Android package name.
         *
         * @param name Package name (e.g., "com.example.app")
         */
        fun packageName(name: String) = apply { packageName = name }

        /**
         * Sets the iOS App Store ID.
         *
         * @param id App Store ID (numeric string)
         */
        fun appStoreId(id: String) = apply { appStoreId = id }

        /**
         * Sets a custom URL for version checking.
         *
         * The URL should return JSON with version information.
         * Expected format: {"version": "1.2.3", "updateType": "FLEXIBLE"}
         *
         * @param url The custom version check endpoint
         */
        fun customVersionCheckUrl(url: String) = apply { customVersionCheckUrl = url }

        /**
         * Sets the country code for App Store lookup.
         *
         * @param code ISO 3166-1 alpha-2 country code (default: "us")
         */
        fun countryCode(code: String) = apply { countryCode = code }

        /**
         * Builds the AppUpdateConfig instance.
         */
        fun build() = AppUpdateConfig(
            packageName = packageName,
            appStoreId = appStoreId,
            customVersionCheckUrl = customVersionCheckUrl,
            countryCode = countryCode,
        )
    }

    companion object {
        /**
         * Creates a new Builder instance.
         */
        fun builder() = Builder()

        /**
         * Default configuration with no platform-specific settings.
         *
         * Will attempt to auto-detect platform identifiers at runtime.
         */
        val Default = AppUpdateConfig()
    }
}
