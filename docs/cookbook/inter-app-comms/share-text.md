---
title: "How do I share text from a button?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I share text from a button?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import kotlinx.coroutines.launch

@Composable
fun ShareTextButton() {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            Share.share(SharePayload.Text("Hello from kmp-toolkit!"), ShareOptions())
        }
    }) { Text("Share") }
}
```

## Caveats / per-platform notes

- **JS / wasmJs:** must be invoked inside a user-gesture handler (click /
  keydown); else returns `Failed(UserGestureMissing)`.
- **Linux:** falls back to `xclip -selection clipboard` (text-only) when no
  desktop share portal is available.
- **tvOS:** `Failed(UnsupportedPlatform)` unless the consumer ships
  `CmpShareTvosBridge.swift` (see ADR-001).

## Related

- Module: [cmp-share](../../modules/cmp-share.md)
- Sample: [`samples/sample-cmp-share/composeApp/src/commonMain/kotlin/io/github/mobilebytelabs/kmptoolkit/sample/cmpshare/TryItPanel.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-share)
- ADR: [ADR-001 — tvOS no-share + watchOS arm32 binary-share policy](https://github.com/MobileByteLabs/KmpToolkit/blob/development/cmp-share/docs/ADR-001-tvos-no-share-watchos-arm32.md)
