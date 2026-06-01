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

---

## Smoke-test rubric (per platform, added 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 04)

The Home screen's **"Try It (per-platform smoke test)"** card opens [`TryItPanel`](composeApp/src/commonMain/kotlin/io/github/mobilebytelabs/kmptoolkit/sample/cmpintentlauncher/TryItPanel.kt) — three buttons that exercise the production `IntentLauncher` / `SystemIntents` API on whichever target is running and surface the typed `IntentResult` in the UI.

| Target | Run command | Expected behavior |
|---|---|---|
| **Android** | Install APK via `./gradlew :samples:sample-cmp-intent-launcher:composeApp:installDebug` (D9 wrapper Activity deferred — see Out-of-scope in the sub-plan); then launch from launcher | "Open URL" → default browser opens kotlinlang.org → `Result: Ok(https://kotlinlang.org)` |
| **iOS** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:iosDeployIPhone15ProRelease` (or via xcodeproj when D9 lands) | Same — Safari opens; `Ok` |
| **JVM Desktop** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:run` | "Open URL" → `Desktop.browse()` opens default browser → `Ok` |
| **JS Browser** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:jsBrowserDevelopmentRun` | "Open URL" → new tab opens; `Ok` (must click button — user-gesture required) |
| **wasmJs Browser** | `./gradlew :samples:sample-cmp-intent-launcher:composeApp:wasmJsBrowserDevelopmentRun` | "Open URL" → new tab opens; `Ok` |

**Expected sad-path results:**

| Button | Platform | Result |
|---|---|---|
| Pick image | JVM headless | `Failed(NoHandler)` or `Failed(Unknown)` (AWT requires display) |
| Pick image | JS without user-gesture | `Failed(UserGestureMissing)` — call must be from click handler |
| Open app settings | JS / wasmJs | `Failed(UnsupportedPlatform)` — browsers have no app-scoped settings |
| Open app settings | tvOS / watchOS | `Failed(UnsupportedPlatform)` — locked by ADR-09 |
