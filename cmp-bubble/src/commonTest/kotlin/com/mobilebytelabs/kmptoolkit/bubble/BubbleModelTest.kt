package com.mobilebytelabs.kmptoolkit.bubble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BubbleModelTest {

    // ── BubbleAction ────────────────────────────────────────────────

    @Test
    fun action_defaultId() {
        val action = BubbleAction("Download Now")
        assertEquals("download_now", action.id)
    }

    @Test
    fun action_customId() {
        val action = BubbleAction("Open", id = "custom_open")
        assertEquals("custom_open", action.id)
    }

    @Test
    fun action_withCallback() {
        var called = false
        val action = BubbleAction("Click") { called = true }
        action.onClick?.invoke()
        assertTrue(called)
    }

    @Test
    fun action_withoutCallback() {
        val action = BubbleAction("Dismiss")
        assertNull(action.onClick)
    }

    // ── BubbleStyle ─────────────────────────────────────────────────

    @Test
    fun style_allValues() {
        assertEquals(5, BubbleStyle.entries.size)
        assertTrue(BubbleStyle.entries.contains(BubbleStyle.Floating))
        assertTrue(BubbleStyle.entries.contains(BubbleStyle.Notification))
        assertTrue(BubbleStyle.entries.contains(BubbleStyle.Persistent))
        assertTrue(BubbleStyle.entries.contains(BubbleStyle.Service))
        assertTrue(BubbleStyle.entries.contains(BubbleStyle.Auto))
    }

    // ── BubbleState ─────────────────────────────────────────────────

    @Test
    fun state_hidden() {
        assertIs<BubbleState.Hidden>(BubbleState.Hidden)
    }

    @Test
    fun state_showing() {
        assertIs<BubbleState.Showing>(BubbleState.Showing)
    }

    @Test
    fun state_dismissedByUser() {
        val state = BubbleState.Dismissed(byUser = true)
        assertTrue(state.byUser)
    }

    @Test
    fun state_dismissedProgrammatic() {
        val state = BubbleState.Dismissed(byUser = false)
        assertFalse(state.byUser)
    }

    @Test
    fun state_actionTaken() {
        val state = BubbleState.ActionTaken(actionId = "download")
        assertEquals("download", state.actionId)
    }

    // ── BubbleIcon ──────────────────────────────────────────────────

    @Test
    fun icon_system() {
        val icon = BubbleIcon.System("download")
        assertIs<BubbleIcon.System>(icon)
        assertEquals("download", icon.name)
    }

    @Test
    fun icon_url() {
        val icon = BubbleIcon.Url("https://example.com/avatar.png")
        assertIs<BubbleIcon.Url>(icon)
        assertEquals("https://example.com/avatar.png", icon.url)
    }

    @Test
    fun icon_resource() {
        val icon = BubbleIcon.Resource("ic_bubble")
        assertIs<BubbleIcon.Resource>(icon)
        assertEquals("ic_bubble", icon.name)
    }

    // ── BubbleTapAction ─────────────────────────────────────────────

    @Test
    fun tapAction_none() {
        assertIs<BubbleTapAction.None>(BubbleTapAction.None)
    }

    @Test
    fun tapAction_dismiss() {
        assertIs<BubbleTapAction.Dismiss>(BubbleTapAction.Dismiss)
    }

    @Test
    fun tapAction_deepLink() {
        val action = BubbleTapAction.DeepLink("myapp://chat/123")
        assertEquals("myapp://chat/123", action.uri)
    }

    @Test
    fun tapAction_callback() {
        var called = false
        val action = BubbleTapAction.Callback { called = true }
        action.onTap()
        assertTrue(called)
    }

    // ── BubbleScreenConfig ──────────────────────────────────────────

    @Test
    fun screenConfig_defaults() {
        val config = BubbleScreenConfig.Default
        assertEquals(400, config.height)
        assertEquals(BubbleScreenConfig.MATCH_PARENT, config.width)
        assertTrue(config.autoExpand)
    }

    @Test
    fun screenConfig_custom() {
        val config = BubbleScreenConfig(height = 600, width = 300, autoExpand = false)
        assertEquals(600, config.height)
        assertEquals(300, config.width)
        assertFalse(config.autoExpand)
    }

    // ── BubbleConfig ────────────────────────────────────────────────

    @Test
    fun config_defaults() {
        val config = BubbleConfig.Default
        assertEquals(BubbleStyle.Auto, config.defaultStyle)
        assertEquals("kmptoolkit_bubble", config.channelId)
        assertEquals("Bubbles", config.channelName)
        assertFalse(config.vibrate)
        assertFalse(config.sound)
    }

    @Test
    fun config_custom() {
        val config = BubbleConfig(
            defaultStyle = BubbleStyle.Floating,
            channelId = "my_channel",
            channelName = "My Bubbles",
            vibrate = true,
            sound = true,
        )
        assertEquals(BubbleStyle.Floating, config.defaultStyle)
        assertEquals("my_channel", config.channelId)
        assertTrue(config.vibrate)
        assertTrue(config.sound)
    }
}
