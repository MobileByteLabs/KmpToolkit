package com.mobilebytelabs.kmptoolkit.appupdate

/**
 * Represents the result of an update operation.
 *
 * @since 0.3.0
 */
sealed class UpdateResult {
    /**
     * The update check or download completed successfully.
     *
     * @property updateInfo Information about the available update
     */
    data class Success(val updateInfo: UpdateInfo) : UpdateResult()

    /**
     * The update was cancelled by the user.
     */
    data object Cancelled : UpdateResult()

    /**
     * An error occurred during the update process.
     *
     * @property message A human-readable error message
     * @property cause The underlying exception (null if not available)
     */
    data class Error(val message: String, val cause: Throwable? = null) : UpdateResult()

    /**
     * In-app updates are not supported on this platform.
     *
     * @property reason Explanation of why updates are not supported
     */
    data class NotSupported(val reason: String) : UpdateResult()

    /**
     * Checks if this result represents a successful update check.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Checks if this result indicates an update is available.
     */
    val isUpdateAvailable: Boolean
        get() = (this as? Success)?.updateInfo?.isAvailable == true

    /**
     * Returns the UpdateInfo if successful, null otherwise.
     */
    fun updateInfoOrNull(): UpdateInfo? = (this as? Success)?.updateInfo

    /**
     * Returns the error message if this is an error result, null otherwise.
     */
    val errorMessage: String?
        get() = when (this) {
            is Error -> message
            is NotSupported -> reason
            else -> null
        }
}
