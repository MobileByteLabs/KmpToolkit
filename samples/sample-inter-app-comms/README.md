# sample-inter-app-comms

End-to-end sample app exercising the three new `cmp-*` modules from the
`inter-app-comms-suite` epic:

- **cmp-share** — OS-native share sheet (text / url / image)
- **cmp-intent-launcher** — typed Intent builder with ActivityResult (image picker, doc picker)
- **cmp-app-intents** — declarative App Intents DSL (intent registration + test invocation)

## Run on Android

```bash
./gradlew :samples:sample-inter-app-comms:androidApp:installDebug
# Then launch "Inter-App Comms Demo" from your emulator / device
```

## Run on Desktop (JVM)

```bash
./gradlew :samples:sample-inter-app-comms:composeApp:run
# Opens a 700×800 window
```

## Run on Web (wasmJs)

```bash
./gradlew :samples:sample-inter-app-comms:composeApp:wasmJsBrowserDevelopmentRun
# Auto-opens browser at http://localhost:8080/
```

## Run on iOS

No `iosApp/` xcodeproj shipped in v0.1. To run on iOS:

1. Create an Xcode project that depends on the framework `ComposeApp` produced by
   `:samples:sample-inter-app-comms:composeApp` (run `./gradlew :samples:sample-inter-app-comms:composeApp:linkPodReleaseFrameworkIosArm64`
   or your preferred linkage task).
2. Call `MainViewControllerKt.MainViewController()` from your Swift / SwiftUI host.
3. To exercise cmp-app-intents end-to-end, also drop
   `cmp-app-intents/swift/CmpAppIntentBridge.swift` into your Xcode target and call
   `CmpAppIntentBridge.shared.loadManifest()` at app launch.

iosApp scaffold deferred to sub-plan 07 follow-up.

## What the UI does

Three tabs (TabRow):

| Tab | Calls |
|---|---|
| **Share** | `Share.text(...)`, `Share.url(...)`, `Share.image(pngBytes, "image/png")` |
| **Intent** | `rememberIntentLauncher().launch { result(ResultContracts.PickImage); type("image/*") }` etc. |
| **AppIntents** | Registers two intents via `appIntents { ... }` then invokes via `AppIntents.invokeForTesting(...)` |

Each result is surfaced in a status row at the bottom of the screen.

## Platform behaviour summary

- **Android**: full Intent.ACTION_SEND chooser; real PhotoPicker / Document Picker via ActivityResult
- **iOS**: UIActivityViewController for share; picker contracts route through `onUnsupported` (v0.2 polish adds PHPicker)
- **Desktop (JVM)**: clipboard for share-text/url; FileDialog (LOAD) for pickers
- **Web (wasmJs)**: `navigator.share` for share; picker via `onUnsupported` in v0.1
- All platforms: `AppIntents.invokeForTesting(...)` works (programmatic invocation; bypasses OS)
