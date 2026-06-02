---
title: "How do I share a PDF on Android?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I share a PDF on Android?

## Quick start (minimal MWE)

```kotlin
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import java.io.File

suspend fun shareInvoice(pdfFile: File) {
    Share.share(
        payload = SharePayload.File(
            path = pdfFile.absolutePath,
            mimeType = "application/pdf",
            displayName = pdfFile.name,
        ),
        options = ShareOptions(title = "Share invoice"),
    )
}
```

## Caveats / per-platform notes

- **Android:** the file must be reachable via FileProvider. cmp-share auto-wires
  the manifest `<provider>` entry; if you've overridden it, ensure your authority
  matches `${applicationId}.cmp-share.fileprovider`.
- **iOS:** mime type ignored — UTI inferred from the file extension; use `.pdf`.
- **JS / wasmJs:** falls back to download-then-share via Web Share API Level 2;
  Safari only supports it inside a user-gesture handler.
- **JVM Desktop:** opens the system "Share" dialog on macOS Sonoma+ / Windows 11;
  otherwise opens the Files browser at the parent directory.

## Related

- Module: [cmp-share](../../modules/cmp-share.md)
- Sample: [`samples/sample-cmp-share/composeApp/.../FileShareDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-share)
- See also: [cmp-pdf-generator](../../modules/cmp-pdf-generator.md) for producing the PDF first.
