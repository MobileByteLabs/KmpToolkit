package com.mobilebytelabs.kmptoolkit.clipboard

internal class WasmWasiClipboardPermission : ClipboardPermission {
    override fun hasClipboardAccess(): Boolean = false
    override fun hasOverlayPermission(): Boolean = false
    override fun hasNotificationPermission(): Boolean = false
    override suspend fun requestClipboardAccess(): Boolean = false
    override suspend fun requestOverlayPermission(): Boolean = false
    override suspend fun requestNotificationPermission(): Boolean = false
}

actual fun createClipboardPermission(): ClipboardPermission = WasmWasiClipboardPermission()
