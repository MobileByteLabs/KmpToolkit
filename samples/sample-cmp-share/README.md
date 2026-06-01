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

---

## Smoke-test rubric (per platform, added 2026-06-01 by cmp-intent-share-coverage-trueup sub-plan 04)

The Home screen's **"Try It (per-platform smoke test)"** card opens [`TryItPanel`](composeApp/src/commonMain/kotlin/io/github/mobilebytelabs/kmptoolkit/sample/cmpshare/TryItPanel.kt) — four buttons (Share text / URL / image / file) that invoke the production `Share.share()` API on whichever target is running and surface the typed `ShareResult` in the UI.

| Target | Run command | Expected behavior |
|---|---|---|
| **Android** | `./gradlew :samples:sample-cmp-share:composeApp:installDebug` (D9 wrapper Activity deferred); launch from launcher | "Share text" → native chooser appears → pick app → `Completed` |
| **iOS** | `./gradlew :samples:sample-cmp-share:composeApp:iosDeployIPhone15ProRelease` | "Share text" → UIActivityViewController appears → pick / dismiss → `Completed` / `Cancelled` |
| **JVM Desktop** | `./gradlew :samples:sample-cmp-share:composeApp:run` | "Share text" → text copied to clipboard via AWT fallback → `Completed` |
| **JS Browser** | `./gradlew :samples:sample-cmp-share:composeApp:jsBrowserDevelopmentRun` | "Share text" → on mobile Safari/Chrome → native share sheet; on desktop browsers → `Failed(NoHandler)` if no `navigator.share` support |
| **wasmJs Browser** | `./gradlew :samples:sample-cmp-share:composeApp:wasmJsBrowserDevelopmentRun` | Same as JS |

**Expected sad-path results:**

| Button | Platform | Result |
|---|---|---|
| Share image (placeholder PNG) | Android / iOS / macOS | `Completed` — bytes routed via FileProvider / UIActivityViewController. The bytes are a 4-byte PNG magic prefix so OS may flag "invalid image" but the API path is exercised |
| Share image | Linux | `Completed` — bytes materialized to `/tmp/cmp-share-*` then `xdg-open` opens the default image handler |
| Share image | JS / wasmJs | `Completed` on browsers with `navigator.canShare({files})` (Chromium 88+, Safari 15+); else `Failed(NoHandler)` |
| Share file (data: URI) | Most platforms | `Failed(NoHandler)` — `SharePayload.File` expects a real `file://` URI; the `data:` URI shows the error path |
| Any share | Without user-gesture on JS | `Failed(UserGestureMissing)` — must be invoked from click handler |
