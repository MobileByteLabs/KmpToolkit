package com.mobilebytelabs.kmptoolkit.clipboard.monitor

/**
 * Represents the current state of a [ClipboardMonitor][com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitor].
 *
 * @since 0.2.0
 */
sealed class ClipboardMonitorState {
    /** Monitor is created but not started. */
    data object Idle : ClipboardMonitorState()

    /** Monitor is actively watching clipboard changes. */
    data object Monitoring : ClipboardMonitorState()

    /** Monitor is temporarily paused (can be resumed). */
    data object Paused : ClipboardMonitorState()

    /** Monitor encountered an error. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ClipboardMonitorState()
}
