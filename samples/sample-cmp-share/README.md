# sample-cmp-share

Per-module sample app exercising `cmp-share` v0.1.0 — cross-platform share-sheet for text, URL, image, file, and multi-payload sharing.

Plan: [`09-per-module-samples`](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md)

## Targets

| Platform | How to run |
|---|---|
| **Android** | `./gradlew :samples:sample-cmp-share:androidApp:installDebug` then open from launcher |
| **iOS** | Open `samples/sample-cmp-share/composeApp/iosApp.xcodeproj` (xcodeproj scaffold deferred — see D9). Until then: `./gradlew :samples:sample-cmp-share:composeApp:linkPodDebugFrameworkIosSimulatorArm64` produces the framework |
| **JVM Desktop** | `./gradlew :samples:sample-cmp-share:composeApp:run` |
| **Web (JS)** | `./gradlew :samples:sample-cmp-share:composeApp:jsBrowserDevelopmentRun` |
| **Web (wasmJs)** | `./gradlew :samples:sample-cmp-share:composeApp:wasmJsBrowserDevelopmentRun` |

## What this sample demonstrates

| Screen | What it exercises |
|---|---|
| **Basic Share** | `Share.text(...)` and `Share.url(...)` extension funs — single-payload share with default options |
| **Advanced Share** | `Share.multi(payloads, options)` — multi-payload share + `ShareOptions.chooserTitle` |

### Per-platform expected behaviour

| Platform | What happens |
|---|---|
| Android | `Intent.ACTION_SEND` opens the system share chooser; result reported via `Share.share`'s suspend completion |
| iOS | `UIActivityViewController` presented from top-most view controller |
| macOS | `NSSharingServicePicker` presented from key window |
| JVM Desktop | Best-effort clipboard copy + toast (no native share sheet on Linux/Windows; mac uses `Desktop.browse(mailto:?...)` for mail-style) |
| Web (JS / wasmJs) | `navigator.share` when available (Safari, modern Chrome); fallback: `navigator.clipboard.writeText` + on-page hint |

## Acceptance reference

Sub-plan 09 gates G-9.1, G-9.4, G-9.6, G-9.7 — see [09-per-module-samples](../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md).
