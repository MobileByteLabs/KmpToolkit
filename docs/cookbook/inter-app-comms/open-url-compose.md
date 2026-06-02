---
title: "How do I open a URL from a Compose button?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I open a URL from a Compose button?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mobilebytelabs.kmptoolkit.openurl.OpenUrl
import kotlinx.coroutines.launch

@Composable
fun OpenDocsButton() {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            OpenUrl.open("https://mobilebytelabs.github.io/KmpToolkit/")
        }
    }) { Text("Open docs site") }
}
```

## Caveats / per-platform notes

- **iOS / Android:** opens in the platform's default browser; use a Custom Tabs
  / SFSafariViewController wrapper if you need in-app browsing.
- **JS / wasmJs:** uses `window.open(url, '_blank')` — popup blockers may
  intercept; must be called from a user-gesture handler.
- **JVM Desktop:** calls `Desktop.browse()`; falls back to `xdg-open` on Linux
  if AWT is unavailable.

## Related

- Module: [cmp-open-url](../../modules/cmp-open-url.md)
- Sample: [`samples/sample-cmp-open-url/composeApp/.../OpenUrlDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-open-url)
- See also: [cmp-deep-link](../../modules/cmp-deep-link.md) for routing incoming URLs into your app.
