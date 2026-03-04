package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * JavaScript implementation of AppUpdate.
 *
 * In-app updates are not applicable for JavaScript/web applications
 * as web apps are updated by refreshing the page or through
 * service worker updates.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as JS doesn't support traditional in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult = UpdateResult.NotSupported(
        "In-app updates are not applicable for JavaScript applications. " +
            "Web apps are updated by refreshing the page or through service workers.",
    )

    /**
     * Returns UNKNOWN as version tracking depends on the deployment setup.
     */
    actual fun getCurrentVersion(): AppVersion = AppVersion.UNKNOWN

    /**
     * Returns NotSupported as JS doesn't support traditional in-app updates.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not applicable for JavaScript applications.",
        )

    /**
     * Returns false as there's no store to open for JS apps.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates are not supported for JavaScript.
     */
    actual fun isSupported(): Boolean = false
}
