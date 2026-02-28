package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Represents the type/priority of an available update.
 *
 * @since 0.3.0
 */
enum class UpdateType {
    /**
     * No update is available.
     */
    NONE,

    /**
     * An optional update is available.
     *
     * The user can continue using the app without updating.
     * Typically used for minor improvements or non-critical bug fixes.
     *
     * On Android: Corresponds to AppUpdateType.FLEXIBLE
     */
    FLEXIBLE,

    /**
     * A required/critical update is available.
     *
     * The user should update before continuing to use the app.
     * Used for security patches, critical bug fixes, or breaking API changes.
     *
     * On Android: Corresponds to AppUpdateType.IMMEDIATE
     */
    IMMEDIATE,
}
