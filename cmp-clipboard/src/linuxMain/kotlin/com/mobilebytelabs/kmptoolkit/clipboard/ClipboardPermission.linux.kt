package com.mobilebytelabs.kmptoolkit.clipboard

internal class LinuxClipboardPermission : ClipboardPermission {
    override fun hasClipboardAccess(): Boolean = true
    override fun hasOverlayPermission(): Boolean = true
    override fun hasNotificationPermission(): Boolean = true
    override suspend fun requestClipboardAccess(): Boolean = true
    override suspend fun requestOverlayPermission(): Boolean = true
    override suspend fun requestNotificationPermission(): Boolean = true
}

actual fun createClipboardPermission(): ClipboardPermission = LinuxClipboardPermission()
