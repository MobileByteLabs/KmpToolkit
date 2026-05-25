# sample-cmp-app-intents

Per-module sample app exercising `cmp-app-intents` v0.1.0 — declarative SiriKit Shortcuts + Spotlight (iOS 16+); Android on-device App Actions registry.

Plan: [`09-per-module-samples`](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md)

## Targets

| Platform | How to run |
|---|---|
| **Android** | `./gradlew :samples:sample-cmp-app-intents:androidApp:installDebug` then open from launcher |
| **iOS** | Open `samples/sample-cmp-app-intents/composeApp/iosApp.xcodeproj` (xcodeproj scaffold deferred — see D9). Until then: `./gradlew :samples:sample-cmp-app-intents:composeApp:linkPodDebugFrameworkIosSimulatorArm64` produces the framework |
| **JVM Desktop** | `./gradlew :samples:sample-cmp-app-intents:composeApp:run` |
| **Web (JS)** | `./gradlew :samples:sample-cmp-app-intents:composeApp:jsBrowserDevelopmentRun` |
| **Web (wasmJs)** | `./gradlew :samples:sample-cmp-app-intents:composeApp:wasmJsBrowserDevelopmentRun` |

## What this sample demonstrates

| Screen | What it exercises |
|---|---|
| **Register Intents** | `appIntents { intent("openTransfer") { ... } }` DSL + `AppIntents.register(config)` — shows registered IDs/titles/params |
| **Invoke Intent** | `AppIntents.invokeForTesting("openTransfer", mapOf("amount" to 100))` — displays `AppIntentResult.Dialog/Snippet/Done/Failed` outcome |

### Per-platform expected behaviour

| Platform | What happens |
|---|---|
| iOS (16+) | `register()` writes a JSON manifest consumed by the Swift bridge — Siri Shortcuts and Spotlight entries created |
| macOS (13+) | Same manifest mechanism via AppKit bridge |
| Android | On-device App Actions XML registered via intent filter (v0.2 — see plan D5) |
| JVM / JS / wasmJs | Manifest held in-memory only; `invokeForTesting` calls the perform lambda directly |

### Note on test-mode invocation

`invokeForTesting()` bypasses Siri/Spotlight and directly invokes the registered `perform` lambda — useful for verifying intent logic in-app without OS integration. Real assistant integration ships in v0.2 (plan D5/D6/D7/D8).

## Acceptance reference

Sub-plan 09 gates G-9.1, G-9.6, G-9.7 — see [09-per-module-samples](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md).
