package com.mobilebytelabs.kmptoolkit.clipboard

internal class MacosClipboardPermission : ClipboardPermission {
    override fun hasClipboardAccess(): Boolean = true
    override fun hasOverlayPermission(): Boolean = true // macOS allows status bar items
    override fun hasNotificationPermission(): Boolean = true
    override suspend fun requestClipboardAccess(): Boolean = true
    override suspend fun requestOverlayPermission(): Boolean = true
    override suspend fun requestNotificationPermission(): Boolean = true
}

actual fun createClipboardPermission(): ClipboardPermission = MacosClipboardPermission()
