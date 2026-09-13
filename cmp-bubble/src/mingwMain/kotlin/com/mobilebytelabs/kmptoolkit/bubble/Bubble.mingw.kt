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
 * Windows desktop bubbles via a PowerShell WinForms `NotifyIcon` balloon tip.
 *
 * ## What changed and why
 * This reported [BubbleCapability.None] — "MinGW has no notification capability". Windows has had
 * tray balloon notifications since XP, and `System.Windows.Forms.NotifyIcon` reaches them from
 * PowerShell, which ships with the OS. The same subprocess route already powers this toolkit's
 * Windows file dialogs in `cmp-intent-launcher`, so it is neither novel nor unavailable here.
 *
 * A balloon is used rather than a modern toast because a toast requires a registered AppUserModelID
 * — real Windows integration work that belongs to an installer, not to a library that has to work
 * from an unpackaged binary.
 */
internal class MingwBubble(private val config: BubbleConfig) : Bubble {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()

    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    override val capability: BubbleCapability = BubbleCapability.Notification

    override val capabilityReason: String =
        "Windows tray balloon via PowerShell System.Windows.Forms.NotifyIcon"

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        val timeout = if (autoDismissMs > 0) autoDismissMs else DEFAULT_TIMEOUT_MS
        // `$` is escaped throughout: these are PowerShell variables, not Kotlin templates.
        val script = buildString {
            append("Add-Type -AssemblyName System.Windows.Forms; ")
            append("\$n = New-Object System.Windows.Forms.NotifyIcon; ")
            // A NotifyIcon must carry an icon to be shown at all; borrowing the host process's
            // own icon avoids shipping an image resource with the library.
            append("\$n.Icon = [System.Drawing.SystemIcons]::Information; ")
            append("\$n.Visible = \$true; ")
            append("\$n.ShowBalloonTip(").append(timeout).append(", ")
            append("'").append(title.psEscape()).append("', ")
            append("'").append(message.psEscape()).append("', ")
            append("[System.Windows.Forms.ToolTipIcon]::Info); ")
            // Without the sleep the process exits and the tray icon disappears before the
            // balloon renders.
            append("Start-Sleep -Milliseconds ").append(timeout).append("; ")
            append("\$n.Dispose()")
        }
        val command = "powershell -NoProfile -NonInteractive -Command \"$script\" 2>NUL"
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
        // A balloon cannot embed a screen; naming the route is the honest degradation.
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        // Balloons always expire; the longest sensible dwell is the closest thing to persistent.
        show(title = title, message = message, style = style, autoDismissMs = PERSISTENT_TIMEOUT_MS)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000L
        const val PERSISTENT_TIMEOUT_MS = 30_000L
    }
}

/**
 * Escape for a single-quoted PowerShell literal, which only treats `'` specially — then strip the
 * characters that would let text break out of the surrounding `cmd` string.
 */
private fun String.psEscape(): String = replace("'", "''")
    .filterNot { it == '"' || it == '\n' || it == '\r' || it == '&' || it == '|' || it == '<' || it == '>' }

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(MingwBubble(config))
