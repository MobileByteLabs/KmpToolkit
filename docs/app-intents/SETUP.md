# cmp-app-intents — Integration Guide

> `io.github.mobilebytelabs:cmp-app-intents:3.2.11`

`cmp-app-intents` requires 4 steps: add dependency, opt in, call `AppIntents.register()` at
app startup, and (iOS only) install the Swift bridge into your Xcode target.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
cmp-app-intents = "3.2.11"

[libraries]
cmp-app-intents = { module = "io.github.mobilebytelabs:cmp-app-intents", version.ref = "cmp-app-intents" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.cmp.app.intents)
}
```

---

## Step 2 — Opt in to the Experimental Marker

### Per call-site (recommended)

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
fun initIntents() {
    AppIntents.register(appIntents { /* ... */ })
}
```

### Project-wide (all modules)

```kotlin
// shared/build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi"
        )
    }
}
```

---

## Step 3 — Register Intents at App Startup

Call `AppIntents.register(...)` from your shared `App()` init path (e.g. in
`application {}` block or `App.kt` `init` block):

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
fun initApp() {
    AppIntents.register(
        appIntents {
            intent("open_search") {
                title("Search Products")
                param("query") { type = ParamType.String }
                handler { params ->
                    val q = params["query"] as? String ?: ""
                    navigator.navigateToSearch(q)
                    AppIntentResult.Done
                }
            }
            intent("show_dashboard") {
                title("Open Dashboard")
                handler { AppIntentResult.Done }
            }
            intent("get_balance") {
                title("Get Account Balance")
                handler {
                    val balance = repository.getBalance()
                    AppIntentResult.Snippet("Balance: $balance")
                }
            }
        }
    )
}
```

### Testing intents without a device

```kotlin
@OptIn(ExperimentalAppIntentsApi::class)
suspend fun testIntent() {
    val result = AppIntents.invokeForTesting(
        id = "get_balance",
        params = emptyMap()
    )
    println(result) // AppIntentResult.Snippet("Balance: ...")
}
```

---

## Step 4 — iOS Swift Bridge Installation

The Kotlin side writes a manifest JSON; the Swift bridge consumes it at app launch
to (a) wire the Kotlin↔Swift callback for `@AppIntent.perform()` bodies and
(b) push searchable intents into iOS Spotlight via `CSSearchableIndex`.

1. Copy `cmp-app-intents/swift/CmpAppIntentBridge.swift` into your `iosApp` Xcode target
   (File → Add Files to "iosApp"). Add the `CoreSpotlight.framework` to your target's
   "Frameworks, Libraries, and Embedded Content" list.
2. Bootstrap the bridge at app launch — SwiftUI:
   ```swift
   import SwiftUI
   @main struct MyApp: App {
       init() {
           CmpAppIntentBridge.shared.bootstrap {
               // Return the @ObjCName-exposed Kotlin singleton.
               // Replace `kmptoolkit` with your Kotlin/Native framework alias.
               return CmpAppIntentsCallback.shared
           }
       }
       var body: some Scene { WindowGroup { ContentView() } }
   }
   ```
3. (Optional) Handle Spotlight item taps so users land in the right screen:
   ```swift
   ContentView()
       .onContinueUserActivity("\(CmpAppIntentBridge.activityPrefix).openTransfer") { activity in
           _ = CmpAppIntentBridge.shared.handleContinue(activity)
       }
   ```
   Add each `<CmpAppIntentBridge.activityPrefix>.<intent-id>` you support to your
   Info.plist `NSUserActivityTypes` array.
4. Copy `cmp-app-intents/swift/templates/AppIntentStub.swift.template` once per intent
   you declared in Kotlin, substitute `${INTENT_ID}` / `${PARAMS_BLOCK}` / `${PARAMS_DICT}`,
   add to your `AppShortcutsProvider.appShortcuts` array. Xcode's `@AppIntent` macro
   handles SiriKit registration automatically (iOS 16+ / macOS 13+).

> **macOS**: same process — copy `CmpAppIntentBridge.swift` into your `macosApp` target.
> `CoreSpotlight` is also available on macOS 10.13+, so searchable intents work
> identically.

### Android App Actions XML (auto-generated)

On Android, `cmp-app-intents` generates `shortcuts.xml` during the build.  No manual step
is required; the Gradle plugin adds the `<meta-data>` entry to your merged `AndroidManifest.xml`
automatically.

---

## Platform Notes

| Platform | Behaviour |
|----------|-----------|
| Android  | On-device registry via App Actions; `shortcuts.xml` auto-generated at build time |
| iOS / macOS | Manifest JSON written by Kotlin; `CmpAppIntentBridge.bootstrap()` consumes it — wires the Kotlin↔Swift callback, indexes `searchable: true` intents into Spotlight via `CSSearchableIndex`, exposes `handleContinue(_:)` for tap-to-resume |
| JVM / JS / wasmJs | `invokeForTesting` only — no OS integration |

---

## AI-Assisted Setup

```
/sync-app-intents           # Verify Gradle dependency (only gate needed)
/sync-app-intents --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
