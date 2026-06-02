---
title: "How do I copy text to the system clipboard?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I copy text to the system clipboard?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mobilebytelabs.kmptoolkit.clipboard.Clipboard
import kotlinx.coroutines.launch

@Composable
fun CopyShareLinkButton(url: String) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            Clipboard.copy(url)
        }
    }) { Text("Copy link") }
}
```

## Caveats / per-platform notes

- **Android 13+:** the system shows a UI hint "Copied to clipboard" — your app
  does NOT need to display its own toast (and shouldn't, per Material 3
  guidance).
- **iOS:** copy is silent — pair with a toast for user feedback if needed.
- **JS / wasmJs:** must be called inside a user-gesture handler; falls back to
  the legacy `document.execCommand("copy")` path if the modern Clipboard API
  is unavailable.
- **Linux:** requires `xclip` (X11) or `wl-copy` (Wayland) on PATH; without
  either, returns `Result.failure(UnsupportedPlatform)`.

## Related

- Module: [cmp-clipboard](../../modules/cmp-clipboard.md)
- Sample: [`samples/sample-cmp-clipboard/composeApp/.../CopyDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-clipboard)
- See also: [Read text from the system clipboard](clipboard-read-text.md)
