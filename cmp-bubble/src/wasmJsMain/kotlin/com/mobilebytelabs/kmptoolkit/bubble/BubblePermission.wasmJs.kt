package com.mobilebytelabs.kmptoolkit.bubble

internal class WasmJsBubblePermission : BubblePermission {
    override fun canShowBubble(): Boolean = false
    override fun canShowNotification(): Boolean = jsHasNotificationPermission()
    override suspend fun requestBubblePermission(): Boolean = false
    override suspend fun requestNotificationPermission(): Boolean {
        jsRequestNotificationPermission()
        return jsHasNotificationPermission()
    }
}

actual fun createBubblePermission(): BubblePermission = WasmJsBubblePermission()
