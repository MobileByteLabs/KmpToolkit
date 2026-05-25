# cmp-app-intents Swift sources

This directory holds Swift source file(s) that consumer iOS / macOS apps drop into
their Xcode project target to enable App Intents (SiriKit Shortcuts + Spotlight).

> **Phase 0 spike finding TS2 (locked):** Following the `cmp-deep-link/swift/`
> precedent — these are **source files**, NOT a Swift Package Manager package.
> No `Package.swift`, no SPM dependency declaration, no cinterop integration
> in Gradle. Consumer apps manually copy the `.swift` file(s) into their Xcode
> app target.

## Status

Phase 1 placeholder. The actual `CmpAppIntentBridge.swift` + per-intent stub
template lands in sub-plan 06 of the `inter-app-comms-suite` epic.

## Future contents (per sub-plan 06)

| File | Purpose |
|---|---|
| `CmpAppIntentBridge.swift` | Generic Swift class providing `CmpAppIntentBridge.shared` singleton; reads manifest JSON written by Kotlin's `AppIntents.register(config)`; routes invocations back into Kotlin via `AppIntentsCallback.shared` |
| `templates/AppIntentStub.swift.template` | Per-intent `@AppIntent` stub template with `${INTENT_ID}` + `${PARAMS}` placeholders; consumer copies once per declared intent + customizes |
| `README.md` (consumer-facing) | 4-step adoption walkthrough + screenshots + troubleshooting |

## Consumer setup (future state — Phase 5/8)

1. Copy `CmpAppIntentBridge.swift` into your Xcode app target
2. For each intent declared in your Kotlin `appIntents { … }` block, copy the
   stub template, replace placeholders, save as e.g. `MyIntent_openTransfer.swift`,
   add to Xcode target
3. In your app's `didFinishLaunching` (UIKit) or `init` (SwiftUI), call
   `CmpAppIntentBridge.shared.loadManifest()`
4. Xcode compiles `@AppIntent` macros normally; intents register at app launch
   via `@AppShortcutsProvider`

iOS 16+ enforced via runtime `if #available(iOS 16, *)` checks in the shipped
Swift code, NOT via library-wide build-script lock (per Phase 0 TS1).

## Reference

- Epic plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/`
- Spike findings: `inter-app-comms-suite/SPIKE_FINDINGS.md` — TS2 (Swift mechanism)
- Sub-plan 06: `inter-app-comms-suite/06-cmp-app-intents.md` — implementation tasks
- Precedent: `cmp-deep-link/swift/DeepLinkPlugin.swift`
