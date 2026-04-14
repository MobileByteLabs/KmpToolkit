package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer

internal class JvmBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    override val capability: BubbleCapability
        get() = try {
            if (GraphicsEnvironment.isHeadless()) {
                BubbleCapability.None
            } else {
                BubbleCapability.FloatingWindow
            }
        } catch (e: Exception) {
            BubbleCapability.None
        }

    override val capabilityReason: String
        get() = when (capability) {
            BubbleCapability.FloatingWindow -> "JVM floating window (JWindow)"
            BubbleCapability.None -> "Headless JVM (no display)"
            else -> "JVM"
        }

    private var floatingWindow: JWindow? = null
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
            if (GraphicsEnvironment.isHeadless()) return

            val resolvedStyle = if (style == BubbleStyle.Auto) BubbleStyle.Floating else style

            when (resolvedStyle) {
                BubbleStyle.Floating -> showAsFloatingWindow(title, message, actions, onTap, autoDismissMs)
                else -> showAsTrayNotification(title, message, style)
            }

            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            // Headless or no display
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        show(title = title, message = "Open: $route", style = BubbleStyle.Floating)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, actions = actions, style = BubbleStyle.Floating, autoDismissMs = 0L)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        // Dismiss and re-show with new content
        try {
            val t = title ?: ""
            val m = message ?: ""
            if (floatingWindow != null) {
                dismiss()
                show(title = t, message = m, actions = actions ?: emptyList(), style = BubbleStyle.Floating)
            }
        } catch (e: Exception) { }
    }

    override fun dismiss() {
        try {
            SwingUtilities.invokeLater {
                floatingWindow?.isVisible = false
                floatingWindow?.dispose()
                floatingWindow = null
            }
            trayIcon?.let { icon ->
                try {
                    SystemTray.getSystemTray().remove(icon)
                } catch (_: Exception) {}
            }
            trayIcon = null
        } catch (_: Exception) {}
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    // ── Floating Window ─────────────────────────────────────────

    private fun showAsFloatingWindow(
        title: String,
        message: String,
        actions: List<BubbleAction>,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        SwingUtilities.invokeLater {
            // Dismiss any existing window
            floatingWindow?.dispose()

            val window = JWindow()
            window.isAlwaysOnTop = true

            val panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = Color(50, 50, 50)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(100, 100, 100), 1),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16),
                )
            }

            // Title
            val titleLabel = JLabel(title).apply {
                font = Font("SansSerif", Font.BOLD, 14)
                foreground = Color.WHITE
                alignmentX = JLabel.LEFT_ALIGNMENT
            }
            panel.add(titleLabel)

            // Message
            if (message.isNotEmpty()) {
                val messageLabel = JLabel("<html><body style='width:250px'>$message</body></html>").apply {
                    font = Font("SansSerif", Font.PLAIN, 12)
                    foreground = Color(200, 200, 200)
                    alignmentX = JLabel.LEFT_ALIGNMENT
                    border = BorderFactory.createEmptyBorder(6, 0, 0, 0)
                }
                panel.add(messageLabel)
            }

            // Action buttons
            if (actions.isNotEmpty()) {
                val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    background = Color(50, 50, 50)
                    alignmentX = JPanel.LEFT_ALIGNMENT
                    border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
                }
                actions.forEach { action ->
                    val btn = JButton(action.label).apply {
                        font = Font("SansSerif", Font.PLAIN, 11)
                        foreground = Color(100, 180, 255)
                        background = Color(70, 70, 70)
                        isBorderPainted = false
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        addActionListener {
                            action.onClick?.invoke()
                            _state.value = BubbleState.ActionTaken(action.id)
                            dismiss()
                        }
                    }
                    buttonPanel.add(btn)
                }
                panel.add(buttonPanel)
            }

            // Close on click (if no actions)
            if (actions.isEmpty()) {
                panel.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        when (onTap) {
                            is BubbleTapAction.Callback -> onTap.onTap()
                            is BubbleTapAction.Dismiss -> dismiss()
                            else -> dismiss()
                        }
                    }
                })
            }

            // Make draggable
            var dragOffset = Point()
            panel.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    dragOffset = e.point
                }
            })
            panel.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    val loc = window.location
                    window.setLocation(
                        loc.x + e.x - dragOffset.x,
                        loc.y + e.y - dragOffset.y,
                    )
                }
            })

            window.contentPane.add(panel, BorderLayout.CENTER)
            window.pack()

            // Position: bottom-right corner
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            window.setLocation(
                screenSize.width - window.width - 20,
                screenSize.height - window.height - 80,
            )

            window.isVisible = true
            floatingWindow = window

            // Auto-dismiss
            if (autoDismissMs > 0) {
                Timer(autoDismissMs.toInt()) { dismiss() }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
    }

    // ── Tray fallback ───────────────────────────────────────────

    private fun showAsTrayNotification(title: String, message: String, style: BubbleStyle) {
        try {
            if (!SystemTray.isSupported()) return
            val systemTray = SystemTray.getSystemTray()

            if (trayIcon == null) {
                val image = Toolkit.getDefaultToolkit().createImage(ByteArray(0))
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
        } catch (_: Exception) {}
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = JvmBubble(config)
