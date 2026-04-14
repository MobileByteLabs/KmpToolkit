package com.mobilebytelabs.kmptoolkit.bubble

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IosBubblePermission : BubblePermission {

    override fun canShowBubble(): Boolean {
        // iOS doesn't have floating bubbles — uses notifications instead
        return false
    }

    override fun canShowNotification(): Boolean {
        // Check synchronously — will return cached state
        // For accurate check, use the suspend version
        return true // Optimistic — actual check is async on iOS
    }

    override suspend fun requestBubblePermission(): Boolean {
        // No bubble permission on iOS
        return false
    }

    override suspend fun requestNotificationPermission(): Boolean = suspendCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { granted, error ->
            continuation.resume(granted)
        }
    }
}

actual fun createBubblePermission(): BubblePermission = IosBubblePermission()
