# cmp-toast

Compose Multiplatform toast notification system.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-toast.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-toast)

---

## What It Does

A lightweight, composable toast library for Compose Multiplatform. Show short non-blocking
messages with optional action buttons — positioned at top, center, or bottom of screen,
with 5 visual styles and 3 duration presets.

No platform channels, no native code, no DI, no Supabase. Pure Compose.

---

## Quick Start

```kotlin
// 1. Create state (in composable)
val toastState = rememberToastHostState()
val scope = rememberCoroutineScope()

// 2. Place ToastHost in your root Box / Scaffold
Box(modifier = Modifier.fillMaxSize()) {
    YourScreenContent()
    ToastHost(hostState = toastState)
}

// 3. Show toasts from anywhere that has the scope
scope.launch {
    toastState.showToast("File saved!")
    // with options:
    toastState.showToast(
        message     = "Copied to clipboard",
        actionLabel = "Undo",
        duration    = ToastDuration.LONG,
        position    = ToastPosition.BOTTOM,
        style       = ToastStyle.SUCCESS,
    )
}
```

---

## API Reference

### State

```kotlin
// Create (in composable)
val toastState = rememberToastHostState()

// Show (in coroutine scope)
suspend fun showToast(
    message:     String,
    actionLabel: String?       = null,
    duration:    ToastDuration = ToastDuration.SHORT,
    position:    ToastPosition = ToastPosition.BOTTOM,
    style:       ToastStyle    = ToastStyle.DEFAULT,
): ToastResult

// Dismiss programmatically
fun dismiss()

// Observe
val currentToast: StateFlow<ToastData?>
```

### ToastHost (composable)

```kotlin
@Composable
fun ToastHost(
    hostState: ToastHostState,
    modifier:  Modifier = Modifier,
    toast:     @Composable (ToastData) -> Unit = { DefaultToast(it) },
)
```

### Enums

```kotlin
enum class ToastDuration { SHORT, LONG, INDEFINITE }
// SHORT = 3000ms, LONG = 5000ms, INDEFINITE = stays until dismissed

enum class ToastPosition { TOP, CENTER, BOTTOM }

enum class ToastStyle { DEFAULT, SUCCESS, ERROR, WARNING, INFO }
```

### ToastResult

```kotlin
enum class ToastResult { DISMISSED, ACTION_PERFORMED }
```

---

## Platform Support

Runs everywhere Compose Multiplatform runs:

| Platform | Support |
|----------|:-------:|
| Android | ✅ |
| iOS | ✅ |
| macOS | ✅ |
| JVM | ✅ |
| JS / Wasm | ✅ |

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-toast`
