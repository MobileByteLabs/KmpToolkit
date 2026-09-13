/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSBundle
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * watchOS bubbles via `UNUserNotificationCenter` local notifications.
 *
 * ## What changed and why
 * This reported [BubbleCapability.None] — "watchOS has no notification capability". It does:
 * `UserNotifications` is part of the watchOS SDK and resolves in Kotlin/Native (verified by
 * compiling against it). watchOS delivers these to the wrist, honouring the user's notification settings.
 *
 * There is no floating-window or chat-head surface here, so a local notification is the whole of
 * what the platform offers — which is exactly what [BubbleCapability.Notification] describes.
 *
 * **Authorisation is the app's job.** A notification only appears once the user has granted
 * permission; request it through `createBubblePermission()` before showing anything. An
 * unauthorised request is dropped by the OS, which is why [state] only advances once the request
 * is accepted for delivery.
 */
internal class WatchOsBubble(private val config: BubbleConfig) : Bubble {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()

    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    override val capability: BubbleCapability
        get() = if (isNotificationAvailable) BubbleCapability.Notification else BubbleCapability.None

    override val capabilityReason: String
        get() = if (isNotificationAvailable) {
            "watchOS UNUserNotificationCenter local notification"
        } else {
            "No application bundle — UNUserNotificationCenter is unavailable outside a real app " +
                "(a bare test binary, for instance)"
        }

    private var counter = 0

    /**
     * Whether this process has a real application bundle.
     *
     * `UNUserNotificationCenter.currentNotificationCenter()` raises
     * `NSInternalInconsistencyException: bundleProxyForCurrentProcess is nil` when there is none —
     * and an Objective-C exception is NOT a Kotlin `Throwable`, so `runCatching` cannot catch it
     * and the process **terminates**. CI surfaced this the first time watchOS code in this repo
     * ever executed: a unit-test binary has no bundle, so `dismiss()` killed the test runner.
     *
     * Checking the bundle first is the only way to avoid it, and it is right for production too —
     * an unusual host (a bare executable, some extension contexts) would have crashed identically.
     */
    private val isNotificationAvailable: Boolean
        get() = NSBundle.mainBundle.bundleIdentifier != null

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        if (!isNotificationAvailable) return
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            if (config.sound) setSound(platform.UserNotifications.UNNotificationSound.defaultSound)
        }
        // A null trigger means "deliver immediately".
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = config.channelId + "-" + counter++,
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { error ->
                if (error == null) _state.value = BubbleState.Showing
            }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        // No embeddable surface on watchOS; naming the route is the honest degradation.
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        // Local notifications always follow the user's own dismissal rules; there is no
        // persistent variant to reach for.
        show(title = title, message = message, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        if (isNotificationAvailable) {
            UNUserNotificationCenter.currentNotificationCenter().removeAllDeliveredNotifications()
        }
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(WatchOsBubble(config))
