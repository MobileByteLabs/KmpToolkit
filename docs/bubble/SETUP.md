# cmp-bubble — Integration Guide

> `io.github.mobilebytelabs:kmp-bubble:2.1.0`

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmp-bubble = "2.1.0"

[libraries]
kmp-bubble = { module = "io.github.mobilebytelabs:kmp-bubble", version.ref = "kmp-bubble" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmp.bubble)
}
```

---

## Step 2 — Create a Bubble Instance

No config class or initialization needed. Create an instance wherever you need it:

```kotlin
import com.mobilebytelabs.kmptoolkit.bubble.createBubble
import com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig

// Default
val bubble = createBubble()

// Customised
val bubble = createBubble(BubbleConfig(
    channelId   = "downloads",
    channelName = "Download Notifications",
    vibrate     = true,
    fabColor    = 0xFFE91E63,   // ARGB hex long
    fabText     = "⬇",
))
```

Recommended: create one instance per ViewModel or as a singleton in your DI graph.

---

## Step 3 — Show Bubbles

### Basic notification-style

```kotlin
bubble.show(
    title   = "Download Complete",
    message = "video.mp4 saved to gallery",
    actions = listOf(
        BubbleAction("Open") { openFile(path) },
        BubbleAction("Share") { shareFile(path) },
    ),
    autoDismissMs = 4000L,
)
```

### Open a screen in the bubble (Android chat-head / iOS deep link)

```kotlin
bubble.showScreen(
    title        = "Quick Reply",
    route        = "reply/$messageId",
    screenConfig = BubbleScreenConfig(height = 420, expandedByDefault = true),
)
```

### Persistent (ongoing notification)

```kotlin
// Show
bubble.showPersistent(
    title   = "Uploading…",
    message = "3 / 12 files",
    actions = listOf(BubbleAction("Cancel") { cancelUpload() }),
)

// Update progress
bubble.update(message = "7 / 12 files")

// Dismiss when done
bubble.dismiss()
```

---

## Step 4 — Handle Permissions (Android)

For floating FAB on Android < 30, `SYSTEM_ALERT_WINDOW` permission is needed:

```kotlin
import com.mobilebytelabs.kmptoolkit.bubble.BubblePermission

// Check
if (!BubblePermission.isGranted()) {
    BubblePermission.request()   // launches system settings
}
```

For notifications on iOS, request permission at app launch (standard iOS flow).

---

## Step 5 — Customise Style

```kotlin
// Force notification-only (no floating overlay)
bubble.show(
    title = "Alert",
    message = "Something happened",
    style = BubbleStyle.Notification,
)

// Check what the platform supports
when (bubble.capability) {
    BubbleCapability.Bubble    -> println("Native Android bubble")
    BubbleCapability.Overlay   -> println("Floating FAB")
    BubbleCapability.None      -> println("Notifications only")
    else -> {}
}
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| No floating UI on Android | `SYSTEM_ALERT_WINDOW` not granted (Android <30) or `BubbleCapability.None` | Call `BubblePermission.request()` |
| Bubble shows as notification | Android 30+ Bubble not appearing | Ensure app has notification permission + channel is set up |
| `createBubble()` unresolved | Missing import | `import com.mobilebytelabs.kmptoolkit.bubble.createBubble` |
| Actions not showing on iOS | iOS max 3 actions | Limit `actions` list to ≤ 3 items |

---

## AI-Assisted Setup

Run `/sync-bubble` in Claude Code for automated Gradle check and wiring verification:

```
/sync-bubble           # Full verify-gated sync
/sync-bubble --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
