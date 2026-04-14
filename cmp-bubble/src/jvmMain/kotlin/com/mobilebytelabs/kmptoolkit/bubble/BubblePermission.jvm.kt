package com.mobilebytelabs.kmptoolkit.bubble

internal class JvmBubblePermission : BubblePermission {
    override fun canShowBubble(): Boolean = java.awt.SystemTray.isSupported()
    override fun canShowNotification(): Boolean = java.awt.SystemTray.isSupported()
    override suspend fun requestBubblePermission(): Boolean = canShowBubble()
    override suspend fun requestNotificationPermission(): Boolean = canShowNotification()
}

actual fun createBubblePermission(): BubblePermission = JvmBubblePermission()
