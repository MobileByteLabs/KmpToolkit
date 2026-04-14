# Bubble Module

> `io.github.mobilebytelabs:kmp-bubble:0.1.0`

Cross-platform floating UI, bubbles, and notifications for Kotlin Multiplatform. Show chat-head bubbles on Android, notification banners on iOS, system tray popups on desktop, and browser notifications on web — all from a single API.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  ANY TRIGGER                      cmp-bubble                     │
│                                                                  │
│  Clipboard URL detected ──┐                                      │
│  Chat message received ───┤    bubble.show(                      │
│  Download completed ──────┼──→   title, message, actions         │
│  Background task done ────┤    )                                 │
│  Timer fired ─────────────┘                                      │
│                                                                  │
│  Platform auto-selects best mechanism:                           │
│  ├── Android 30+: Bubbles API (no permission needed)            │
│  ├── Android <30: Notification (fallback)                        │
│  ├── iOS: UNUserNotificationCenter local banner                  │
│  ├── macOS: UNUserNotificationCenter                             │
│  ├── JVM: SystemTray + TrayIcon                                  │
│  ├── JS/Wasm: Browser Notification API                           │
│  └── Others: No-op                                               │
└─────────────────────────────────────────────────────────────────┘
```

## Installation

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mobilebytelabs:kmp-bubble:0.1.0")
        }
    }
}
```

**Zero dependencies** beyond `kotlinx-coroutines-core`. No setup required on any platform.

## Quick Start

### Show a Notification

```kotlin
import com.mobilebytelabs.kmptoolkit.bubble.*

val bubble = createBubble()

bubble.show(
    title = "Download Complete",
    message = "video.mp4 saved to gallery",
    actions = listOf(
        BubbleAction("Open") { openFile() },
        BubbleAction("Share") { shareFile() }
    )
)
```

### Show with Deep Link

```kotlin
bubble.show(
    title = "New Message from John",
    message = "Hey, are you free?",
    icon = BubbleIcon.Url("https://example.com/avatar.jpg"),
    onTap = BubbleTapAction.DeepLink("myapp://chat/123")
)
```

### Open a Screen (Bottom Sheet, Activity, etc.)

```kotlin
// Android: Opens Activity inside Bubble (API 30+) or via deep link
// iOS: Notification tap opens deep link route
// Desktop: Tray notification with route callback
bubble.showScreen(
    title = "Quick Reply",
    route = "myapp://chat/reply/123",
    screenConfig = BubbleScreenConfig(height = 400)
)
```

### Persistent Service Notification

```kotlin
bubble.showPersistent(
    title = "Monitoring Active",
    message = "Watching for URLs",
    actions = listOf(
        BubbleAction("Pause") { pauseMonitor() },
        BubbleAction("Stop") { stopMonitor(); bubble.dismiss() }
    ),
    style = BubbleStyle.Service
)
```

### Update Live Content

```kotlin
bubble.show(title = "Downloading...", message = "0%")
// Later...
bubble.update(message = "50%")
// Later...
bubble.update(title = "Complete!", message = "100%")
```

### Observe State

```kotlin
bubble.state.collect { state ->
    when (state) {
        is BubbleState.Hidden -> println("Not visible")
        is BubbleState.Showing -> println("Visible")
        is BubbleState.Dismissed -> println("Dismissed by user: ${state.byUser}")
        is BubbleState.ActionTaken -> println("Action: ${state.actionId}")
    }
}
```

### Check Permissions

```kotlin
val permission = createBubblePermission()

if (!permission.canShowNotification()) {
    val granted = permission.requestNotificationPermission()
    if (!granted) println("Notification permission denied")
}
```

## Use Cases

