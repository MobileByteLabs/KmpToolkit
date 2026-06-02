---
title: "How do I read text from the system clipboard?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I read text from the system clipboard?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.mobilebytelabs.kmptoolkit.clipboard.Clipboard
import kotlinx.coroutines.launch

@Composable
fun PasteIntoFieldButton(onPasted: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            Clipboard.read()?.let(onPasted)
        }
    }) { Text("Paste") }
}
```

## Caveats / per-platform notes

- **iOS 14+:** a paste action surfaces a system toast "{App} pasted from
  {SourceApp}" — Apple's privacy policy, not bypassable.
- **Android 12+:** the OS shows a paste-notification toast for the same reason.
  For privacy compliance, only read the clipboard in direct response to a
  user-initiated action (e.g. button click), never on app foreground.
- **JS / wasmJs:** browsers require the page to be in focus AND a user-gesture
  handler; some browsers prompt the user for permission on first read.
- **JVM Desktop:** `Toolkit.getSystemClipboard()`; on Linux, also tries
  `xclip -selection clipboard -o` for non-AWT environments.

## Related

- Module: [cmp-clipboard](../../modules/cmp-clipboard.md)
- Sample: [`samples/sample-cmp-clipboard/composeApp/.../PasteDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-clipboard)
- See also: [Copy text to the system clipboard](clipboard-copy-text.md)
