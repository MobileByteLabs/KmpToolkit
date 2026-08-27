# cmp-share

Cross-platform share-sheet library for Kotlin Multiplatform.

> **Status:** Experimental. All public APIs marked `@ExperimentalShareApi`.
> Ships alongside the other `cmp-*` modules at the shared `kmptoolkit.version`.

## Features

- **Payload variants**: text, URL, image bytes, file URI, multi (composite)
- **Platforms**: Android, iOS, macOS, JVM Desktop, JS, wasmJs
- **Native UI**: each platform uses its OS-native share surface — no custom Compose UI
- **Suspend API**: integrates with structured concurrency; cancellation propagates correctly
- **Typed errors**: sealed `ShareError` hierarchy + `ShareResult` for completion / cancel / failure
- **Zero third-party deps**: native platform APIs only (kotlinx-coroutines + kotlinx-browser already in kmp-toolkit)

## Platform support

| Platform | Backing API | Notes |
|----------|-------------|-------|
| Android | `Intent.ACTION_SEND` + `Intent.createChooser` | Files via FileProvider (auto-wired); `ACTION_SEND_MULTIPLE` for Multi payloads |
| iOS | `UIActivityViewController` | Anchored to key-window root; iOS 14+ |
| macOS | `NSSharingServicePicker` | Anchored to key-window contentView; macOS 11+ |
| JVM (Desktop) | System clipboard (text/url/image) + AWT `FileDialog` SAVE (file) | Best-effort — NOT a native share sheet |
| JS / wasmJs | `navigator.share` if available; else `navigator.clipboard.writeText` fallback | **Must invoke from a user-gesture handler** (browser security) |

> **Not targeted:** tvOS, watchOS, Linux native, mingwX64, wasmWasi.
> Per cmp-toolkit Tier-3 exclusion policy. Adding them later requires upstream
> share-API coverage.

## Install

```kotlin
// build.gradle.kts (your consumer app)
dependencies {
    val kmptoolkit = "3.2.13" // or latest — see https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share
    implementation("io.github.mobilebytelabs:cmp-share:$kmptoolkit")
}
```

## Quick start

### Share text

```kotlin
@OptIn(ExperimentalShareApi::class)
@Composable
fun ShareButton() {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = Share.text(
                content = "Hello from cmp-share!",
                options = ShareOptions(chooserTitle = "Share quote"),
            )
            when (result) {
                ShareResult.Completed -> showToast("Shared")
                ShareResult.Cancelled -> { /* user dismissed */ }
                is ShareResult.Failed -> showToast("Share failed: ${result.cause}")
            }
        }
    }) { Text("Share") }
}
```

### Share a generated file

```kotlin
@OptIn(ExperimentalShareApi::class)
suspend fun shareReceipt(pdfUri: String) {
    Share.file(
        uri = pdfUri,
        mimeType = "application/pdf",
        filename = "receipt.pdf",
        options = ShareOptions(chooserTitle = "Send receipt"),
    )
}
```

### Share an image

```kotlin
@OptIn(ExperimentalShareApi::class)
suspend fun shareSnapshot(pngBytes: ByteArray) {
    Share.image(bytes = pngBytes, mimeType = "image/png", filename = "snap.png")
}
```

### Multi-payload (text + file + image)

```kotlin
@OptIn(ExperimentalShareApi::class)
suspend fun shareReport(summary: String, pdfUri: String, chartPng: ByteArray) {
    Share.multi(
        payloads = listOf(
            SharePayload.Text(summary),
            SharePayload.File(pdfUri, "application/pdf", "report.pdf"),
            SharePayload.Image(chartPng, "image/png", "chart.png"),
        ),
    )
}
```

## Per-platform setup notes

### Android — zero-config

The library declares its `ShareInitProvider` + `FileProvider` (authority
`${applicationId}.cmp-share.fileprovider`) via manifest-merger. Consumer apps don't
need to declare anything.

### Android — direct-to-app share (skip the chooser)

Set `ShareOptions.targetPackage` to route the payload straight to a specific app (WhatsApp,
Instagram, …) instead of the system chooser:

```kotlin
Share.file(
    uri = "content://…/status.mp4",
    mimeType = "video/mp4",
    options = ShareOptions(targetPackage = "com.whatsapp"),
)
```

- If the target package isn't installed, can't handle the payload, or isn't visible under
  Android 11+ package-visibility, it **falls back to the normal chooser** (never fails).
- `targetPackage` is **Android-only** — ignored on iOS / desktop / web (those platforms have no
  per-app targeting).
- **Android 11+ package visibility is handled for you.** cmp-share's own manifest declares the
  `<queries>` SEND intent, merged into every consumer via manifest-merger — so a target app is
  visible without any consumer-side `<queries>` boilerplate. (Same zero-config contract as the
  bundled `ShareInitProvider` + `FileProvider`.)

### iOS — present via key-window

Default: cmp-share traverses `UIApplication.keyWindow.rootViewController.topMostController`
to find the presenting `UIViewController`. To anchor to a specific VC:

```kotlin
Share.text(
    content = "hi",
    options = ShareOptions(presentingController = myUIViewController),
)
```

### JVM Desktop — best-effort, NOT a native share sheet

- Text / URL → system clipboard write + log "copied to clipboard"
- Image → clipboard image transferable
- File → AWT `FileDialog(SAVE)` save-as prompt
- Multi → first text-payload subset → clipboard, first file-payload → FileDialog

Documented loudly as "best-effort"; consumer apps that need a true native macOS share sheet on Desktop should run via macOS Compose-MP target.

### JS / wasmJs — user-gesture required

```kotlin
// ✅ CORRECT — Share.share() inside Composable onClick (user-gesture activation)
Button(onClick = { scope.launch { Share.text("hi") } }) { ... }

// ❌ WRONG — LaunchedEffect is NOT a user-gesture call stack
// Browser blocks navigator.share() / navigator.clipboard.writeText()
LaunchedEffect(Unit) { Share.text("hi") }  // returns ShareResult.Failed(UserGestureMissing)
```

## See also

- SPEC: [idea-layer/modules/cmp-share/SPEC.md](../../../idea-layer/modules/cmp-share/SPEC.md)
- API: [idea-layer/modules/cmp-share/API.md](../../../idea-layer/modules/cmp-share/API.md)
- ADRs: [idea-layer/modules/cmp-share/adrs/](../../../idea-layer/modules/cmp-share/adrs/)
- Sibling modules: [cmp-intent-launcher](../cmp-intent-launcher/), [cmp-app-intents](../cmp-app-intents/), [cmp-open-url](../cmp-open-url/), [cmp-deep-link](../cmp-deep-link/)
