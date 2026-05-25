# cmp-app-intents

Declare and register app intents (Siri Shortcuts / Android App Actions) from shared Kotlin code.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-app-intents.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-app-intents)

---

## What It Does

`cmp-app-intents` lets you declare your app's Siri Shortcuts, Android App Actions, and Spotlight
search contributions from a single Kotlin DSL.  On iOS/macOS it generates the required
`AppIntents` manifest and bridges to Swift via `CmpAppIntentBridge`.  On Android it registers
intents with the on-device registry.  On JVM, JS, and wasmJs it provides `invokeForTesting`
so you can drive intent execution in tests or demos without a real device.

> **Experimental API**: annotated with `@ExperimentalAppIntentsApi`. Opt in with
> `@OptIn(ExperimentalAppIntentsApi::class)` or the compiler flag below.

---

## Platform Support

| Platform | Real impl | `onUnsupported` | Notes |
|----------|:---------:|:---------------:|-------|
| Android  | ✅        | —               | On-device registry via App Actions XML + runtime binding |
| iOS      | ✅        | —               | Manifest + Swift bridge; Spotlight requires real-device verification |
| macOS    | ✅        | —               | Same manifest + Swift bridge as iOS |
| JVM      | ✅ invokeForTesting | —      | No OS integration; intents run in-process for tests/demos |
| JS       | ✅ invokeForTesting | —      | No OS integration; intents run in-process for tests/demos |
| wasmJs   | ✅ invokeForTesting | —      | No OS integration; intents run in-process for tests/demos |
| tvOS     | ✅ registry + manifest | — | Siri Suggestions on Apple TV (tvOS 14+); CoreSpotlight skipped via `#if canImport(CoreSpotlight)` guard |
| watchOS  | ✅ registry + manifest | — | Shortcuts on Apple Watch (watchOS 10+); same Swift bridge as iOS, Spotlight skipped |
| Linux    | ✅ registry-only | — | `invokeForTesting()` works; no native Shortcuts equivalent on Linux |
| mingw    | ✅ registry-only | — | `invokeForTesting()` works; no native Shortcuts equivalent on Windows |
| wasmWasi | ⛔ out-of-scope | —          | No UI surface — server-side WASM only |

> **v0.2 update (v3.3.0):** tier-3 targets now register intents into the in-memory
> `AppIntentsRuntime` registry so `invokeForTesting()` works on every platform.
> watchOS gets full Swift-bridge App Intents support (watchOS 10+ Shortcuts);
> tvOS partial via Siri Suggestions. CoreSpotlight indexing silently skips on
> tvOS/watchOS via `#if canImport(CoreSpotlight)` guards in `CmpAppIntentBridge.swift`.
> `wasmWasi` stays out-of-scope — genuinely no UI surface.

---

## Quick Start

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
fun registerIntents() {
    AppIntents.register(
        appIntents {
            intent("search_products") {
                title("Search Products")
                param("query") { type = ParamType.String }
                handler { params ->
                    val query = params["query"] as? String ?: ""
                    AppIntentResult.Snippet("Results for: $query")
                }
            }
            intent("open_dashboard") {
                title("Open Dashboard")
                handler { AppIntentResult.Done }
            }
        }
    )
}
```

---

## API Reference

### `AppIntents` expect object

```kotlin
@ExperimentalAppIntentsApi
expect object AppIntents {
    fun register(config: AppIntentsConfig)
    suspend fun invokeForTesting(
        id: String,
        params: Map<String, Any> = emptyMap()
    ): AppIntentResult?
}
```

### Top-level DSL builder

```kotlin
fun appIntents(block: AppIntentsBuilder.() -> Unit): AppIntentsConfig
```

### `AppIntentsBuilder`

```kotlin
class AppIntentsBuilder {
    fun intent(id: String, block: AppIntentBuilder.() -> Unit)
}
```

### `AppIntentBuilder`

```kotlin
class AppIntentBuilder {
    fun title(text: String)
    fun param(name: String, block: ParamBuilder.() -> Unit)
    fun handler(block: suspend (params: Map<String, Any>) -> AppIntentResult)
}
```

### `ParamType`

```kotlin
enum class ParamType {
    String, Int, Boolean, Double, Entity
}
```

### `AppIntentResult` sealed class

```kotlin
sealed class AppIntentResult {
    data class Dialog(val message: String) : AppIntentResult()
    data class Snippet(val markdown: String) : AppIntentResult()
    object Done : AppIntentResult()
    data class Failed(val message: String) : AppIntentResult()
}
```

---

## Notes

- **iOS Spotlight**: `invokeForTesting` works on Simulator; Spotlight index surfacing requires
  verification on a real device with Siri enabled.
- **Android**: App Actions require `shortcuts.xml` in the app module — `cmp-app-intents`
  generates this at build time from your registered config.
- **Swift bridge**: copy `cmp-app-intents/swift/CmpAppIntentBridge.swift` into your `iosApp`
  Xcode target — see [SETUP.md](SETUP.md) Step 4.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps (includes iOS Swift bridge installation)
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-app-intents`
