# Inter-App Comms Suite — Capability Matrix (v1.0)

> Canonical 3-module × KMP-target × per-API support matrix. Cells: ✅ real native impl · ⚠️ fallback (works but not native UX) · ❌ ADR-09 #N WONTFIX (architectural OS limitation) · 🔗 v0.4 PROVISIONAL (compiles; runtime verification pending real-device CI) · ⊘ target not declared.
>
> See [`cmp-app-intents/adrs/ADR-09-platform-impl-exits.md`](../../cmp-app-intents/adrs/ADR-09-platform-impl-exits.md) for the full WONTFIX rationale.

## cmp-share (19 declared targets)

| Target | Text | Url | Image | File | Multi |
|---|:---:|:---:|:---:|:---:|:---:|
| `androidLibrary` | ✅ ACTION_SEND | ✅ ACTION_SEND | ✅ ACTION_SEND | ✅ ACTION_SEND | ✅ ACTION_SEND |
| `jvm` | ⚠️ AWT clipboard | ✅ OS-detect xdg-open / open / start | ⚠️ AWT clipboard | ⚠️ AWT FileDialog | ⚠️ AWT clipboard |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✅ UIActivityViewController | ✅ UIActivityViewController | ✅ UIActivityViewController | ✅ UIActivityViewController | ✅ UIActivityViewController |
| `macosX64` / `macosArm64` | ✅ NSSharingServicePicker | ✅ NSSharingServicePicker | ✅ NSSharingServicePicker | ✅ NSSharingServicePicker | ✅ NSSharingServicePicker |
| `tvosX64` / `tvosArm64` / `tvosSimulatorArm64` | 🔗 Swift bridge | 🔗 Swift bridge | ❌ #1 | ❌ #1 | ❌ #1 |
| `watchosX64` / `watchosArm32` / `watchosArm64` / `watchosSimulatorArm64` / `watchosDeviceArm64` | ✅ WCSession.transferUserInfo | ✅ WCSession.transferUserInfo | ✅ WCSession.transferFile | ✅ WCSession.transferFile | ✅ first-success |
| `linuxX64` / `linuxArm64` | ✅ xclip | ✅ xdg-open | ⚠️ deferred | ✅ xdg-open | ✅ first-success |
| `mingwX64` | ✅ clip.exe | ✅ cmd start | 🔗 CF_DIB cinterop | ✅ cmd start | ✅ first-success |
| `js` (browser) | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share |
| `wasmJs` (browser) | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share | ✅ navigator.share |

## cmp-intent-launcher (19 declared) + cmp-intent-launcher-compose (9 declared)

| Target | PickImage | PickDocument | PickContact | Arbitrary URL |
|---|:---:|:---:|:---:|:---:|
| `androidLibrary` | ✅ PHPicker-equiv | ✅ ACTION_GET_CONTENT | ✅ ContactsContract | ✅ ACTION_VIEW |
| `jvm` | ✅ AWT FileDialog | ✅ AWT FileDialog | ❌ #7 (no JVM contact picker) | ⚠️ Desktop.browse |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✅ PHPicker | ✅ UIDocumentPicker | ✅ CNContactPicker | ✅ UIApplication.openURL |
| `macosX64` / `macosArm64` | ✅ NSOpenPanel | ✅ NSOpenPanel | ❌ #4 (deferred) | ✅ NSWorkspace.openURL |
| `tvosX64` / `tvosArm64` / `tvosSimulatorArm64` | ❌ #5 | ❌ #5 | ❌ #5 | ❌ #5 |
| `watchosX64` / `watchosArm32` / `watchosArm64` / `watchosSimulatorArm64` | ❌ #6 | ❌ #6 | ❌ #6 | ✅ WKExtension.openSystemURL |
| `linuxX64` / `linuxArm64` | ✅ zenity | ✅ zenity | ❌ #7 (architectural) | ✅ xdg-open |
| `mingwX64` | 🔗 GetOpenFileNameW | 🔗 GetOpenFileNameW | ❌ #8 (UWP-only API) | ✅ cmd start |
| `js` (browser) | ✅ `<input type=file>` | ✅ `<input type=file>` | ❌ no API | ⚠️ window.open |
| `wasmJs` (browser) | ✅ `@JsFun` DOM | ✅ `@JsFun` DOM | ❌ no API | ⚠️ window.open |

**Compose Multiplatform target reach** (`cmp-intent-launcher-compose`): 9 targets — `androidLibrary` + `jvm` + iOS×3 + macOS×2 + js + wasmJs. tvOS/watchOS/Linux/mingw consumers use the core `cmp-intent-launcher` module's imperative `IntentLauncher` class directly (Compose Multiplatform doesn't ship `compose.runtime` for those targets — Phase 0 S1.A verdict UNCHANGED).

### cmp-intent-launcher `SystemIntents` (v0.4 Phase 13 — lifecycle-free entry points)

Callable from commonMain / DI / non-UI code. No Composable / ActivityResultLauncher needed. Android Context auto-injected via `IntentLauncherInitProvider`.

