package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * WebAssembly (JS) implementation of AppUpdate.
 *
 * In-app updates are not applicable for WebAssembly applications
 * running in browsers as they follow web app update patterns.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as WasmJs doesn't support traditional in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not applicable for WebAssembly applications. " +
                "Web apps are updated by refreshing the page or through service workers.",
        )

    /**
     * Returns UNKNOWN as version tracking depends on the deployment setup.
     */
    actual fun getCurrentVersion(): AppVersion = AppVersion.UNKNOWN

    /**
     * Returns NotSupported as WasmJs doesn't support traditional in-app updates.
     */
    actual suspend fun startUpdate(
        updateType: UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not applicable for WebAssembly applications.",
        )

    /**
     * Returns false as there's no store to open for WebAssembly apps.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates are not supported for WebAssembly.
     */
    actual fun isSupported(): Boolean = false
}
