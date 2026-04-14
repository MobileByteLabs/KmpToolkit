package com.mobilebytelabs.kmptoolkit.bubble

internal class MacosBubblePermission : BubblePermission {
    override fun canShowBubble(): Boolean = false // macOS doesn't have floating bubbles
    override fun canShowNotification(): Boolean = true // macOS notifications don't need permission
    override suspend fun requestBubblePermission(): Boolean = false
    override suspend fun requestNotificationPermission(): Boolean = true
}

actual fun createBubblePermission(): BubblePermission = MacosBubblePermission()
