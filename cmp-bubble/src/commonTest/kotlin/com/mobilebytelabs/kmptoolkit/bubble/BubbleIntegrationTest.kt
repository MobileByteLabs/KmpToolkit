package com.mobilebytelabs.kmptoolkit.bubble

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BubbleIntegrationTest {

    // ── Factory ─────────────────────────────────────────────────────

    @Test
    fun createBubble_returnsInstance() {
        val bubble = createBubble()
        assertNotNull(bubble)
    }

    @Test
    fun createBubble_withConfig() {
        val bubble = createBubble(
            BubbleConfig(
                defaultStyle = BubbleStyle.Notification,
                vibrate = true,
            ),
        )
        assertNotNull(bubble)
    }

    // ── Initial State ───────────────────────────────────────────────

    @Test
    fun bubble_initialStateIsHidden() {
        val bubble = createBubble()
        assertIs<BubbleState.Hidden>(bubble.state.value)
    }

    @Test
    fun bubble_initiallyNotShowing() {
        val bubble = createBubble()
        assertFalse(bubble.isShowing)
    }

    // ── Show / Dismiss ──────────────────────────────────────────────

    @Test
    fun bubble_show_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(title = "Test", message = "Hello")
        // Platform may or may not show — we just verify no crash
    }

    @Test
    fun bubble_showWithActions_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(
            title = "URL Detected",
            message = "https://instagram.com/reel/123",
            actions = listOf(
                BubbleAction("Download"),
                BubbleAction("Open"),
                BubbleAction("Dismiss"),
            ),
            style = BubbleStyle.Notification,
        )
    }

    @Test
    fun bubble_showWithDeepLink_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(
            title = "New Message",
            message = "Hey!",
            onTap = BubbleTapAction.DeepLink("myapp://chat/123"),
        )
    }

    @Test
    fun bubble_showWithCallback_doesNotThrow() {
        var tapped = false
        val bubble = createBubble()
        bubble.show(
            title = "Tap me",
            onTap = BubbleTapAction.Callback { tapped = true },
        )
    }

    @Test
    fun bubble_showWithIcon_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(
            title = "Download Complete",
            icon = BubbleIcon.System("download"),
        )
    }

    @Test
    fun bubble_showWithAutoDismiss_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(
            title = "Quick Note",
            autoDismissMs = 3000L,
        )
    }

    @Test
    fun bubble_dismiss_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(title = "Test")
        bubble.dismiss()
    }

    @Test
    fun bubble_dismissWithoutShow_doesNotThrow() {
        val bubble = createBubble()
        bubble.dismiss() // should be safe even without showing
    }

    // ── Show Screen ─────────────────────────────────────────────────

    @Test
    fun bubble_showScreen_doesNotThrow() {
        val bubble = createBubble()
        bubble.showScreen(
            title = "Quick Reply",
            route = "chat/reply/123",
        )
    }

    @Test
    fun bubble_showScreen_withConfig_doesNotThrow() {
        val bubble = createBubble()
        bubble.showScreen(
            title = "Settings",
            route = "settings/notifications",
            screenConfig = BubbleScreenConfig(height = 600, width = 400),
            icon = BubbleIcon.System("settings"),
        )
    }

    // ── Show Persistent ─────────────────────────────────────────────

    @Test
    fun bubble_showPersistent_doesNotThrow() {
        val bubble = createBubble()
        bubble.showPersistent(
            title = "Monitoring Active",
            message = "Watching clipboard",
            actions = listOf(
                BubbleAction("Pause"),
                BubbleAction("Stop"),
            ),
        )
    }

    @Test
    fun bubble_showPersistent_serviceStyle_doesNotThrow() {
        val bubble = createBubble()
        bubble.showPersistent(
            title = "Service Running",
            style = BubbleStyle.Service,
        )
    }

    // ── Update ──────────────────────────────────────────────────────

    @Test
    fun bubble_update_doesNotThrow() {
        val bubble = createBubble()
        bubble.show(title = "Original")
        bubble.update(title = "Updated", message = "New message")
    }

    @Test
    fun bubble_update_withoutShow_doesNotThrow() {
        val bubble = createBubble()
        bubble.update(title = "Won't show") // safe even without prior show
    }

    // ── Permission ──────────────────────────────────────────────────

    @Test
    fun permission_canBeCreated() {
        val permission = createBubblePermission()
        assertNotNull(permission)
    }

    @Test
    fun permission_canShowBubble_doesNotThrow() {
        val permission = createBubblePermission()
        val result = permission.canShowBubble()
        assertNotNull(result.toString())
    }

    @Test
    fun permission_canShowNotification_doesNotThrow() {
        val permission = createBubblePermission()
        val result = permission.canShowNotification()
        assertNotNull(result.toString())
    }

    // ── Multiple Bubbles ────────────────────────────────────────────

    @Test
    fun multipleBubbles_canCoexist() {
        val bubble1 = createBubble()
        val bubble2 = createBubble(BubbleConfig(channelId = "channel2"))
        bubble1.show(title = "Bubble 1")
        bubble2.show(title = "Bubble 2")
        bubble1.dismiss()
        bubble2.dismiss()
    }

    // ── Full Lifecycle ──────────────────────────────────────────────

    @Test
    fun bubble_fullLifecycle() {
        val bubble = createBubble()

        // Show
        bubble.show(
            title = "Test",
            message = "Full lifecycle",
            actions = listOf(BubbleAction("OK")),
            style = BubbleStyle.Auto,
        )

        // Update
        bubble.update(title = "Updated", message = "Changed")

        // Dismiss
        bubble.dismiss()
    }
}
