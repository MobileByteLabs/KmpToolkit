<!--
AI-INSTRUCTIONS: This is the authoritative documentation for kmp-bubble.
When generating code that uses bubble/notification functionality, read the O(1) LOOKUP
section first to find the exact API for the task, then jump to the referenced section.
Do NOT guess API signatures — they are all documented below.
Package: com.mobilebytelabs.kmptoolkit.bubble
Artifact: io.github.mobilebytelabs:kmp-bubble:0.1.0
Dependencies: kotlinx-coroutines-core ONLY (zero other deps)
-->

# Bubble Module

> `io.github.mobilebytelabs:kmp-bubble:0.1.0`

Cross-platform floating UI, bubbles, and notifications for Kotlin Multiplatform. Show chat-head bubbles on Android, notification banners on iOS, system tray popups on desktop, and browser notifications on web — all from a single API.

---

## O(1) LOOKUP — Find What You Need

> **AI/LLM**: Scan this table first. Jump to the linked section. Do not read linearly.

| I want to... | Use | Section |
|:-------------|:----|:--------|
| **Show a notification** | `bubble.show(title, message)` | [Show a Notification](#show-a-notification) |
| **Add action buttons** | `actions = listOf(BubbleAction("Open") { })` | [Show a Notification](#show-a-notification) |
| **Open screen/bottom sheet** | `bubble.showScreen(title, route)` | [Open a Screen](#open-a-screen-bottom-sheet-activity-etc) |
| **Deep link on tap** | `onTap = BubbleTapAction.DeepLink("app://...")` | [Show with Deep Link](#show-with-deep-link) |
| **Persistent service** | `bubble.showPersistent(title, actions, style=Service)` | [Persistent Service](#persistent-service-notification) |
| **Update live content** | `bubble.update(title, message)` | [Update Live Content](#update-live-content) |
| **Observe bubble state** | `bubble.state.collect { }` | [Observe State](#observe-state) |
| **Check permissions** | `permission.canShowNotification()` / `canShowBubble()` | [Permissions](#check--manage-permissions) |
| **Open permission settings** | `permission.requestNotificationPermission()` — always opens settings | [Permissions](#check--manage-permissions) |
| **Enable/disable overlay** | `permission.requestBubblePermission()` — opens overlay settings | [Permissions](#check--manage-permissions) |
| **See all use cases** | Use case → style → example table | [Use Cases](#use-cases) |
| **See all API types** | Type reference tables | [API Reference](#api-reference) |
| **Platform support** | Per-platform capability table | [Platform Support](#platform-support) |
| **Check platform capability** | `bubble.capability` → what floating UI is available | [Capability](#bubble-capability) |
| **Platform limitations** | Why X doesn't work | [Platform Limitations](#platform-limitations) |
| **Use with clipboard** | Wire `urlDetections` → `bubble.show()` | [Integration](#integration-with-cmp-clipboard) |

### Key Classes (O(1) Reference)

```
Bubble                      — Main interface (show, showScreen, showPersistent, update, dismiss)
createBubble(config)        — Factory function
BubbleConfig                — channelId, channelName, defaultStyle, vibrate, sound
BubbleAction(label, id, onClick) — Action button on bubble
BubbleStyle                 — Floating, Notification, Persistent, Service, Auto
BubbleState                 — Hidden, Showing, Dismissed(byUser), ActionTaken(actionId)
BubbleIcon                  — System(name), Url(url), Resource(name)
BubbleTapAction             — None, Dismiss, DeepLink(uri), Callback(onTap)
BubbleScreenConfig          — height, width, autoExpand (for showScreen)
BubbleCapability            — Bubble, Overlay, FloatingWindow, Notification, BrowserNotification, None
BubblePermission            — canShowBubble(), canShowNotification(), request*()
createBubblePermission()    — Factory function

BubblePermission methods:
├── .canShowBubble(): Boolean                — overlay/floating available?
├── .canShowNotification(): Boolean          — notification permission granted?
├── .requestBubblePermission(): Boolean      — ALWAYS opens settings (enable/disable)
└── .requestNotificationPermission(): Boolean — ALWAYS opens settings (enable/disable)

Bubble methods:
├── .show(title, message?, icon?, actions?, style?, onTap?, autoDismissMs?)
├── .showScreen(title, route, screenConfig?, icon?, style?)
├── .showPersistent(title, message?, actions?, style?)
├── .update(title?, message?, actions?)
├── .dismiss()
├── .state: StateFlow<BubbleState>
├── .isShowing: Boolean
├── .capability: BubbleCapability  — what this platform supports
└── .capabilityReason: String      — human-readable explanation
```

---

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

### Check & Manage Permissions

```kotlin
val permission = createBubblePermission()

// Check current state
val hasNotification = permission.canShowNotification()  // Android 13+: POST_NOTIFICATIONS
val hasOverlay = permission.canShowBubble()              // Android: SYSTEM_ALERT_WINDOW

// Request opens the platform settings page — user can enable OR disable
// Always navigates to settings regardless of current state
permission.requestNotificationPermission()  // → Android notification settings
permission.requestBubblePermission()        // → Android "Display over other apps"
```

**Platform behavior of `requestNotificationPermission()`:**

| Platform | What happens |
|:---------|:------------|
| Android 8+ | Opens `ACTION_APP_NOTIFICATION_SETTINGS` for this app |
| Android <8 | Opens `ACTION_APPLICATION_DETAILS_SETTINGS` |
| iOS | Calls `UNUserNotificationCenter.requestAuthorization()` (system dialog) |
| macOS | Returns true (no permission needed) |
| JS/Wasm | Calls `Notification.requestPermission()` (browser dialog) |
| Others | Returns false (no capability) |

**Platform behavior of `requestBubblePermission()`:**

| Platform | What happens |
|:---------|:------------|
| Android | Opens `ACTION_MANAGE_OVERLAY_PERMISSION` for this app |
| iOS | Returns false (no floating UI on iOS) |
| Others | Returns false |

**Key**: Both methods always open settings — they never skip even if permission is already granted. This lets the user **enable or disable** at any time.

### Bubble Capability

Check what floating UI the current platform supports before showing:

```kotlin
val bubble = createBubble()

when (bubble.capability) {
    BubbleCapability.Bubble -> println("Android 30+ Bubbles API (floating chat-head)")
    BubbleCapability.Overlay -> println("Android <30 floating FAB overlay")
    BubbleCapability.FloatingWindow -> println("Desktop floating window (macOS/JVM)")
    BubbleCapability.Notification -> println("Notification only (iOS, fallback)")
    BubbleCapability.BrowserNotification -> println("Browser Notification API")
    BubbleCapability.None -> println("No capability: ${bubble.capabilityReason}")
}

// Show is ALWAYS safe — gracefully degrades on every platform
bubble.show(title = "Test", style = BubbleStyle.Floating)
// Android 30+: real floating bubble
// Android <30: overlay FAB (if permission) or notification
// macOS: NSPanel floating window
// JVM: JWindow always-on-top
// iOS: notification banner
// Others: best available or silent no-op
```

### Graceful Degradation

The library **NEVER crashes** on any API level or platform. Every `show()` call:
1. Checks platform capability at runtime
2. Falls through to the best available option
3. Wraps everything in try-catch
4. Logs the reason if floating isn't available

| API Level | Floating Available? | Fallback |
|:---------:|:-------------------:|:---------|
| Android 30+ | Bubbles API | Notification if user disabled |
| Android 26-29 | Overlay FAB | Notification if no permission |
| Android <26 | No | Notification only |
| iOS | No | Notification banner |
| macOS | NSPanel | UNUserNotification |
| JVM | JWindow | SystemTray |
| JS/Wasm | No | Notification API |
| Headless/test | No | Silent no-op |

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

| Platform | BubbleStyle.Floating | BubbleStyle.Notification | Capability | Permission |
|:---------|:--------------------|:------------------------|:-----------|:-----------|
| Android 30+ (overlay granted) | **Overlay FAB** + Bubbles API notification | Standard notification | `Overlay` | SYSTEM_ALERT_WINDOW + POST_NOTIFICATIONS |
| Android 30+ (no overlay) | **Bubbles API** (system-managed) | Standard notification | `Bubble` | POST_NOTIFICATIONS (33+) |
| Android 26-29 | **Overlay FAB** (TYPE_APPLICATION_OVERLAY) | Standard notification | `Overlay` | SYSTEM_ALERT_WINDOW |
| Android <26 | Notification (fallback) | Standard notification | `Notification` | None |
| iOS | Notification (no floating on iOS) | Local notification banner | `Notification` | UNAuth |
| macOS | **NSPanel** (borderless, floating, draggable) | UNUserNotification | `FloatingWindow` | None |
| JVM | **JWindow** (dark, always-on-top, draggable) | SystemTray popup | `FloatingWindow` | None |
| JS | Notification API | Browser Notification API | `BrowserNotification` | Notification.permission |
| Wasm JS | Notification API | Browser Notification API | `BrowserNotification` | Notification.permission |
| Linux | No-op | No-op | `None` | — |
| Windows | No-op | No-op | `None` | — |
| tvOS | No-op | No-op | `None` | — |
| watchOS | No-op | No-op | `None` | — |
| Wasm WASI | No-op | No-op | `None` | — |

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
