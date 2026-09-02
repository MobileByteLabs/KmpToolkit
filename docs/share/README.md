# cmp-share

Cross-platform native share sheet for Kotlin Multiplatform — one API, every target.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-share.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share)

---

## What It Does

`cmp-share` exposes a single suspending `Share` object that invokes the native share sheet
on Android/iOS/macOS, falls back to `navigator.share` on JS/wasmJs, and provides a JVM
best-effort implementation (opens system file manager / mailto).  Share text, URLs, images,
arbitrary files, or a mixed multi-payload — all through one unified API.

> **Experimental API**: annotated with `@ExperimentalShareApi`. Opt in with
> `@OptIn(ExperimentalShareApi::class)` or the compiler flag below.

---

## Platform Support

| Platform | Real impl | `onUnsupported` | Notes |
|----------|:---------:|:---------------:|-------|
| Android  | ✅        | —               | ACTION_SEND / ACTION_SEND_MULTIPLE via Chooser |
| iOS      | ✅        | —               | UIActivityViewController |
| macOS    | ✅        | —               | NSSharingServicePicker |
| JVM      | ✅ best-effort | —          | Desktop.open() / mailto for URLs; file-manager for files |
| JS       | ✅        | —               | `navigator.share()` (HTTPS + user gesture required) |
| wasmJs   | ✅        | —               | `navigator.share()` (same constraints as JS) |
| tvOS     | ⚠ UnsupportedPlatform | ✅ | tvOS lacks UIActivityViewController; K/N bindings also omit UIPasteboard (v3.3.0 limitation) |
| watchOS  | ⚠ UnsupportedPlatform | ✅ | No share-sheet surface; handoff-to-iPhone via WCSession is v0.3 candidate |
| Linux    | ✅ URL/file via xdg-open | ✅ | Requires `xdg-utils`. Text/image/multi onUnsupported |
| mingw    | ✅ URL/file via `cmd /c start` | ✅ | Default Windows handler resolution. Text/image/multi onUnsupported |
| wasmWasi | ⛔ out-of-scope | —          | No DOM, no clipboard, no UI gesture surface — server-side WASM only |

> **v0.2 update (v3.3.0):** tier-3 targets are no longer hard-excluded. Linux + mingw
> ship real URL-share implementations; tvOS/watchOS ship documented `UnsupportedPlatform`
> fallbacks (the same `onUnsupported { }` hook callers already use on JS/wasmJs is honored).
> `wasmWasi` stays out-of-scope — the constraint is genuine, not effort-based.

---

## Quick Start

```kotlin
@OptIn(ExperimentalShareApi::class)
suspend fun onShareClicked() {
    val result = Share.text("Check out KMP Toolkit!")
    when (result) {
        is ShareResult.Completed -> println("Shared successfully")
        is ShareResult.Cancelled -> println("User cancelled")
        is ShareResult.Failed    -> println("Error: ${result.cause}")
    }
}
```

---

## API Reference

### `Share` expect object

```kotlin
@ExperimentalShareApi
expect object Share {
    suspend fun share(
        payload: SharePayload,
        options: ShareOptions = ShareOptions()
    ): ShareResult
}
```

### Extension functions

```kotlin
suspend fun Share.text(content: String, options: ShareOptions = ShareOptions()): ShareResult
suspend fun Share.url(href: String, options: ShareOptions = ShareOptions()): ShareResult
suspend fun Share.image(
    bytes: ByteArray,
    mimeType: String,
    filename: String? = null,
    options: ShareOptions = ShareOptions()
): ShareResult
suspend fun Share.file(
    bytes: ByteArray,
    mimeType: String,
    filename: String,
    options: ShareOptions = ShareOptions()
): ShareResult
suspend fun Share.multi(
    payloads: List<SharePayload>,
    options: ShareOptions = ShareOptions()
): ShareResult
```

### `SharePayload` sealed class

```kotlin
sealed class SharePayload {
    data class Text(val content: String) : SharePayload()
    data class Url(val href: String) : SharePayload()
    data class Image(val bytes: ByteArray, val mimeType: String, val filename: String?) : SharePayload()
    data class File(val bytes: ByteArray, val mimeType: String, val filename: String) : SharePayload()
    data class Multi(val payloads: List<SharePayload>) : SharePayload()
}
```

### `ShareOptions`

```kotlin
data class ShareOptions(
    val chooserTitle: String? = null,          // Android only — Chooser dialog title
    val excludedActivities: List<String> = emptyList(), // Android only — package names to exclude
    val presentingController: Any? = null      // iOS/macOS — UIViewController / NSViewController
)
```

### `ShareResult` sealed class

```kotlin
sealed class ShareResult {
    object Completed : ShareResult()
    object Cancelled : ShareResult()
    data class Failed(val cause: ShareError) : ShareResult()
}
```

### `ShareError`

```kotlin
sealed class ShareError {
    object UnsupportedPlatform : ShareError()
    object NoHandler : ShareError()
    object UserGestureMissing : ShareError()   // JS/wasmJs only
    data class Unknown(val message: String) : ShareError()
}
```

---

## Notes

- **Android file shares — `file://` is resolved for you (since v3.5.20).** `ACTION_SEND` rejects a
  `file://` URI on Android 7+ (`FileUriExposedException`). `SharePayload.File` previously passed the
  caller's URI to `EXTRA_STREAM` verbatim, so every `file://` share failed; and because `share()`
  wraps everything in `try/catch → ShareResult.Failed`, any caller that ignored the returned
  `ShareResult` saw the share button silently do nothing. Now:
  - a `content://` (or `http(s)://`) URI is passed through **unchanged** — no behaviour change for
    payloads that already worked;
  - a `file://` URI (or a bare path) is wrapped through this module's own FileProvider into a
    `content://` URI, which — with the `FLAG_GRANT_READ_URI_PERMISSION` already set — gives the
    receiving app scoped, per-URI read access and no storage permission;
  - a file outside the paths in `cmp_share_paths.xml` is staged into the module's cache dir first
    (one copy) and shared from there, so consumers don't have to widen their own FileProvider paths;
  - if all of that fails the original URI is used, so behaviour is never worse than before.

  **Still prefer passing a `content://` URI** when you already have one (e.g. a MediaStore insert) —
  it skips the wrap and any staging copy entirely.
- **JS / wasmJs**: `navigator.share()` requires HTTPS and a user gesture — call from a click
  handler, not a coroutine launched in `LaunchedEffect` without user interaction.
- **JVM**: best-effort — opens the default handler registered by the OS; file-type support
  depends on installed applications.
- **Multi-payload on iOS**: wraps items in a single `UIActivityViewController` activity items
  array.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-share`
