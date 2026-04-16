<!--
⚠️ REDIRECT NOTICE — This file is kept for backwards compatibility.
Current docs have moved to docs/clipboard/
  → README:  docs/clipboard/README.md
  → Setup:   docs/clipboard/SETUP.md
  → AI sync: docs/clipboard/CLAUDE_AI_SETUP.md

Current artifact: io.github.mobilebytelabs:kmp-clipboard:2.1.0
Current package:  com.mobilebytelabs.kmptoolkit.clipboard
-->

> **Docs moved** → [`docs/clipboard/README.md`](clipboard/README.md) | [`SETUP.md`](clipboard/SETUP.md)
> Current version: **2.1.0** (this file references 0.2.0 — use the new docs)

---

# Clipboard Module (Legacy — see docs/clipboard/)

> `io.github.mobilebytelabs:kmp-clipboard:0.2.0` ← stale, current is 2.1.0

A cross-platform clipboard library for Kotlin Multiplatform. Copy, paste, observe, monitor with URL detection, clipboard history, async operations — all from a single unified `ClipboardManager` API or individual lower-level APIs.

---

## O(1) LOOKUP — Find What You Need

> **AI/LLM**: Scan this table first. Jump to the linked section. Do not read linearly.

| I want to... | Use | Section |
|:-------------|:----|:--------|
| **Copy/paste (simplest)** | `ClipboardManager().copy(text)` / `.paste()` | [Quick Start](#quick-start) |
| **Observe clipboard changes** | `clipboard.content` (StateFlow) | [Quick Start](#quick-start) |
| **Keep clipboard history** | `clipboard.history` (StateFlow, configurable max) | [Full Featured](#full-featured-url-detection--history--async) |
| **Detect social media URLs** | `ClipboardManagerConfig.Full` + `clipboard.urlDetections` | [Full Featured](#full-featured-url-detection--history--async) |
| **Async copy/read (JS/Wasm)** | `clipboard.copyAsync()` / `.pasteAsync()` | [Full Featured](#full-featured-url-detection--history--async) |
| **Use in Compose** | `remember { ClipboardManager() }` + `DisposableEffect` | [Compose Usage](#compose-usage) |
| **Use in ViewModel (MVI)** | `ClipboardViewModel` + `onAction()` pattern | [ViewModel](#viewmodel-mvi--recommended-for-production) |
| **Choose Compose vs ViewModel** | See comparison table | [When to Use Which](#when-to-use-which-pattern) |
| **InSaver-style service** | `ClipboardManagerConfig.SocialMediaDownloader` | [Config Presets](#config-presets) |
| **Use lower-level APIs** | `createClipboardMonitor()`, `createClipboardObserver()` | [Lower-Level APIs](#lower-level-apis) |
| **Android foreground service** | `showNotification = true` in config | [Android: Foreground Service](#android-foreground-service--floating-fab) |
| **Android WorkManager** | `createClipboardWorkerTrigger()` | [Android: WorkManager](#android-workmanager-integration) |
| **Custom URL matchers** | `RegexUrlMatcher(name, patterns)` | [Custom URL Matchers](#custom-url-matchers) |
| **Filter clipboard content** | `ClipboardFilter.urlOnly()`, `.minLength()`, `.exclude()` | [With Filters](#with-filters) |
| **Check permissions** | `clipboard.hasNotificationPermission()` | [Permissions](#permissions) |
| **See all API types** | Type reference tables | [API Reference](#api-reference) |
| **Platform support matrix** | Per-platform capability table | [Platform Support](#platform-support) |
| **Why platform X doesn't work** | Per-platform explanation | [Platform Limitations](#platform-limitations--explanations) |
| **Android manifest** | Auto-merged permissions | [Android Manifest](#android-manifest-auto-merged) |
| **Migrate from v0.1.0** | Backward-compatible additions list | [Migration](#migration-from-v010) |

### Key Classes (O(1) Reference)

```
ClipboardManager            — Unified API (recommended entry point)
ClipboardManagerConfig      — Config: .Default, .Full, .SocialMediaDownloader
├── observe: Boolean        — enable auto-observation (StateFlow)
├── historySize: Int        — clipboard history entries (0 = disabled)
├── detectUrls: Boolean     — enable URL pattern matching
├── urlMatchers: List       — SocialMediaUrlMatchers.all() for 10 platforms
├── filters: List           — ClipboardFilter.urlOnly(), .minLength(), etc.
├── showNotification: Bool  — Android ForegroundService notification
├── showOverlay: Boolean    — Android floating FAB overlay
├── triggerWorkerOnUrl: Bool — auto background worker on URL match
└── autoClearAfterReadMs: Long — auto-clear clipboard (0 = disabled, 30000 = 30s)

Config Presets (pick one, or customize):
├── .Minimal               — sync copy/paste only, no observe, no history
├── .Default               — observe + history(20)
├── .ChatMonitor           — observe + history(50), for messaging apps
├── .LinkCollector         — URL detect + history(100), silent (no notification)
├── .SecureClipboard       — observe + auto-clear 30s, no history (password managers)
├── .SocialMediaDownloader — URL detect + notification + FAB + worker (InSaver-style)
├── .Developer             — everything + history(100) + fast polling (debug tools)
└── .Full                  — everything except notification/overlay

ClipboardManager methods:
├── .copy(text): Boolean              — sync copy
├── .paste(): String?                 — sync read (null on JS/Wasm)
├── .copyAsync(text): Boolean         — suspend copy (all platforms)
├── .pasteAsync(): String?            — suspend read (all platforms)
├── .content: StateFlow<String?>      — auto-observing current content
├── .history: StateFlow<List<Entry>>  — clipboard history (newest first)
├── .urlDetections: SharedFlow<Det>   — URL match events
├── .changes: SharedFlow<Change>      — all clipboard change events
├── .monitorState: StateFlow<State>   — Idle/Monitoring/Paused/Error
├── .start() / .stop()               — lifecycle
├── .pause() / .resume()             — monitor control
├── .copyFromHistory(entry)           — re-copy from history
├── .clearHistory()                   — clear all history
├── .addUrlMatcher(matcher)           — add URL detector
└── .addFilter(filter)                — add content filter
```

---

## ClipboardManager — Unified API (Recommended)

The simplest way to use all clipboard features. One class, one `start()`, everything works.

### Quick Start

```kotlin
val clipboard = ClipboardManager()
clipboard.start()

// Sync operations
clipboard.copy("Hello!")
val text = clipboard.paste()

// Auto-observing content (StateFlow)
clipboard.content.collect { println("Clipboard: $it") }

// Clipboard history (newest first)
clipboard.history.collect { entries -> println("${entries.size} items") }

clipboard.stop()
```

### Full Featured (URL Detection + History + Async)

```kotlin
val clipboard = ClipboardManager(ClipboardManagerConfig.Full)
clipboard.start()

// Everything works through one object:
clipboard.copy("https://instagram.com/reel/123") // sync copy
clipboard.copyAsync("text")                       // async copy (JS/Wasm)
clipboard.paste()                                  // sync read
clipboard.pasteAsync()                            // async read (JS/Wasm)
clipboard.content                                  // StateFlow<String?> (auto-observing)
clipboard.history                                  // StateFlow<List<ClipboardHistoryEntry>>
clipboard.urlDetections                           // SharedFlow<UrlDetection>
clipboard.changes                                 // SharedFlow<ClipboardChange>
clipboard.latestChange                            // StateFlow<ClipboardChange?>
clipboard.monitorState                            // StateFlow<ClipboardMonitorState>

// History operations
clipboard.copyFromHistory(entry)                  // re-copy from history
clipboard.removeFromHistory(entry)                // remove entry
clipboard.clearHistory()                          // clear all

// URL matchers (added via config or manually)
clipboard.addUrlMatcher(SocialMediaUrlMatchers.instagram())
clipboard.addAllSocialMediaMatchers()
clipboard.addFilter(ClipboardFilter.urlOnly())

clipboard.stop()
```

### Config Presets

```kotlin
// Minimal — observe + history
ClipboardManager(ClipboardManagerConfig.Default)

// Full featured — observe + history + URL detection + async
ClipboardManager(ClipboardManagerConfig.Full)

// Social media downloader — InSaver-style
ClipboardManager(ClipboardManagerConfig.SocialMediaDownloader)

// Custom
ClipboardManager(ClipboardManagerConfig(
    observe = true,
    historySize = 50,
    detectUrls = true,
    urlMatchers = SocialMediaUrlMatchers.all(),
    filters = listOf(ClipboardFilter.urlOnly()),
    showNotification = true,
    showOverlay = true,
))
```

### Compose Usage

```kotlin
@Composable
fun MyScreen() {
    val clipboard = remember { ClipboardManager(ClipboardManagerConfig.Full) }

    DisposableEffect(clipboard) {
        clipboard.start()
        onDispose { clipboard.stop() }
    }

    val content by clipboard.content.collectAsState()
    val history by clipboard.history.collectAsState()

    Column {
        Text("Clipboard: ${content ?: "empty"}")
        Text("History: ${history.size} items")

        Button(onClick = { clipboard.copy("Hello!") }) {
            Text("Copy")
        }
    }
}
```

### ViewModel (MVI — Recommended for Production)

```kotlin
// ── State ─────────────────────────────────────────────
data class ClipboardUiState(
    val currentContent: String? = null,
    val history: List<ClipboardHistoryEntry> = emptyList(),
    val monitorState: ClipboardMonitorState = ClipboardMonitorState.Idle,
    val urlDetections: List<String> = emptyList(),
    val statusMessage: String = "",
)

// ── Actions ───────────────────────────────────────────
sealed class ClipboardAction {
    data class Copy(val text: String) : ClipboardAction()
    data class CopyAsync(val text: String) : ClipboardAction()
    data object PasteAsync : ClipboardAction()
    data class CopyFromHistory(val entry: ClipboardHistoryEntry) : ClipboardAction()
    data class RemoveFromHistory(val entry: ClipboardHistoryEntry) : ClipboardAction()
    data object ClearHistory : ClipboardAction()
    data object Pause : ClipboardAction()
    data object Resume : ClipboardAction()
}

// ── ViewModel ─────────────────────────────────────────
class ClipboardViewModel : ViewModel() {

    private val clipboard = ClipboardManager(ClipboardManagerConfig.Full)

    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

    init {
        clipboard.start()

        // All flows → single UiState
        viewModelScope.launch {
            clipboard.content.collect { content ->
                _uiState.update { it.copy(currentContent = content) }
            }
        }
        viewModelScope.launch {
            clipboard.history.collect { entries ->
                _uiState.update { it.copy(history = entries) }
            }
        }
        viewModelScope.launch {
            clipboard.monitorState.collect { state ->
                _uiState.update { it.copy(monitorState = state) }
            }
        }
        viewModelScope.launch {
            clipboard.urlDetections.collect { detection ->
                _uiState.update { state ->
                    val entry = "${detection.matcher.name}: ${detection.url}"
                    state.copy(urlDetections = (listOf(entry) + state.urlDetections).take(10))
                }
            }
        }
    }

    fun onAction(action: ClipboardAction) {
        when (action) {
            is ClipboardAction.Copy -> {
                val success = clipboard.copy(action.text)
                _uiState.update { it.copy(statusMessage = if (success) "Copied!" else "Failed") }
            }
            is ClipboardAction.CopyAsync -> viewModelScope.launch {
                clipboard.copyAsync(action.text)
                _uiState.update { it.copy(statusMessage = "Async copied!") }
            }
            is ClipboardAction.PasteAsync -> viewModelScope.launch {
                val text = clipboard.pasteAsync()
                _uiState.update { it.copy(statusMessage = "Read: ${text ?: "empty"}") }
            }
            is ClipboardAction.CopyFromHistory -> clipboard.copyFromHistory(action.entry)
            is ClipboardAction.RemoveFromHistory -> clipboard.removeFromHistory(action.entry)
            is ClipboardAction.ClearHistory -> clipboard.clearHistory()
            is ClipboardAction.Pause -> clipboard.pause()
            is ClipboardAction.Resume -> clipboard.resume()
        }
    }

    override fun onCleared() {
        clipboard.stop()
    }
}

// ── Screen ────────────────────────────────────────────
@Composable
fun ClipboardScreen(viewModel: ClipboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column {
        Text("Clipboard: ${state.currentContent ?: "empty"}")

        Button(onClick = { viewModel.onAction(ClipboardAction.Copy("Hello!")) }) {
            Text("Copy")
        }

        // History
        state.history.forEach { entry ->
            Row {
                Text(entry.content, Modifier.weight(1f))
                TextButton(onClick = {
                    viewModel.onAction(ClipboardAction.CopyFromHistory(entry))
                }) { Text("Copy") }
            }
        }

        // URL detections
        state.urlDetections.forEach { Text(it) }
    }
}
```

### When to Use Which Pattern

| Pattern | ClipboardManager lives in | Lifecycle | Best for |
|:--------|:------------------------|:----------|:---------|
| Compose direct | `remember { }` + `DisposableEffect` | Tied to composable | Simple screens, prototypes |
| ViewModel | ViewModel field | Survives rotation, `onCleared()` stops | Production apps, shared state |

The `ClipboardManager` itself is framework-agnostic — it exposes `StateFlow`/`SharedFlow`. Whether you collect in Compose directly or pipe through ViewModel `UiState`, the API is the same.

---

## Lower-Level APIs

If you only need specific features, use the individual APIs directly:

| API | Use Case |
|:----|:---------|
| `copyToClipboard()` / `getFromClipboard()` | Simple sync copy/paste |
| `copyToClipboardAsync()` / `getFromClipboardAsync()` | Async (JS/Wasm support) |
| `createClipboardObserver()` | Auto-detect changes via StateFlow |
| `createClipboardMonitor()` | Continuous monitoring + URL detection |
| `createClipboardHistory()` | Keep N clipboard entries |
| `createClipboardPermission()` | Check/request permissions |

---

## Clipboard Monitor (Lower-Level)

For advanced monitoring with direct control over the service lifecycle:

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIPBOARD MONITOR PIPELINE                    │
│                                                                  │
│  Clipboard Change → Filters → URL Matchers → Emission           │
│       │                                          │               │
│       │                              ┌───────────┴──────────┐   │
│       ▼                              ▼                      ▼   │
│  ClipboardChange              UrlDetection            Worker    │
│  (SharedFlow)                 (SharedFlow)           Trigger   │
│                                                                  │
│  Platform Implementations:                                       │
│  ├── Android: ForegroundService + FAB Overlay + WorkManager     │
│  ├── iOS:     NSTimer + UIPasteboard.changeCount                │
│  ├── macOS:   NSTimer + NSPasteboard.changeCount                │
│  ├── JVM:     FlavorListener + Daemon Thread                    │
│  ├── JS:      navigator.clipboard + visibilitychange            │
│  ├── Wasm:    navigator.clipboard async + polling               │
│  ├── Linux:   xclip polling via coroutines                      │
│  └── Windows: Win32 clipboard polling via coroutines            │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Basic Monitoring

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.*
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.*

val monitor = createClipboardMonitor()
monitor.start()

// Collect clipboard changes
monitor.changes.collect { change ->
    println("Clipboard: ${change.content}")
    println("Type: ${change.contentType}")  // Text, Uri, Html, Image
    println("From: ${change.source}")        // Internal, External
}

monitor.stop()
```

### Social Media URL Detection (InSaver-style)

```kotlin
val monitor = createClipboardMonitor()

// Add built-in social media URL matchers
SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }

// Start with social media downloader preset
monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)

// React to URL detections
monitor.urlDetections.collect { detection ->
    println("Detected ${detection.matcher.name} URL: ${detection.url}")
    // → "Detected Instagram URL: https://www.instagram.com/reel/ABC123/"
    // → "Detected TikTok URL: https://vm.tiktok.com/ZMF2abc/"
}
```

### With Filters

```kotlin
val monitor = createClipboardMonitor()

// Only process URLs (skip plain text)
monitor.addFilter(ClipboardFilter.urlOnly())

// Minimum content length
monitor.addFilter(ClipboardFilter.minLength(10))

// Exclude sensitive content
monitor.addFilter(ClipboardFilter.exclude(
    Regex("password", RegexOption.IGNORE_CASE),
    Regex("secret", RegexOption.IGNORE_CASE)
))

monitor.addUrlMatcher(SocialMediaUrlMatchers.instagram())
monitor.start()
```

### Android: Foreground Service + Floating FAB

```kotlin
// On Android, the monitor automatically:
// 1. Starts a ForegroundService with persistent notification
// 2. Shows "Clipboard Monitor Service - Monitoring clipboard changes"
// 3. Notification has [Pause] and [Stop] actions

val monitor = createClipboardMonitor()
SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }

// Enable floating FAB overlay
monitor.start(ClipboardMonitorConfig(
    showNotification = true,     // Persistent notification
    showOverlay = true,          // Floating FAB on screen
    triggerWorkerOnUrl = true,   // Auto-trigger background worker
    notificationTitle = "Clipboard Monitor Service",
    notificationText = "Monitoring clipboard changes"
))

// Pause/resume from code
monitor.pause()   // Notification shows "Paused"
monitor.resume()  // Back to monitoring
```

### Android: WorkManager Integration

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.worker.*

// Set up worker trigger handler
val trigger = createClipboardWorkerTrigger() as AndroidClipboardWorkerTrigger
trigger.urlHandler = { url, matcher, change ->
    // Enqueue your download worker
    val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(workDataOf(
            "url" to url,
            "source" to matcher.name
        ))
        .build()
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

### Android: Auto-Start on Boot

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardBootReceiver

// Enable auto-start after device boot
ClipboardBootReceiver.setAutoStartEnabled(context, true)
```

### Async Clipboard Operations

```kotlin
// Works on ALL platforms including JS/Wasm (where sync read isn't possible)
val text = getFromClipboardAsync()
text?.let { println("Clipboard: $it") }

val success = copyToClipboardAsync("Hello!")
val hasText = hasClipboardTextAsync()
```

### Permissions

```kotlin
val permission = createClipboardPermission()

// Check permissions
if (!permission.hasOverlayPermission()) {
    permission.requestOverlayPermission() // Opens Android Settings
}

if (!permission.hasNotificationPermission()) {
    permission.requestNotificationPermission() // Android 13+ runtime permission
}
```

### Custom URL Matchers

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.RegexUrlMatcher

// Create matcher for your own service
val myMatcher = RegexUrlMatcher(
    name = "MyService",
    patterns = listOf(
        Regex("https?://myservice\\.com/video/\\d+"),
        Regex("https?://my\\.svc/[\\w]+")
    )
)

monitor.addUrlMatcher(myMatcher)
```

## API Reference

### Core Types

| Type | Description |
|:-----|:-----------|
| `ClipboardMonitor` | Main monitoring interface — start/stop/pause/resume |
| `ClipboardChange` | Change event — content, type, timestamp, source |
| `ClipboardContentType` | Enum: `Text`, `Uri`, `Html`, `Image`, `Unknown` |
| `ClipboardSource` | Enum: `Internal`, `External`, `Unknown` |
| `UrlDetection` | URL match result — url, matcher, change |

### Configuration

| Type | Description |
|:-----|:-----------|
| `ClipboardMonitorConfig` | Polling interval, notification, overlay, worker trigger |
| `ClipboardMonitorConfig.Default` | Basic monitoring with notification |
| `ClipboardMonitorConfig.SocialMediaDownloader` | URL detection + overlay + worker |
| `ClipboardOverlayConfig` | FAB position, drag, auto-dismiss |

### Filters & Matchers

| Type | Description |
|:-----|:-----------|
| `ClipboardFilter` | Interface — `shouldProcess(content): Boolean` |
| `ClipboardFilter.urlOnly()` | Only process HTTP/HTTPS URLs |
| `ClipboardFilter.minLength(n)` | Minimum content length |
| `ClipboardFilter.maxLength(n)` | Maximum content length |
| `ClipboardFilter.exclude(vararg Regex)` | Exclude matching patterns |
| `ClipboardUrlMatcher` | Interface — `matches(content)`, `extractUrl(content)` |
| `SocialMediaUrlMatchers` | Built-in matchers for 10 platforms |
| `RegexUrlMatcher` | Default implementation with regex patterns |

### Built-in Social Media Matchers

| Matcher | Example URLs |
|:--------|:------------|
| `instagram()` | `instagram.com/reel/...`, `instagr.am/...` |
| `tiktok()` | `tiktok.com/@user/video/...`, `vm.tiktok.com/...` |
| `youtube()` | `youtube.com/watch?v=...`, `youtu.be/...`, `youtube.com/shorts/...` |
| `twitter()` | `twitter.com/.../status/...`, `x.com/...`, `t.co/...` |
| `facebook()` | `facebook.com/watch/...`, `facebook.com/reel/...`, `fb.watch/...` |
| `snapchat()` | `snapchat.com/spotlight/...`, `t.snapchat.com/...` |
| `pinterest()` | `pinterest.com/pin/...`, `pin.it/...` |
| `reddit()` | `reddit.com/r/.../comments/...`, `redd.it/...` |
| `linkedin()` | `linkedin.com/posts/...`, `linkedin.com/feed/update/...` |
| `threads()` | `threads.net/@user/post/...`, `threads.net/t/...` |

### Permissions

| Type | Description |
|:-----|:-----------|
| `ClipboardPermission` | Check/request clipboard, overlay, notification permissions |
| `hasClipboardAccess()` | Clipboard read/write access |
| `hasOverlayPermission()` | Android: `SYSTEM_ALERT_WINDOW` |
| `hasNotificationPermission()` | Android 13+: `POST_NOTIFICATIONS` |

### Async Operations

| Function | Description |
|:---------|:-----------|
| `getFromClipboardAsync()` | Suspend clipboard read (works on JS/Wasm) |
| `copyToClipboardAsync(text)` | Suspend clipboard write |
| `hasClipboardTextAsync()` | Suspend clipboard check |

## Platform Support

| Platform | Monitor | Overlay | Worker | Async | URL Match | Method |
|:---------|:-------:|:-------:|:------:|:-----:|:---------:|:-------|
| Android | Full | FAB | WorkManager | Yes | Yes | ForegroundService |
| iOS | Foreground | Notification | BGTask | Yes | Yes | NSTimer + changeCount |
| macOS | Full | Menu Bar | Yes | Yes | Yes | NSTimer + changeCount |
| JVM | Full | System Tray | Yes | Yes | Yes | FlavorListener + Thread |
| JS | Foreground | DOM | Limited | Yes | Yes | Clipboard API + visibility |
| Wasm JS | Foreground | — | Limited | Yes | Yes | Clipboard API async |
| Linux | Full | — | Yes | Yes | Yes | xclip polling |
| Windows | Full | — | Yes | Yes | Yes | Win32 polling |
| tvOS | No | No | No | No | No | No clipboard |
| watchOS | No | No | No | No | No | No clipboard |
| Wasm WASI | No | No | No | No | No | Sandboxed |

### Platform Limitations & Explanations

#### iOS — Foreground Only

iOS does not allow persistent background services like Android. Clipboard monitoring works through two mechanisms:

1. **`UIPasteboard.changeCount` polling** — An `NSTimer` periodically checks if the pasteboard change count has incremented. This only works while the app is in the foreground.
2. **`UIApplicationDidBecomeActiveNotification`** — When the user switches back to your app, the library immediately reads the clipboard and compares against the last known content.

Starting with **iOS 16**, Apple introduced a system paste permission banner. Every time your app reads `UIPasteboard.generalPasteboard.string`, the user sees _"[App] would like to paste from [Source App]"_ and must tap **Allow Paste**. There is no way to suppress this — your app's UX must account for it (e.g., explain why clipboard access is needed before the banner appears).

There is no floating overlay on iOS. Apple does not provide a `SYSTEM_ALERT_WINDOW` equivalent. URL detections are surfaced via `SharedFlow` only — your app can show a local notification via `UNUserNotificationCenter` if needed.

#### macOS — Full Support

macOS uses `NSPasteboard.generalPasteboard.changeCount` polling via `NSTimer`, combined with `NSApplicationDidBecomeActiveNotification` for focus-based detection. Unlike iOS, macOS does not show a paste permission banner, so clipboard reads are silent. Full continuous background monitoring is supported as macOS apps are not suspended when backgrounded.

#### JVM (Desktop) — Full Support

Uses `java.awt.datatransfer.FlavorListener` for native clipboard change events, plus a daemon polling thread as a reliability backup (FlavorListener can be unreliable on some Linux window managers). Requires a non-headless environment — in headless/server JVMs the monitor will start but won't detect changes since there is no system clipboard.

#### JavaScript (Browser) — Foreground Only, Permission Required

Browser clipboard monitoring has strict limitations imposed by web security:

1. **`navigator.clipboard.readText()`** requires either a user gesture (click/tap) or an explicit Permissions API grant. Periodic polling will fail silently if permission hasn't been granted.
2. **`document.visibilitychange`** and `window.focus` events are used to detect when the user returns to the tab, triggering a clipboard read attempt.
3. **Synchronous read is impossible** — `getFromClipboard()` always returns `null`. Use `getFromClipboardAsync()` instead.
4. **No background execution** — when the tab is hidden, `setInterval` is throttled to once per minute by the browser, and clipboard reads will be denied.

The overlay is not implemented for JS since browsers don't support floating system windows. URL detections are emitted via `SharedFlow` only.

#### Wasm JS — Same as JS, Async Only

Kotlin/Wasm targets the same browser environment as the JS target but uses `@JsFun` interop instead of Kotlin/JS dynamic calls. The same browser security restrictions apply:

- Clipboard write works via `navigator.clipboard.writeText()` (fire-and-forget).
- Clipboard read requires the async `navigator.clipboard.readText()` API — only available through `getFromClipboardAsync()`.
- Monitoring uses coroutine-based polling with `getFromClipboardAsync()`, subject to the same permission and focus requirements as JS.

#### Linux — Full Monitoring, No Overlay

Linux clipboard access depends on external tools:
- **X11**: Requires `xclip` or `xsel` to be installed (`sudo apt install xclip`).
- **Wayland**: Not yet supported — `xclip` only works under XWayland compatibility.

Monitoring is implemented via coroutine-based polling that shells out to `xclip -selection clipboard -o` on each interval. This is reliable but adds subprocess overhead. There is no overlay or system tray implementation in the library — desktop Linux UIs vary too widely (GNOME, KDE, Sway, etc.).

#### Windows (mingwX64) — Full Monitoring, No Overlay

Uses the Win32 clipboard API (`OpenClipboard`, `GetClipboardData`, `SetClipboardData`) via Kotlin/Native `mingw` interop. Monitoring is coroutine-based polling that reads the clipboard at the configured interval.

A native `AddClipboardFormatListener` implementation (event-driven instead of polling) is planned for a future release. The current polling approach is functional but slightly less efficient than an event listener.

No system tray or overlay is implemented yet for mingw. The Win32 window management APIs required for this are complex to bridge via Kotlin/Native.

#### tvOS — Not Supported

tvOS does not have a system clipboard. The `UIPasteboard` API is not available on Apple TV. All clipboard operations return `false`/`null`, and the monitor remains in `Idle` state. This is a hardware/OS limitation — Apple TV is controlled via a remote and has no text selection or copy/paste workflow.

#### watchOS — Not Supported

watchOS does not expose a clipboard API. `UIPasteboard` is unavailable on Apple Watch. The tiny screen and limited input methods (Digital Crown, small touch target) make clipboard operations impractical. All operations are no-op stubs.

#### Wasm WASI — Not Supported

WASI (WebAssembly System Interface) runs in a sandboxed server-side environment without a GUI, browser, or clipboard. There is no clipboard concept in the WASI specification. All operations are no-op stubs. If you need clipboard support in a Wasm context, use the `wasmJs` target (browser) instead.

## Android Manifest (Auto-merged)

The library automatically merges these into your app's manifest:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Migration from v0.1.0

v0.2.0 is fully backward-compatible. Existing `copyToClipboard()`, `getFromClipboard()`, `hasClipboardText()`, `clearClipboard()`, and `ClipboardObserver` APIs are unchanged.

New additions:
- `ClipboardMonitor` — continuous monitoring with URL detection
- `ClipboardAsync` — suspend function variants
- `ClipboardPermission` — permission checking
- `ClipboardFilter` — content filtering
- `ClipboardUrlMatcher` — URL pattern detection
- `ClipboardOverlayConfig` — floating FAB configuration
- `ClipboardWorkerTrigger` — background task trigger
