package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * WebAssembly WASI implementation of AppUpdate.
 *
 * In-app updates are not applicable for WASI applications
 * as they run in sandboxed environments without UI.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as WASI doesn't support in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not applicable for WASI applications. " +
                "WASI runs in sandboxed environments without UI capabilities.",
        )

    /**
     * Returns UNKNOWN as WASI doesn't have standard version mechanisms.
     */
    actual fun getCurrentVersion(): AppVersion = AppVersion.UNKNOWN

    /**
     * Returns NotSupported as WASI doesn't support in-app updates.
     */
    actual suspend fun startUpdate(
        updateType: UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult =
        UpdateResult.NotSupported(
            "In-app updates are not applicable for WASI applications.",
        )

    /**
     * Returns false as WASI doesn't support opening URLs.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean = false

    /**
     * In-app updates are not supported for WASI.
     */
    actual fun isSupported(): Boolean = false
}
