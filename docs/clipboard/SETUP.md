# cmp-clipboard — Integration Guide

> `io.github.mobilebytelabs:kmp-clipboard:2.1.0`

`cmp-clipboard` is **zero-configuration** — no init, no DI module, no nav destinations.
3 steps: add dependency, import, use.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmp-clipboard = "2.1.0"

[libraries]
kmp-clipboard = { module = "io.github.mobilebytelabs:kmp-clipboard", version.ref = "kmp-clipboard" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmp.clipboard)
}
```

---

## Step 2 — Copy and Paste

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard

// Copy
val ok = copyToClipboard("Text to copy")
// ok = true if initiated successfully

// Paste
val text = getFromClipboard()  // null if empty or JS/Wasm

// Paste button state
pasteButton.isEnabled = hasClipboardText()

// Clear sensitive data
clearClipboard()
```

---

## Step 3 — Optional: Reactive Observer

Use `ClipboardObserver` to detect when the user copies content in another app:

### In a ViewModel (coroutine scope)

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardObserver

class MyViewModel : ViewModel() {
    private val clipboardObserver = createClipboardObserver()

    val clipboardContent = clipboardObserver.clipboardContent  // StateFlow<String?>

    init {
        clipboardObserver.startObserving()
    }

    override fun onCleared() {
        clipboardObserver.stopObserving()
        super.onCleared()
    }
}
```

### In Compose (auto lifecycle-managed)

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.rememberClipboardObserver

@Composable
fun PasteFromClipboardExample() {
    val observer = rememberClipboardObserver()
    val content by observer.clipboardContent.collectAsState()

    if (content != null) {
        Button(onClick = { /* use content */ }) {
            Text("Paste: $content")
        }
    }
}
```

---

## Platform Notes

| Platform | Copy | Read | Observer |
|----------|:----:|:----:|:--------:|
| Android / iOS / macOS / JVM | ✅ | ✅ | ✅ |
| JS / Wasm JS | ✅ write-only | ❌ | Limited |
| Linux / Windows | ✅ | ✅ | Limited (polling) |
| WASI | ❌ | ❌ | ❌ |

---

## AI-Assisted Setup

```
/sync-clipboard           # Verify Gradle dependency (only gate needed)
/sync-clipboard --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
