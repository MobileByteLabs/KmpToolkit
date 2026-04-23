# cmp-bubble

Cross-platform floating UI, bubbles, and notifications for Kotlin Multiplatform.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-bubble.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-bubble)

---

## What It Does

Show chat-head bubbles, notification banners, floating overlays, and persistent UI from a single
shared API — using the best native mechanism on each platform:

| Platform | Implementation |
|----------|---------------|
| Android 30+ | Bubbles API (native chat-head) |
| Android <30 | TYPE_APPLICATION_OVERLAY floating FAB |
| iOS | Local notification banners with action buttons |
| macOS | NSUserNotificationCenter |
| JVM | System tray notifications |
| JS / Wasm | Browser Notification API |
| Linux / Windows | Native notification |

---

## Quick Start

```kotlin
// 1. Create bubble
val bubble = createBubble()

// 2. Show notification-style bubble
bubble.show(
    title = "Download Complete",
    message = "video.mp4 saved",
    actions = listOf(
        BubbleAction("Open") { openFile() },
        BubbleAction("Share") { shareFile() },
    )
)

// 3. Open screen inside bubble (Android: chat-head, iOS: deep link)
bubble.showScreen(
    title = "Quick Reply",
    route = "chat/reply/$messageId",
    screenConfig = BubbleScreenConfig(height = 400),
)

// 4. Show persistent (ongoing notification / foreground service)
bubble.showPersistent(
    title = "Downloading…",
    message = "42% complete",
    actions = listOf(BubbleAction("Cancel") { cancel() }),
)
```

---

## API Reference

### Create

```kotlin
// Default config
val bubble = createBubble()

// Custom config
val bubble = createBubble(BubbleConfig(
    channelId   = "my_channel",
    channelName = "My Notifications",
    vibrate     = true,
    fabColor    = 0xFF6200EE,  // ARGB long
))
```

### Show

```kotlin
bubble.show(
    title         = "Title",
    message       = "Body text",
    icon          = null,                         // BubbleIcon? — optional
    actions       = listOf(BubbleAction("OK") {}),// up to 3 on iOS
    style         = BubbleStyle.Auto,             // Auto, Floating, Notification, Persistent, Service
    onTap         = BubbleTapAction.None,         // None, OpenRoute, Custom
    autoDismissMs = 3000L,                        // 0 = no auto-dismiss
)
```

### showScreen

```kotlin
bubble.showScreen(
    title        = "Bubble Title",
    route        = "chat/reply/42",
    screenConfig = BubbleScreenConfig.Default,    // height, width, expandedByDefault
    icon         = null,
    style        = BubbleStyle.Floating,
)
```

### showPersistent

```kotlin
bubble.showPersistent(
    title   = "Running…",
    message = "Background task active",
    actions = listOf(BubbleAction("Stop") { stop() }),
    style   = BubbleStyle.Persistent,             // Persistent or Service
)
```

### Lifecycle

```kotlin
bubble.update(title = "Updated!", message = null)   // null keeps current value
bubble.dismiss()
val isShowing: Boolean = bubble.isShowing
val state: BubbleState = bubble.state.value         // StateFlow
```

---

## BubbleStyle

| Style | Behaviour |
|-------|-----------|
| `Auto` | Best available — Bubble on Android 30+, FAB below, Notification elsewhere |
| `Floating` | Force floating UI (FAB overlay on Android <30) |
| `Notification` | Force notification-only (no floating) |
| `Persistent` | Ongoing notification — stays until dismissed |
| `Service` | Foreground service notification (Android) |

---

## BubbleCapability

Check what the current platform can do before showing:

```kotlin
when (bubble.capability) {
    BubbleCapability.Bubble         -> // Android 30+ real bubble
    BubbleCapability.Overlay        -> // Android <30 FAB
    BubbleCapability.FloatingWindow -> // macOS/JVM window
    BubbleCapability.Notification   -> // notification only
    BubbleCapability.None           -> // nothing available
}
```

---

## BubbleConfig Presets

```kotlin
BubbleConfig.Default   // purple FAB, no vibrate
BubbleConfig.Download  // pink FAB, download icon
BubbleConfig.Chat      // blue FAB, chat icon
```

---

## Permissions

**Android**: For `BubbleStyle.Floating` on Android <30, the library requests
`SYSTEM_ALERT_WINDOW`. Use `BubblePermission.request()` to check / request it.

**iOS**: Notification permission must be requested at app launch.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-bubble`
