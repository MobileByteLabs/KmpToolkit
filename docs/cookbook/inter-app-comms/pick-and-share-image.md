---
title: "How do I pick an image then share it?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I pick an image then share it?

## Quick start (minimal MWE)

```kotlin
import com.mobilebytelabs.kmptoolkit.intentlauncher.rememberIntentLauncher
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentRequest
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun PickAndShareButton() {
    val scope = rememberCoroutineScope()
    val picker = rememberIntentLauncher(IntentRequest.PickImage()) { result ->
        scope.launch {
            result.uri?.let { Share.share(SharePayload.File(it, "image/*"), ShareOptions()) }
        }
    }
    Button(onClick = { picker.launch() }) { Text("Pick + share") }
}
```

## Caveats / per-platform notes

- **iOS:** `PickImage` uses PHPickerViewController on iOS 14+; falls back to
  UIImagePickerController on earlier. Photo Library permission auto-prompted.
- **Android:** uses ActivityResultContracts.PickVisualMedia (system photo picker
  on Android 13+); falls back to `ACTION_PICK` on older.
- **Desktop / Web:** PickImage opens a file dialog filtered to images/*.

## Related

- Module: [cmp-intent-launcher](../../modules/cmp-intent-launcher.md) + [cmp-share](../../modules/cmp-share.md)
- Sample: [`samples/sample-cmp-intent-launcher/composeApp/.../PickFlowDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-intent-launcher)
