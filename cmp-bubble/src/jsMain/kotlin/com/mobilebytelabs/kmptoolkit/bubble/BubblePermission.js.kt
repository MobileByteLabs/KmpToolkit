package com.mobilebytelabs.kmptoolkit.bubble

internal class JsBubblePermission : BubblePermission {
    override fun canShowBubble(): Boolean = false
    override fun canShowNotification(): Boolean {
        return try { (js("Notification.permission") as? String) == "granted" } catch (e: Throwable) { false }
    }
    override suspend fun requestBubblePermission(): Boolean = false
    override suspend fun requestNotificationPermission(): Boolean {
        return try {
            js("Notification.requestPermission()")
            canShowNotification()
        } catch (e: Throwable) { false }
    }
}

actual fun createBubblePermission(): BubblePermission = JsBubblePermission()
