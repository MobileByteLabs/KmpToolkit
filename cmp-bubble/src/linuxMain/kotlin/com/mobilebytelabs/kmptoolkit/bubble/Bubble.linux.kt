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
import platform.posix.system

/**
 * Linux desktop bubbles via `notify-send`.
 *
 * ## What changed and why
 * This reported [BubbleCapability.None] — "Linux has no notification capability". Linux has had a
 * freedesktop notification spec for twenty years, and `notify-send` (from `libnotify-bin`) is its
 * standard command-line entry point; GNOME, KDE, XFCE and the rest all honour it. This module's
 * own Linux actuals elsewhere already shell out to `xdg-open` and `xclip`, so the mechanism was
 * neither novel nor unavailable — it simply was not used.
 *
 * `notify-send` is not always installed, so a failed dispatch leaves the state unchanged rather
 * than claiming the bubble appeared.
 */
internal class LinuxBubble(private val config: BubbleConfig) : Bubble {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()

    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    override val capability: BubbleCapability = BubbleCapability.Notification

    override val capabilityReason: String =
        "freedesktop notification via notify-send (requires libnotify-bin)"

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        val urgency = if (style == BubbleStyle.Persistent) "critical" else "normal"
        val expire = if (autoDismissMs > 0) autoDismissMs.toString() else "0"
        val command = buildString {
            append("notify-send")
            append(" --app-name='").append(config.channelName.shellEscape()).append("'")
            append(" --urgency=").append(urgency)
            append(" --expire-time=").append(expire)
            append(" '").append(title.shellEscape()).append("'")
            append(" '").append(message.shellEscape()).append("'")
            append(" >/dev/null 2>&1")
        }
        // A non-zero exit means notify-send is missing or the session bus is unreachable; saying
        // "Showing" then would be a lie the caller cannot detect.
        if (system(command) == 0) {
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
        // A desktop notification cannot embed a screen; naming the route is the honest degradation.
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        // expire-time 0 asks the daemon to keep it until dismissed.
        show(title = title, message = message, style = BubbleStyle.Persistent, autoDismissMs = 0L)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        // The spec supports replacing by id, but notify-send does not expose one — reissue.
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

/** Single-quote escaping for a `sh -c` argument: close, escape, reopen. */
private fun String.shellEscape(): String = replace("'", "'\\''")

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(LinuxBubble(config))