| Target | `openAppSettings()` | `createDocument(name, mime)` |
|---|:---:|:---:|
| `androidLibrary` | ✅ ACTION_APPLICATION_DETAILS_SETTINGS + FLAG_ACTIVITY_NEW_TASK | ✅ Invisible proxy Activity + ActivityResultContracts.CreateDocument |
| `jvm` | ✅ OS-aware ProcessBuilder | ✅ JFileChooser SAVE_DIALOG on EDT (via Dispatchers.IO) |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✅ UIApplicationOpenSettingsURLString | ✅ UIDocumentPickerViewController (ExportToService) + DelegatePin |
| `macosX64` / `macosArm64` | ✅ x-apple.systempreferences | ✅ NSSavePanel.runModal() |
| `tvosX64` / `tvosArm64` / `tvosSimulatorArm64` | ❌ #5 | ❌ #5 |
| `watchosX64` / `watchosArm32` / `watchosArm64` / `watchosSimulatorArm64` / `watchosDeviceArm64` | ❌ #6 | ❌ #6 |
| `linuxX64` / `linuxArm64` | ✅ gnome-control-center / kcmshell5 / xdg-open chain | ✅ zenity --file-selection --save |
| `mingwX64` | ✅ start ms-settings:appsfeatures | ❌ #13 (Win32 GetSaveFileNameW cinterop deferred) |
| `js` (browser) | ❌ #10 (no app-scoped settings) | ✅ window.showSaveFilePicker (Chromium only; ❌ on Firefox/Safari) |
| `wasmJs` (browser) | ❌ #10 | ✅ window.showSaveFilePicker via `@JsFun` (Chromium only) |

New ADR-09 rows from v0.4 Phase 13: **#13** mingw `SystemIntents.createDocument` (GetSaveFileNameW cinterop) — WONTFIX-PROVISIONAL until follow-up cinterop expansion.

## cmp-app-intents (19 declared) + cmp-app-intents-compose (9 declared)

| Target | register() | Runtime registry | OS surface |
|---|:---:|:---:|---|
| `androidLibrary` | ✅ | ✅ in-memory | ✅ shortcuts.xml capability tags (BIIs) via Phase 5 `generateShortcutsXml` Gradle task |
| `jvm` | ⚠️ no-op | ✅ in-memory | ❌ #9 architectural |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✅ | ✅ in-memory | ✅ Swift bridge + AppShortcutsProvider (per ADR-04) + auto-generated Swift stubs via Phase 5 `generateSwiftIntents` |
| `macosX64` / `macosArm64` | ✅ | ✅ in-memory | ✅ same as iOS |
| `tvosX64` / `tvosArm64` / `tvosSimulatorArm64` | ✅ v0.4 manifest write | ✅ in-memory | ✅ manifest + Swift bridge (CoreSpotlight no-op via #if canImport guard) |
| `watchosX64` / `watchosArm32` / `watchosArm64` / `watchosSimulatorArm64` | ✅ v0.4 manifest write | ✅ in-memory | ✅ manifest + Swift bridge (watchOS 10+ Shortcuts) |
| `linuxX64` / `linuxArm64` | ✅ v0.4 XDG manifest + .desktop | ✅ in-memory | ✅ `.desktop` action handlers in `$XDG_DATA_HOME/applications/` |
| `mingwX64` | ✅ v0.4 APPDATA manifest | ✅ in-memory | 🔗 IShellLinkW Start Menu shortcuts deferred to post-v0.4 CI verification |
| `js` (browser) | ⚠️ no-op | ✅ in-memory | ❌ #10 architectural (PWA shortcut manifest = separate future `cmp-pwa-shortcuts` module) |
| `wasmJs` (browser) | ⚠️ no-op | ✅ in-memory | ❌ #10 architectural |

**Compose Multiplatform target reach** (`cmp-app-intents-compose`): same 9 as above. Consumers use `AppIntentsRegistration(config)` Composable for lifecycle-bound registration + `AppIntentsRegistry()` for Material 3 styled dev/debug UI.

## ADR-09 status at v1.0 ship

12 v0.4 candidate rows from v0.3-alpha:
- **✅ CLOSED**: #1 (tvOS partial Swift bridge wiring); #2 (watchOS transferFile); #3 (mingw CF_DIB cinterop); #4 (JVM OS-detect ProcessBuilder); #8 (mingw GetOpenFileNameW cinterop); #11 (all 4 tier-3 manifest writes); #12 (Gradle codegen tasks).
- **WONTFIX (architectural)**: #5 (tvOS picker surface non-existent); #6 (watchOS picker surface non-existent); #7 (Linux contact-picker absent); #9 (JVM Desktop no canonical intent surface); #10 (JS/wasmJs no canonical Web intent surface — PWA shortcuts = future module).

See [`ADR-09`](../../cmp-app-intents/adrs/ADR-09-platform-impl-exits.md) for the per-row rationale + post-v1.0 follow-up guidance.

## Compose vs non-Compose decision tree

```
Are you using Compose Multiplatform in commonMain?
├── YES  → Add `cmp-{share|intent-launcher|app-intents}-compose` deps
│         Use `rememberShareLauncher()` / `IntentPickerDialog()` /
│         `AppIntentsRegistration()` opinionated Composables
│
└── NO   → Add `cmp-{share|intent-launcher|app-intents}` core deps only
          Use `Share.share(...)` / `IntentLauncher()` /
          `AppIntents.register(...)` imperative APIs directly
```
