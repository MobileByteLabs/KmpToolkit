@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSColor
import platform.AppKit.NSFont
import platform.AppKit.NSPanel
import platform.AppKit.NSScreen
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.Foundation.NSBundle
import platform.Foundation.NSMakeRect
import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private fun isNotificationAvailable(): Boolean = NSBundle.mainBundle.bundleIdentifier != null

internal class MacosBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    override val capability: BubbleCapability
        get() = try {
            if (NSScreen.mainScreen != null) BubbleCapability.FloatingWindow else BubbleCapability.Notification
        } catch (e: Exception) {
            BubbleCapability.Notification
        }

    override val capabilityReason: String
        get() = when (capability) {
            BubbleCapability.FloatingWindow -> "macOS floating panel (NSPanel)"
            else -> "macOS UNUserNotification"
        }

    private var currentNotificationId: String? = null
    private var floatingPanel: NSPanel? = null

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
            val resolvedStyle = if (style == BubbleStyle.Auto) {
                if (NSScreen.mainScreen != null) BubbleStyle.Floating else BubbleStyle.Notification
            } else {
                style
            }

            when (resolvedStyle) {
                BubbleStyle.Floating -> showAsFloatingPanel(title, message)
                else -> showAsNotification(title, message)
            }

            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        show(title = title, message = route, icon = icon, style = style, onTap = BubbleTapAction.DeepLink(route))
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, actions = actions, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        try {
            if (floatingPanel != null && (title != null || message != null)) {
                dismiss()
                show(title = title ?: "", message = message ?: "", style = BubbleStyle.Floating)
            } else if (isNotificationAvailable()) {
                val id = currentNotificationId ?: return
                val content = UNMutableNotificationContent().apply {
                    title?.let { setTitle(it) }
                    message?.let { setBody(it) }
                }
                val request = UNNotificationRequest.requestWithIdentifier(
                    identifier = id,
                    content = content,
                    trigger = null,
                )
                UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
            }
        } catch (_: Exception) {}
    }

    override fun dismiss() {
        try {
            floatingPanel?.close()
            floatingPanel = null

            if (isNotificationAvailable()) {
                currentNotificationId?.let { id ->
                    UNUserNotificationCenter.currentNotificationCenter()
                        .removeDeliveredNotificationsWithIdentifiers(listOf(id))
                }
            }
        } catch (_: Exception) {}
        currentNotificationId = null
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    // ── Floating Panel ──────────────────────────────────────────

    private fun showAsFloatingPanel(title: String, message: String) {
        floatingPanel?.close()

        val screen = NSScreen.mainScreen ?: return
        val panelWidth = 320.0
        val panelHeight = if (message.isEmpty()) 50.0 else 80.0

        // Get screen position
        var screenX = 0.0
        var screenY = 0.0
        var screenW = 1440.0
        screen.visibleFrame.useContents {
            screenX = origin.x
            screenY = origin.y
            screenW = size.width
        }

        val x = screenX + screenW - panelWidth - 20.0
        val y = screenY + 20.0

        val panel = NSPanel(
            contentRect = NSMakeRect(x, y, panelWidth, panelHeight),
            styleMask = 0uL, // borderless
            backing = NSBackingStoreBuffered,
            defer = false,
        )
        panel.setLevel(25) // floating level
        panel.setBackgroundColor(NSColor.windowBackgroundColor)
        panel.setOpaque(false)
        panel.hasShadow = true
        panel.setMovableByWindowBackground(true)

        // Content
        val contentView = NSView(NSMakeRect(0.0, 0.0, panelWidth, panelHeight))

        val titleLabel = NSTextField(NSMakeRect(12.0, panelHeight - 30.0, panelWidth - 24.0, 20.0))
        titleLabel.stringValue = title
        titleLabel.setEditable(false)
        titleLabel.setBordered(false)
        titleLabel.setDrawsBackground(false)
        titleLabel.font = NSFont.boldSystemFontOfSize(13.0)
        contentView.addSubview(titleLabel)

        if (message.isNotEmpty()) {
            val msgLabel = NSTextField(NSMakeRect(12.0, 8.0, panelWidth - 24.0, panelHeight - 40.0))
            msgLabel.stringValue = message
            msgLabel.setEditable(false)
            msgLabel.setBordered(false)
            msgLabel.setDrawsBackground(false)
            msgLabel.font = NSFont.systemFontOfSize(11.0)
            msgLabel.textColor = NSColor.secondaryLabelColor
            contentView.addSubview(msgLabel)
        }

        panel.contentView = contentView
        panel.makeKeyAndOrderFront(null)
        floatingPanel = panel
    }

    // ── Notification fallback ───────────────────────────────────

    private fun showAsNotification(title: String, message: String) {
        if (!isNotificationAvailable()) return

        val notificationId = NSUUID().UUIDString
        currentNotificationId = notificationId

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            if (config.sound) {
                setSound(UNNotificationSound.defaultSound)
            }
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notificationId,
            content = content,
            trigger = null,
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error == null) {
                _state.value = BubbleState.Showing
            }
        }
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = MacosBubble(config)
