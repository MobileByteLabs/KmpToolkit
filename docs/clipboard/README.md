# cmp-clipboard

Zero-configuration cross-platform clipboard for Kotlin Multiplatform.

```
io.github.mobilebytelabs:kmp-clipboard:2.1.0
```

---

## What It Does

Copy, paste, check, and observe the system clipboard across all KMP platforms — no setup, no
config class, no DI. Just import and use.

---

## Platform Support

| Platform | Copy | Read | Observe |
|----------|:----:|:----:|:-------:|
| Android  | ✅   | ✅   | ✅ (lifecycle-aware) |
| iOS      | ✅   | ✅   | ✅ (foreground change detection) |
| macOS    | ✅   | ✅   | ✅ (polling + changeCount) |
| tvOS     | ✅   | ✅   | ✅ |
| watchOS  | ✅   | ✅   | ✅ |
| JVM      | ✅   | ✅   | ✅ (FlavorListener) |
| JS       | ✅   | ❌   | Limited (visibility change) |
| Wasm JS  | ✅   | ❌   | Limited |
| Linux    | ✅   | ✅   | Limited (polling, requires xclip/xsel) |
| Windows  | ✅   | ✅   | Limited (polling) |
| WASI     | ❌   | ❌   | ❌ |

---

## Quick Start

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard

// Copy
val success = copyToClipboard("Hello, World!")

// Read
val text: String? = getFromClipboard()

// Check
if (hasClipboardText()) { /* enable paste button */ }

// Clear
clearClipboard()
```

---

## API Reference

### Core Functions (top-level)

```kotlin
// Copy text — returns true if initiated successfully
fun copyToClipboard(text: String): Boolean

// Read text — returns null if empty, unsupported, or non-text
fun getFromClipboard(): String?

// Check if clipboard has text content
fun hasClipboardText(): Boolean

// Clear clipboard
fun clearClipboard()
```

### Reactive Observer (optional)

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardObserver
import com.mobilebytelabs.kmptoolkit.clipboard.rememberClipboardObserver

// In a ViewModel / coroutine scope
val observer = createClipboardObserver()
observer.startObserving()
observer.clipboardContent.collect { content ->
    println("Clipboard changed: $content")
}
observer.stopObserving()

// In Compose (lifecycle-managed)
@Composable
fun MyScreen() {
    val observer = rememberClipboardObserver()
    val content by observer.clipboardContent.collectAsState()
    Text("Clipboard: $content")
}
```

### ClipboardObserver interface

```kotlin
interface ClipboardObserver {
    val clipboardContent: StateFlow<String?>
    val isObserving: Boolean
    fun startObserving()
    fun stopObserving()
}
```

---

## Notes

- **JS**: `copyToClipboard` is fire-and-forget (async write) — always returns `true`
- **JS / Wasm**: `getFromClipboard()` returns `null` (async API, read not supported synchronously)
- **Linux**: requires `xclip` or `xsel` installed on the system
- **WASI**: no clipboard access available in runtime

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-clipboard`
