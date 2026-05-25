# sample-cmp-intent-launcher

Per-module sample app exercising `cmp-intent-launcher` v0.1.0 — typed Android-Intent builder + ActivityResult; iOS picker whitelist.

Plan: [`09-per-module-samples`](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md)

## Targets

| Platform | How to run |
|---|---|
| **Android** | `./gradlew :samples:sample-cmp-intent-launcher:androidApp:installDebug` then open from launcher |
| **iOS** | Open `samples/sample-cmp-intent-launcher/composeApp/iosApp.xcodeproj` (xcodeproj scaffold deferred — see D9). Until then: `./gradlew :samples:sample-cmp-intent-launcher:composeApp:linkPodDebugFrameworkIosSimulatorArm64` produces the framework |
| **JVM Desktop** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:run` |
| **Web (JS)** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:jsBrowserDevelopmentRun` |
| **Web (wasmJs)** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:wasmJsBrowserDevelopmentRun` |

## What this sample demonstrates

| Screen | What it exercises |
|---|---|
| **Intent Launch** | `rememberIntentLauncher().launch { action(...); type(...) }` — ACTION_PICK for image/*, observes `IntentResult.Ok/Cancelled/Failed` |
| **Intent Builder** | Full DSL — custom action, MIME type, data URI, and preset extras via `IntentBuilder` |

### Per-platform expected behaviour

| Platform | What happens |
|---|---|
| Android | `ActivityResult` API dispatches the typed intent; returns `IntentResult.Ok(IntentData)` / `.Cancelled` / `.Failed` |
| iOS | Picker contracts via `UIDocumentPickerViewController` / `PHPickerViewController` |
| macOS | `NSOpenPanel` anchored to key window's contentView |
| JVM Desktop | AWT `FileDialog` (LOAD mode) on `Dispatchers.IO` |
| Web (JS / wasmJs) | Hidden `<input type=file>` — **must be called from a user-gesture handler** (`onClick`); otherwise returns `IntentResult.Failed(IntentError.UserGestureMissing)` |
| Non-Android (non-picker intents) | Returns `IntentResult.Failed(IntentError.UnsupportedPlatform)` — roadmap D1/D2/D3 |

## Acceptance reference

Sub-plan 09 gates G-9.1, G-9.5, G-9.6, G-9.7 — see [09-per-module-samples](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md).
