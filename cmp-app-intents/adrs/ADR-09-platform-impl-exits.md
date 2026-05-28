# ADR-09 — Platform Impl Exits Catalog (v0.3)

## Context

The `inter-app-comms-real-native-impls` v0.3 epic (2026-05-28) commits to "real native impl on every declared target" per [GOAL.md](../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-real-native-impls/GOAL.md) §Locked decisions D1, D2. In practice, some platform exits remain because:
1. The host OS truly lacks an equivalent API, OR
2. The Kotlin/Native binding requires non-trivial interop work that exceeds v0.3 timeline.

This ADR enumerates every surviving `UnsupportedPlatform` / no-op exit across the 3 IPC modules — `cmp-share`, `cmp-intent-launcher`, `cmp-app-intents` — with file:line citation + OS-API justification.

## Decision

Every `UnsupportedPlatform` / no-op exit in the 3 IPC modules carries a `// ADR-09:` inline comment AND a row in the Consequences table below.

## Status

Accepted 2026-05-28 (initial). Audit refreshed at every Phase 6 of inter-app-comms-real-native-impls execution.

## Consequences

| # | Module | File | Exit type | OS-API justification | v0.4 candidate |
|---|---|---|---|---|---|
| 1 | cmp-share | `Share.tvos.kt` | SharePayload.* → UnsupportedPlatform | K/N tvOS bindings lack UIPasteboard; dynamic ObjC dispatch from Kotlin/Native to the consumer-shipped `CmpShareTvosBridge.swift` requires custom cinterop .def declarations or `@ExportObjCClass`; non-variadic `objc_msgSend` K/N binding blocks runtime dispatch. The Swift file IS shipped for consumers who want to call it from their app code directly. | v0.4: ship `cmp-share-tvos-bridge.def` cinterop for the `CmpShareTvosBridge` Swift class |
| 2 | cmp-share | `Share.watchos.kt` | SharePayload.Image / File / Multi → UnsupportedPlatform | WCSession.transferUserInfo only handles dict-shaped userInfo; binary file transfer requires WCSession.transferFile + companion-app receiver wiring on iOS side — out of v0.3 scope | v0.4 optional `cmp-share-watchos-handoff` companion API |
| 3 | cmp-share | `Share.mingw.kt` | SharePayload.Image → UnsupportedPlatform | Win32 clipboard `CF_DIB` binary write requires `GlobalAlloc` / `GlobalLock` / `SetClipboardData` pointer marshaling that exceeds the basic Win32 cinterop pattern proved by Phase 0 S0.A spike. The `.def` file at `src/mingwMain/cinterop/win32-clipboard.def` is shipped for v0.4 expansion. | v0.4 extend Phase 0 spike .def + add binary image-write Kotlin path |
| 4 | cmp-share | `Share.jvm.kt` | All payloads → AWT clipboard / FileDialog (best-effort, NOT native share sheet) | JVM has no native share-sheet abstraction; macOS-on-JVM cinterop to `NSSharingServicePicker` + Linux-on-JVM `xdg-open` shell-out + Windows-on-JVM `ShellExecuteW` via JNA all require platform-detect branching + new dep (JNA on JVM) that GOAL.md D4 places out of scope (tests + new deps deferred) | v0.4 add `Share.jvm.preferNativeOnMacos` opt-in flag + macOS JNA path |
| 5 | cmp-intent-launcher | `IntentLauncher.tvos.kt` | All ResultContracts + arbitrary → UnsupportedPlatform | tvOS lacks PHPicker / UIDocumentPicker / CNContactPicker (UIKit-only on tvOS). Even the subset (Photos.framework limited tvOS access) requires UIKit delegate bridging out of v0.3 scope | None — tvOS picker surface is genuinely thin |
| 6 | cmp-intent-launcher | `IntentLauncher.watchos.kt` | All ResultContracts → UnsupportedPlatform | watchOS has no native picker surface (no Photos, no DocumentPicker, no Contact picker) | None — watchOS picker surface non-existent |
| 7 | cmp-intent-launcher | `IntentLauncher.linux.kt` | ResultContracts.PickContact → UnsupportedPlatform | Linux has no canonical OS-level contact-picker API (GNOME Evolution / KDE contacts are app-specific; libfolks is a library not a UI) | None — Linux contact-picker is non-standard |
| 8 | cmp-intent-launcher | `IntentLauncher.mingw.kt` | All ResultContracts → UnsupportedPlatform | Win32 `GetOpenFileNameW` cinterop requires `OPENFILENAMEW` struct marshaling that exceeds the simple flat-API Win32 cinterop pattern proven by Phase 0 S0.A spike. The .def stub `src/mingwMain/cinterop/win32-pickers.def` is NOT shipped yet (deferred to v0.4); arbitrary `cmd /c start` URL path works for ACTION_VIEW today. | v0.4 author `win32-pickers.def` + struct marshal layer |
| 9 | cmp-app-intents | `AppIntents.jvm.kt` | register() → no-op | JVM Desktop has no canonical OS-level intent / App Actions API (Windows Cortana, GNOME GLib actions, macOS App Shortcuts on Desktop are all platform-specific JNI work; none of them have a unified abstraction) | None — JVM has no canonical surface; consumers should use `invokeForTesting` for dev |
| 10 | cmp-app-intents | `AppIntents.js.kt`, `AppIntents.wasmJs.kt` | register() → no-op | Web has no canonical OS-level intent API. PWA shortcut Web App Manifest is a separate concern (per GOAL.md D8 — deferred to future `cmp-pwa-shortcuts` module if requested) | future `cmp-pwa-shortcuts` module |
| 11 | cmp-app-intents | `AppIntents.{watchos,tvos,linux,mingw}.kt` | register() → no-op (registry-only stubs, no manifest write) | Phase 5 of inter-app-comms-real-native-impls scoped manifest writes for these targets but the Swift bridge / .desktop emission / IShellLinkW shortcut work exceeds v0.3 timeline. Existing v0.2 registry-only behavior preserved. | v0.4 (Phase 5 work) ship `AppIntents.{watchos,tvos,linux,mingw}.kt` real manifest writers |
| 12 | cmp-app-intents | `cmp-app-intents/build.gradle.kts` | NO `generateShortcutsXml` or `generateSwiftIntents` Gradle task | Per Phase 4 T4 of inter-app-comms-real-native-impls: the build-time codegen tasks require traversal of the registered AppIntentsConfig DSL at Gradle config time, which needs either KSP integration (additional dep) or deserialization from a manifest JSON written by consumer build-time. Deferred — the `AssistantBii` mapper + `AppIntentBuilder.bii` field landed (Phase 4) so the v0.4 task can consume them. | v0.4 Phase 4 polish — author both Gradle tasks |
| 13 | cmp-intent-launcher | `SystemIntents.mingw.kt` | `createDocument(...)` → UnsupportedPlatform | Win32 `GetSaveFileNameW` cinterop requires `OPENFILENAMEW` struct marshal IDENTICAL to v0.4 Row #8 (`GetOpenFileNameW`) but for the SAVE variant — separate function with a different `OPENFILENAMEW` flag (`OFN_OVERWRITEPROMPT`). `win32-pickers.def` only binds the OPEN variant today. The `openAppSettings()` mingw actual works fine via `system("start ms-settings:")`. | Post-v0.4 — extend `win32-pickers.def` with `GetSaveFileNameW` + add `cmpil_alloc_ofnw_save` helper |
| 14 | cmp-intent-launcher | `SystemIntents.{tvos,watchos}.kt` | `openAppSettings()` + `createDocument(...)` → UnsupportedPlatform | tvOS lacks `UIApplicationOpenSettingsURLString` deep-link and `UIDocumentPickerViewController`. watchOS Settings is reachable only via the paired iPhone's Watch app — no programmatic deep-link from watchOS itself. Architectural OS limits, identical to Rows #5 + #6. | None — architectural |
| 15 | cmp-intent-launcher | `SystemIntents.{js,wasmJs}.kt` | `openAppSettings()` → UnsupportedPlatform | Browsers do not expose a per-application settings surface. Closest equivalent (`chrome://settings/content/siteDetails?site={origin}`) is Chromium-specific, blocked from programmatic navigation, and not a stable API. | None — architectural (same family as Row #10) |

## Why this ADR exists

Per [GOAL.md](../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-real-native-impls/GOAL.md) §Acceptance criteria AC1: *"`grep -r 'UnsupportedPlatform|onUnsupported' cmp-share/src/ cmp-intent-launcher/src/ cmp-app-intents/src/` produces zero hits that are not justified by an ADR-09 row."* This ADR is the contract that turns the exit-count grep from "0 exits, ever" (unrealistic) into "0 unjustified exits" (achievable). Every remaining exit is enumerated above; net v0.3 result is real native impl on every target where the OS API + K/N binding combination permits it.

Phases 0-7 of inter-app-comms-real-native-impls landed roughly half of the planned real-impl surface; the other half is documented here as v0.4 candidates. The `cmp-share/swift/CmpShareTvosBridge.swift` ship-source-file + `cmp-share/src/mingwMain/cinterop/win32-clipboard.def` cinterop seed are shipped IN v0.3 as future-use artifacts even though their Kotlin-side dispatch is deferred.

## Supersedes / Related

- **ADR-04 (iOS Swift bridge — ship-source-file)**: superseded in part — the manual ship-source-file pattern stays as opt-out, but the planned `generateSwiftIntents` Gradle task is deferred to v0.4 per Row 12 above.
- **ADR-05 (Android Assistant scope — deferred to separate library)**: superseded — Assistant integration via BIIs + shortcuts.xml (the 2026-current canonical path per SPIKE_FINDINGS_V0_3.md S0.B PIVOT verdict) is folded INTO cmp-app-intents (via `AssistantBii.kt` + AndroidManifest.xml stub); the planned separate `cmp-app-intents-assistant` library closes. Per Row 12 above, the `generateShortcutsXml` Gradle task is deferred to v0.4.
- **ADR-07 (tier-3 EXCLUSION pattern)**: refined for IPC modules — IPC modules DECLARE all 19 targets and use per-symbol `UnsupportedPlatform` exits anchored to rows above. EXCLUSION pattern stays the default for non-IPC modules (e.g., cmp-pdf-generator).

## Audit log

| Date | Audit | Result |
|---|---|---|
| 2026-05-28 | v0.3-alpha Phase 6 T6 audit-grep after Phases 2-5 land | 12 rows enumerated as v0.4 candidates |
| 2026-05-28 | v0.4 (`inter-app-comms-compose-completeness`) Phases 2-5 close-ADR-09 + Phase 11 audit refresh | **8 rows ✅ CLOSED v0.4** (#1 partial — Swift bridge probing; #2 — WCSession.transferFile; #3 — mingw CF_DIB cinterop PROVISIONAL; #4 — JVM OS-detect ProcessBuilder; #8 — mingw GetOpenFileNameW cinterop PROVISIONAL; #11 — all 4 tier-3 manifest writes; #12 — Gradle codegen tasks). **4 rows WONTFIX (architectural)**: #5 tvOS picker surface non-existent; #6 watchOS picker surface non-existent; #7 Linux contact-picker absent; #9 + #10 JVM/JS/wasmJs no canonical Web/Desktop intent surface (PWA shortcuts = future `cmp-pwa-shortcuts` module). |
| 2026-05-28 | v0.4 Phase 13 (`SystemIntents` lifecycle-free entry points) audit add | **3 new rows added**: #13 mingw `SystemIntents.createDocument` (GetSaveFileNameW cinterop deferred to post-v0.4); #14 tvOS/watchOS `SystemIntents.*` (architectural — Settings deep-link non-existent); #15 JS/wasmJs `SystemIntents.openAppSettings` (architectural — browsers have no app-scoped settings). `SystemIntents.openAppSettings` + `SystemIntents.createDocument` close the v0.3 gap where pure-commonMain consumers (e.g. kmp-project-template `IntentManager`) could not reach OS surfaces without per-target source sets. |
