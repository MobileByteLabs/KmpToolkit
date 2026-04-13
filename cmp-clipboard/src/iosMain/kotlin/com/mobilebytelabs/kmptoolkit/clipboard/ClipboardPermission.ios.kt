package com.mobilebytelabs.kmptoolkit.clipboard

internal class IosClipboardPermission : ClipboardPermission {
    override fun hasClipboardAccess(): Boolean = true
    override fun hasOverlayPermission(): Boolean = false // iOS has no overlay permission
    override fun hasNotificationPermission(): Boolean = false // Phase 3: Check UNUserNotificationCenter
    override suspend fun requestClipboardAccess(): Boolean = true
    override suspend fun requestOverlayPermission(): Boolean = false // Not available on iOS
    override suspend fun requestNotificationPermission(): Boolean {
        // Phase 3: Request UNUserNotificationCenter authorization
        return false
    }
}

actual fun createClipboardPermission(): ClipboardPermission = IosClipboardPermission()
