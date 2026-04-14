package com.mobilebytelabs.kmptoolkit.bubble

internal class TvosBubblePermission : BubblePermission {
    override fun canShowBubble(): Boolean = false
    override fun canShowNotification(): Boolean = false
    override suspend fun requestBubblePermission(): Boolean = false
    override suspend fun requestNotificationPermission(): Boolean = false
}

actual fun createBubblePermission(): BubblePermission = TvosBubblePermission()