| Use Case | Style | Example |
|:---------|:------|:--------|
| URL detected in clipboard | `Notification` | `bubble.show("Instagram URL Detected", url, actions=[Download, Open])` |
| Chat message received | `Floating` | `bubble.show("John", "Hey!", icon=avatar)` |
| Download completed | `Notification` | `bubble.show("Download Complete", "video.mp4", actions=[Open, Share])` |
| Background task status | `Persistent` | `bubble.showPersistent("Syncing...", "45%")` |
| Open quick reply | `Floating` | `bubble.showScreen("Reply", "app://chat/reply/123")` |
| Music mini-player | `Service` | `bubble.showPersistent("Now Playing", song, actions=[Pause, Skip])` |
| Open bottom sheet | `Floating` | `bubble.showScreen("Settings", "app://settings", screenConfig)` |

## API Reference

### Core

| Type | Description |
|:-----|:-----------|
| `Bubble` | Main interface — `show()`, `showScreen()`, `showPersistent()`, `update()`, `dismiss()` |
| `createBubble(config)` | Factory function, returns platform-specific implementation |
| `BubbleConfig` | Global config — channel ID, default style, vibrate, sound |

### Models

| Type | Description |
|:-----|:-----------|
| `BubbleAction` | Action button — `label`, `id`, `onClick` callback |
| `BubbleStyle` | `Floating`, `Notification`, `Persistent`, `Service`, `Auto` |
| `BubbleState` | `Hidden`, `Showing`, `Dismissed(byUser)`, `ActionTaken(actionId)` |
| `BubbleIcon` | `System(name)`, `Url(url)`, `Resource(name)` |
| `BubbleTapAction` | `None`, `Dismiss`, `DeepLink(uri)`, `Callback(onTap)` |
| `BubbleScreenConfig` | Screen dimensions — `height`, `width`, `autoExpand` |

### Permissions

| Type | Description |
|:-----|:-----------|
| `BubblePermission` | Permission checker — `canShowBubble()`, `canShowNotification()` |
| `createBubblePermission()` | Factory function |

## Platform Support

| Platform | Mechanism | Permission | Actions | Deep Link |
|:---------|:----------|:-----------|:-------:|:---------:|
| Android 30+ | Bubbles API / Notification | POST_NOTIFICATIONS (API 33+) | Yes | Yes |
| Android <30 | Notification | None | Yes | Yes |
| iOS | UNUserNotificationCenter | Authorization required | Yes (3 max) | Yes |
| macOS | UNUserNotificationCenter | None | Limited | Yes |
| JVM | SystemTray + TrayIcon | None (if tray supported) | No | Callback |
| JS | Browser Notification API | Notification.permission | No | Callback |
| Wasm JS | Browser Notification API | Notification.permission | No | Callback |
| Linux | No-op | — | — | — |
| Windows | No-op | — | — | — |
| tvOS | No-op | — | — | — |
| watchOS | No-op | — | — | — |
| Wasm WASI | No-op | — | — | — |

### Platform Limitations

**iOS**: No floating overlay. Bubbles show as notification banners. iOS 16+ shows a paste permission banner when reading clipboard (not related to this module). Up to 3 action buttons per notification category.

**Android <30**: Bubbles API not available. Falls back to standard notification. For floating overlay (TYPE_APPLICATION_OVERLAY), use `cmp-clipboard`'s overlay or implement custom overlay logic.

**JVM**: Requires non-headless environment with SystemTray support. Notifications are basic tray popups — no action buttons, no rich content.

**JS/Wasm**: Requires user gesture to grant `Notification.permission`. Cannot show notifications in background tabs. Limited to title + body (no action buttons in standard Notification API).

**Linux/Windows**: No standard cross-platform notification API in Kotlin/Native. Future versions may add `libnotify` (Linux) and Win32 Toast (Windows) support.

## Integration with cmp-clipboard

```kotlin
// cmp-clipboard monitor triggers cmp-bubble
val monitor = createClipboardMonitor()
val bubble = createBubble()

SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)

monitor.urlDetections.collect { detection ->
    bubble.show(
        title = "${detection.matcher.name} URL Detected",
        message = detection.url,
        actions = listOf(
            BubbleAction("Download") { startDownload(detection.url) },
            BubbleAction("Open") { openBrowser(detection.url) }
        ),
        onTap = BubbleTapAction.DeepLink("myapp://download?url=${detection.url}")
    )
}
```
