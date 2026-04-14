package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.SystemTray
import java.awt.TrayIcon

internal class JvmBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing
    override val capability: BubbleCapability = if (java.awt.SystemTray.isSupported()) BubbleCapability.Notification else BubbleCapability.None
    override val capabilityReason: String = if (java.awt.SystemTray.isSupported()) "JVM SystemTray" else "Headless JVM (no SystemTray)"

    private var trayIcon: TrayIcon? = null

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        try {
            if (!SystemTray.isSupported()) return
            val systemTray = SystemTray.getSystemTray()

            // Create/reuse tray icon
            if (trayIcon == null) {
                val image = java.awt.Toolkit.getDefaultToolkit().createImage(ByteArray(0))
                trayIcon = TrayIcon(image, "KmpToolkit Bubble").also {
                    it.isImageAutoSize = true
                    systemTray.add(it)
                }
            }

            val messageType = when (style) {
                BubbleStyle.Service, BubbleStyle.Persistent -> TrayIcon.MessageType.INFO
                else -> TrayIcon.MessageType.NONE
            }

            trayIcon?.displayMessage(title, message, messageType)
            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            // Headless environment or SystemTray not supported
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (title != null || message != null) {
            trayIcon?.displayMessage(title ?: "", message ?: "", TrayIcon.MessageType.NONE)
        }
    }

    override fun dismiss() {
        trayIcon?.let { icon ->
            try {
                SystemTray.getSystemTray().remove(icon)
            } catch (e: Exception) {}
        }
        trayIcon = null
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = JvmBubble(config)
