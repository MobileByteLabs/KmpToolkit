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
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

/**
 * JVM desktop bubbles via the AWT system tray.
 *
 * ## What changed and why
 * This reported [BubbleCapability.None] — "JVM has no notification capability". The JDK has
 * shipped `java.awt.SystemTray` + `TrayIcon.displayMessage` since Java 6, which raises a real
 * desktop notification on Windows, macOS and most Linux desktops. No dependency, no native code.
 *
 * The tray icon is created lazily on first [show] and reused, because adding one to the tray is
 * visible to the user — doing it at construction would put an icon in the menu bar of an app that
 * may never notify.
 *
 * Where the tray genuinely is not available — a headless JVM, or a desktop environment with no
 * system tray — [capability] reports [BubbleCapability.None] with a reason saying which.
 */
internal class JvmBubble(private val config: BubbleConfig) : Bubble {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()

    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    private val traySupported: Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    override val capability: BubbleCapability =
        if (traySupported) BubbleCapability.Notification else BubbleCapability.None

    override val capabilityReason: String =
        if (traySupported) {
            "AWT SystemTray notification"
        } else {
            "No system tray on this JVM (headless, or a desktop environment without one)"
        }

    private var trayIcon: TrayIcon? = null

    private fun ensureTrayIcon(): TrayIcon? {
        if (!traySupported) return null
        trayIcon?.let { return it }
        return runCatching {
            // A 1x1 transparent image: the API requires one, but a notifier that has no business
            // occupying the menu bar should not draw anything there.
            val image = Toolkit.getDefaultToolkit().createImage(ByteArray(0))
            TrayIcon(image, config.channelName).apply {
                isImageAutoSize = true
                SystemTray.getSystemTray().add(this)
            }
        }.getOrNull()?.also { trayIcon = it }
    }

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        val tray = ensureTrayIcon() ?: return
        runCatching {
            tray.displayMessage(title, message, TrayIcon.MessageType.INFO)
            _state.value = BubbleState.Showing
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        // A tray notification cannot embed a screen; naming the route is the honest degradation.
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        // AWT notifications always auto-dismiss; there is no persistent variant to reach for.
        show(title = title, message = message, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        runCatching { trayIcon?.let { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    // BubbleStyle models placement (Floating / Notification / Persistent / Service / Auto), not
    // severity, so there is nothing here to map onto AWT's ERROR/WARNING message types.
}

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(JvmBubble(config))
