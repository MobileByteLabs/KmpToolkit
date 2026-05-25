# cmp-app-intents

Declarative App Intents DSL for SiriKit Shortcuts + Spotlight (iOS 16+) and
on-device runtime registry (Android, JVM, JS, wasmJs).

> **Status:** Experimental. All public APIs marked `@ExperimentalAppIntentsApi`.
> Ships alongside the other `cmp-*` modules at the shared `kmptoolkit.version`.

## Features

- **Declarative DSL**: `appIntents { intent("openTransfer") { ... } }` — author once, register everywhere
- **iOS App Intents integration**: ship-source-file pattern (consumer drops `.swift` files into Xcode)
- **JSON manifest**: Kotlin emits the manifest; Swift bridge reads it at app launch
- **Test helper**: `AppIntents.invokeForTesting(id, params)` — bypass OS, exercise `perform` lambdas in unit tests
- **Parameter type system**: `Text`, `Integer`, `Number`, `Bool`, `Entity(name)`

## Platform support

| Platform | Behaviour | v0.1 status |
|----------|-----------|------------|
| iOS 16+ | Writes manifest JSON for `CmpAppIntentBridge.swift` consumer file to read; Spotlight + Shortcuts integration via `@AppShortcutsProvider` | ✅ Complete (consumer-side Swift wiring required) |
| macOS 13+ | Same `CmpAppIntentBridge.swift` works for macOS; App Shortcuts on Sonoma+ | ✅ Complete |
| Android | On-device runtime registry; test via `adb shell am broadcast` | ✅ Complete — **Google Assistant integration DEFERRED to v0.2 `cmp-app-intents-assistant`** |
| JVM (Desktop) | No-op + `invokeForTesting` helper | ✅ Complete |
| JS / wasmJs | No-op + `invokeForTesting` helper | ✅ Complete |

> **iOS 16+ requirement** enforced via runtime `if #available(iOS 16, *)` checks in
> the shipped Swift code — NOT via library build-script lock (which would break the
> 13 other modules' consumers on older iOS). On iOS 14–15, the Swift bridge no-ops.

> **Not targeted:** tvOS, watchOS, Linux native, mingwX64, wasmWasi.

## Install

```kotlin
// build.gradle.kts
dependencies {
    val kmptoolkit = "3.2.13" // or latest
    implementation("io.github.mobilebytelabs:cmp-app-intents:$kmptoolkit")
}
```

## Quick start

### Declare intents in Kotlin

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
val config = appIntents {
    intent("openTransfer") {
        title = "Open Transfer"
        description = "Opens the money transfer screen, optionally pre-filling the amount."
        parameter("amount", ParamType.Number, isRequired = false)
        searchable(category = "Money")
        perform { params ->
            val amount = params["amount"] as? Double
            navigationCoordinator.openTransfer(prefillAmount = amount)
            AppIntentResult.Dialog("Opened transfer for ${amount ?: "no preset amount"}")
        }
    }
    intent("checkBalance") {
        title = "Check Balance"
        description = "Reads the current account balance."
        searchable()
        perform { _ ->
            val balance = balanceRepo.current()
            AppIntentResult.Snippet("**Balance:** $${balance}")
        }
    }
}
```

### Register at app launch

#### iOS / macOS (Compose-MP MainViewController)

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
fun MainViewController() = ComposeUIViewController {
    LaunchedEffect(Unit) { AppIntents.register(config) }
    App()
}
```

#### Android (Application class)

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppIntents.register(config)
    }
}
```

### Test (commonTest / JVM)

```kotlin
@Test
fun openTransfer_withAmount_returnsDialog() = runTest {
    AppIntents.register(config)
    val result = AppIntents.invokeForTesting("openTransfer", mapOf("amount" to 50.0))
    assertIs<AppIntentResult.Dialog>(result)
}
```

## iOS consumer setup (file-copy workflow)

The library ships two Swift artifacts in `cmp-app-intents/swift/`:

| File | Action |
|---|---|
| `CmpAppIntentBridge.swift` | Copy ONCE into your Xcode iOS / macOS app target |
| `templates/AppIntentStub.swift.template` | Copy ONCE PER INTENT; replace `${INTENT_ID}` + `${PARAMS_BLOCK}` + `${PARAMS_DICT}` placeholders |

At app launch (SwiftUI `App.init` or UIKit `AppDelegate.didFinishLaunchingWithOptions`):

```swift
import AppIntents

@main
struct MyApp: App {
    init() {
        CmpAppIntentBridge.shared.loadManifest()  // reads JSON written by Kotlin AppIntents.register(...)
    }
    var body: some Scene { WindowGroup { ContentView() } }
}

@available(iOS 16, *)
struct MyAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenTransferIntent(),  // your per-intent Swift stub
            phrases: ["Open transfer in \(.applicationName)"],
            shortTitle: "Open Transfer",
            systemImageName: "arrow.left.arrow.right.circle",
        )
    }
}
```

See `cmp-app-intents/swift/CmpAppIntentBridge.swift` for the full bridge contract +
`cmp-app-intents/swift/templates/AppIntentStub.swift.template` for the per-intent
template.

## Android consumer setup

Just call `AppIntents.register(config)` from `Application.onCreate`. The library
registers a `BroadcastReceiver` programmatically — no manifest entries needed.

### Test via adb

```
adb shell am broadcast -a cmp.appintents.INVOKE \
  -e intent_id openTransfer \
  --es amount 100
```

### v0.1 limitation — Google Assistant integration deferred to v0.2

The `actions.xml` + `BUILT_IN_INTENT` filter approach has been in maintenance mode
since ~2024. We defer Assistant rollout to v0.2 `cmp-app-intents-assistant` follow-up,
which will follow current Google guidance (Assistant Schema + App Engagement APIs).

For Assistant integration NOW, wire `actions.xml` in your app target per current
Google docs; cmp-app-intents on Android v0.1 only registers the on-device runtime
registry for the BroadcastReceiver invocation path.

## See also

- SPEC: [idea-layer/modules/cmp-app-intents/SPEC.md](../../../idea-layer/modules/cmp-app-intents/SPEC.md)
- API: [idea-layer/modules/cmp-app-intents/API.md](../../../idea-layer/modules/cmp-app-intents/API.md)
- ADRs: [idea-layer/modules/cmp-app-intents/adrs/](../../../idea-layer/modules/cmp-app-intents/adrs/)
- Sibling modules: [cmp-share](../cmp-share/), [cmp-intent-launcher](../cmp-intent-launcher/), [cmp-deep-link](../cmp-deep-link/)
- Sample app: [samples/sample-inter-app-comms/](../samples/sample-inter-app-comms/)
