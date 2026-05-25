# cmp-intent-launcher

Type-safe Android Intent launcher with graceful `onUnsupported` fallback for all other KMP targets.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-intent-launcher.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-intent-launcher)

---

## What It Does

`cmp-intent-launcher` wraps Android's `ActivityResultLauncher` pattern in a coroutine-friendly,
Compose-ready `IntentLauncher` class.  Build intents via a Kotlin DSL, `await` the result, and
handle the typed `IntentResult` — no callbacks, no `onActivityResult`.  Picker contracts
(`PickImage`, `PickMultipleImages`, `PickDocument`, `PickContact`) are wired to native UIKit pickers
on iOS (`PHPickerViewController`, `UIDocumentPickerViewController`, `CNContactPickerViewController`);
unsupported actions on any platform route to the caller's `onUnsupported` lambda.

> **Experimental API**: annotated with `@ExperimentalIntentLauncherApi`. Opt in with
> `@OptIn(ExperimentalIntentLauncherApi::class)` or the compiler flag below.

---

## Platform Support

| Platform | Real impl | `onUnsupported` | Notes |
|----------|:---------:|:---------------:|-------|
| Android  | ✅        | —               | ActivityResultLauncher + coroutine bridge |
| iOS      | ✅ pickers | ✅              | PHPicker (image) + UIDocumentPicker (file) + CNContactPicker (contact); arbitrary actions → `onUnsupported` |
| macOS    | ⚠ onUnsupported | ✅         | NSOpenPanel planned for v0.2 |
| JVM      | 🟡 partial | —              | java.awt.Desktop file/URI open; no result callback |
| JS       | ⚠ onUnsupported | ✅         | v1 TS9 out-of-scope |
| wasmJs   | ⚠ onUnsupported | ✅         | v1 TS9 out-of-scope |
| tvOS     | ⛔ out-of-scope | —          | v1 TS9 explicitly excluded |
| watchOS  | ⛔ out-of-scope | —          | v1 TS9 explicitly excluded |
| Linux    | ⛔ out-of-scope | —          | v1 TS9 explicitly excluded |
| mingw    | ⛔ out-of-scope | —          | v1 TS9 explicitly excluded |
| wasmWasi | ⛔ out-of-scope | —          | v1 TS9 explicitly excluded |

> Tier-3 targets (tvOS, watchOS, Linux, mingw, wasmWasi) are explicitly out-of-scope for
> v1 per Phase 0 spike TS9.

---

## Quick Start

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun PickFileScreen() {
    val launcher = rememberIntentLauncher()

    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                action = Intent.ACTION_GET_CONTENT
                type = "*/*"
            }
            when (result) {
                is IntentResult.Ok        -> handleData(result.data)
                is IntentResult.Cancelled -> { /* user dismissed */ }
                is IntentResult.Failed    -> logError(result.cause)
            }
        }
    }) {
        Text("Pick File")
    }
}
```

---

## API Reference

### `IntentLauncher` expect class

```kotlin
@ExperimentalIntentLauncherApi
expect class IntentLauncher {
    suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult
}
```

### Compose helper

```kotlin
@Composable
fun rememberIntentLauncher(): IntentLauncher
```

### `IntentBuilder` DSL

```kotlin
class IntentBuilder {
    var action: String?
    var type: String?
    fun data(uri: String)
    fun extra(key: String, value: Any)
    var flags: Int
    fun targetPackage(packageName: String)  // Android only — targeted launch
}
```

### `IntentResult` sealed class

```kotlin
sealed class IntentResult {
    data class Ok(val data: IntentData?) : IntentResult()
    object Cancelled : IntentResult()
    data class Failed(val cause: IntentError) : IntentResult()
}
```

### `IntentError`

```kotlin
sealed class IntentError {
    object UnsupportedPlatform : IntentError()
    object NoHandler : IntentError()
    object UserGestureMissing : IntentError()
    data class Unknown(val message: String) : IntentError()
}
```

### `ResultContracts` object

```kotlin
object ResultContracts {
    val PICK: String          // ACTION_PICK
    val GET_CONTENT: String   // ACTION_GET_CONTENT
    val OPEN_DOCUMENT: String // ACTION_OPEN_DOCUMENT
    val CREATE_DOCUMENT: String
    val SEND: String          // ACTION_SEND
}
```

---

## Notes

- `rememberIntentLauncher()` **must be called at Compose composition time** — not inside a
  `LaunchedEffect` or `onClick` lambda.  It registers the `ActivityResultLauncher` during
  composition.
- **JVM partial**: `Desktop.open()` / `Desktop.browse()` runs but returns
  `IntentResult.Ok(null)` — no result data is propagated back.
- On non-Android targets, `onUnsupported` surfaces as `IntentResult.Failed(IntentError.UnsupportedPlatform)`.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps (includes Android Activity wiring)
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-intent-launcher`
