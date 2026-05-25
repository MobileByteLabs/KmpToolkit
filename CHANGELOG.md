# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added — v0.2 Platform Parity (cmp-share + cmp-app-intents)

**Toolkit version bumped 3.2.11 → 3.3.0.** Two suite modules now ship 19 KMP targets each — matching the target matrix of `cmp-deep-link` / `cmp-open-url` / `cmp-clipboard`.

- **`cmp-share`** adds `tvosX64/Arm64/SimulatorArm64`, `watchosX64/Arm32/Arm64/SimulatorArm64/DeviceArm64`, `linuxX64/Arm64`, `mingwX64` (11 new targets, 19 total). Real implementations:
  - **Linux**: URL/file share via `xdg-open` subprocess (`xdg-utils` dependency). Single-quote-escaped to prevent shell injection.
  - **mingw (Windows)**: URL/file share via `cmd /c start`. Double-quote-escaped.
  - **tvOS**: `UnsupportedPlatform` fallback — Kotlin/Native bindings (as of Kotlin 2.3.10) don't expose `UIPasteboard` for tvOS even though the Objective-C API exists.
  - **watchOS**: `UnsupportedPlatform` fallback — no share-sheet surface.
- **`cmp-app-intents`** adds same 11 targets (19 total). Tier-3 platforms get registry-only behavior — `AppIntents.register()` stores config in `AppIntentsRuntime`; `invokeForTesting()` works for dev/test on every platform. Swift bridge `CmpAppIntentBridge.swift` updated with `#if canImport(CoreSpotlight)` guards so the same file compiles for iOS, macOS, tvOS, and watchOS — Spotlight indexing silently skips on tvOS/watchOS (no CoreSpotlight on those platforms).
- **`cmp-intent-launcher`** stays at 9 targets. **Constraint discovered during Phase 10.A**: the Compose Compiler Gradle plugin is module-level (not source-set-level) and requires `compose.runtime` on every target's classpath. The `composeMain` intermediate source-set workaround fails because the compiler plugin runs before source-set resolution. Future v0.3 path: split into `cmp-intent-launcher-core` (non-Compose, 19 targets) + `cmp-intent-launcher` (current API, depends on -core). Documented in `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/10-platform-parity-v0-2.md`.
- **wasmWasi** remains excluded — server-side WASM has no DOM, no clipboard, no UI gesture surface (genuine technical impossibility).

### Added — cmp-intent-launcher iOS picker support
- **iOS picker contracts now route to native UIKit pickers.** `ResultContracts.PickImage`
  + `PickMultipleImages` → `PHPickerViewController` (iOS 14+); `PickDocument` →
  `UIDocumentPickerViewController`; `PickContact` → `CNContactPickerViewController`.
  Suspend-coroutine bridges resume exactly once from the delegate callback (success /
  cancel / error); a `DelegatePin` singleton holds strong refs while presentations are
  in flight to defend against ARC dropping the Kotlin/Native delegate shadow.
- Arbitrary actions still fall back to `onUnsupported` or `IntentResult.Failed(UnsupportedPlatform)`.
- Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md` §G-9.6 follow-up.
- **No public API changes** — caller code that already used `ResultContracts.PickImage`
  with an `onUnsupported` fallback now receives real picker results on iOS; the fallback
  is preserved for non-picker actions.

### Added — cmp-app-intents iOS Spotlight indexing
- **Searchable intents are now indexed into iOS Spotlight + Siri Suggestions.** The Swift
  bridge `CmpAppIntentBridge.bootstrap(callbackResolver:)` reads the manifest written by
  Kotlin's `AppIntents.register(config)`, pushes every `searchable: true` entry into
  `CSSearchableIndex.defaultSearchableIndex()` with title + contentDescription, and
  exposes `handleContinue(_:)` to route Spotlight taps back into the Kotlin DSL via
  `CmpAppIntentsCallback.shared.handler`.
- Activity prefix `com.mobilebytelabs.kmptoolkit.appintents.<id>` is the user-activity
  type to declare in your Info.plist `NSUserActivityTypes` array.
- Real Kotlin↔Swift callback wiring replaces the placeholder `print()` from v0.1.
  The bridge uses KVC + `NSSelectorFromString("invoke::")` so it stays framework-alias
  agnostic (works whether your Kotlin/Native framework is published as `kmptoolkit`,
  `shared`, or any custom name).
- macOS 10.13+ also benefits — `CoreSpotlight` is available there.
- Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md` §Resolved deferred items.
- Migration: existing consumers should replace `CmpAppIntentBridge.shared.loadManifest()`
  at app launch with `CmpAppIntentBridge.shared.bootstrap { CmpAppIntentsCallback.shared }`.

### Changed — build configuration
- **`org.gradle.jvmargs` bumped to `-Xmx4096M`** (was 2048M). The lower heap was OOMing
  wasmJs compilation when 3+ new sample modules with Compose were added to the workspace.
  Local developer machines and CI both benefit; no downstream changes required.
- **`compose.desktop.packaging.checkJdkVendor=false`** added to `gradle.properties` to
  permit `createDistributable` runs on Homebrew JDKs (per JetBrains/compose-multiplatform#3107).
  CI release pipelines are unaffected — they use vendor-pinned Corretto/Temurin where
  the flag is a no-op.

### Added — cmp-pdf-generator (new module)
- **New module `cmp-pdf-generator`** — cross-platform PDF generation library.
  Coordinates: `io.github.mobilebytelabs:cmp-pdf-generator` (ships at the shared
  `kmptoolkit.version`).
- **Input modes:** HTML string, Markdown (via `MarkdownPdfAdapter`), DSL (`pdf { … }`).
  Composable snapshot + image-to-PDF deferred to a future release.
- **Output destinations:** File, ByteArray, platform URI, Share, Print, Save.
- **Page config:** A3/A4/A5/B5/LETTER/LEGAL/TABLOID/STATEMENT + custom size +
  portrait/landscape + per-edge margins + optional per-page header/footer/page-numbers.
- **Branding:** injectable `PdfBranding(logo, poweredByText, theme, dateFormatter, watermark)`.
  De-branded `HtmlTemplateGenerator` base class.
- **Pre-built templates:** `InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`,
  `StatementTemplate`, `LetterTemplate`.
- **Error model:** sealed `PdfError` hierarchy + cancellation + `Flow<PdfProgressEvent>`.
- **Platforms:** Android, iOS (14+), macOS (11+), JVM, JS (browser+nodejs),
  wasmJs (HTML route only — DSL/byte route deferred pending pdf-lib wasmJs interop).
  Not targeted: tvOS, watchOS, Linux native, mingwX64, wasmWasi — `kotlinx-html`
  and `org.intellij.markdown` don't publish artifacts there.
- **Marker:** all public symbols `@ExperimentalPdfGeneratorApi` until v1.0.
- **PLAN:** `plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-pdf-generator/`
  (epic, 12 sub-plans, 266 tasks).

### Fixed — cmp-open-url
- **iOS / macOS `AppHint` parity (G6 fix)** — `openWithApp(url, AppHint.{EMAIL/PHONE/SMS/MAPS})`
  on Apple platforms now rewrites the URL to the scheme-appropriate form (`mailto:`, `tel:`,
  `sms:`, `maps:`/`geo:`) BEFORE calling `UIApplication.openURL` / `NSWorkspace.openURL`.
  Previously the hint was silently ignored — `AppHint.EMAIL + bare HTTPS` would open Safari
  instead of Mail.app. Incompatible (url, hint) combinations now return
  `OpenUrlResult.Error(message)` with a clear hint about the required URL scheme.
  - `AppHint.Custom(packageName)` is now documented as **Android-only**; on iOS / macOS /
    JVM / JS / wasmJs it silently falls back to `AppHint.DEFAULT` behaviour (no behaviour
    change vs the previous silent fallback — only KDoc clarification).
  - Pure-logic `transformUrl()` helper added in commonMain (internal); 20+ unit-test cases
    cover the rewrite matrix.
  - Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/03-open-url-g6-fix.md`
  - **No public API changes** — BCV baseline unaffected; consumer code requires no migration.

### Breaking — cmp-remote-config (next release: 4.0.0)
- **Removed dependency on `cmp-product-tickets`.** `cmp-remote-config` is now standalone.
  No transitive Maven pull, no shared config object, no cross-module imports.
- **Public API reduced to a single Koin DSL extension**: `fun Module.remoteConfig(block)`.
  Drops the previous public `val remoteConfigModule` + `object RemoteConfigConfig`.
- **`RemoteConfigConfig` (public mutable singleton with `ifEmpty { ProductTicketsConfig.x }` fallbacks) deleted.**
  Replaced by an internal `RemoteConfigSettings` data class populated via the DSL builder.
- **Dropped `productType` parameter.** Per-project Supabase model (mirrors cmp-product-tickets v3.0.0 — each
  consumer app has its own Supabase project, so the `product_type` filter column is meaningless on the client).
  Removed from: `RemoteConfigService` ctor, `getActiveConfigs()` filter, `get_device_impressions` /
  `record_config_impression` / `dismiss_config` RPC parameters (`p_product_type`), `RemoteConfig` data class
  (`@SerialName("product_type")` field), `RemoteConfigBuilder` DSL, `RemoteConfigSettings`.
  - Server follow-up: the `product_remote_config.product_type` column and `p_product_type` RPC arguments can be
    dropped in a separate migration; until then they remain ignored (`ignoreUnknownKeys = true` on the JSON deserializer).
- **`ActionType` changed from `enum class` to `@JvmInline value class`.** Built-in constants
  (NONE/URL/DEEPLINK/STORE/DISMISS) preserved as companion vals; consumer apps can extend with their own:
  `object RemoteActions { val OPEN_DOWNLOADS = ActionType("open_downloads") }`. New built-in: `PREMIUM`.
  `ActionType.from(value)` removed — construct directly: `ActionType("my_type")`.
- **New extensible action dispatcher.** Register handlers in the DSL via
  `action("type") { value, ctx -> … }`. Handlers fire when `RemoteConfigHost` is called without an
  explicit `onAction` parameter; the explicit `onAction` escape hatch is preserved.

#### Migration
```kotlin
// Before
ProductTicketsConfig.init(supabaseUrl, supabaseAnonKey, boardType = "your_app")
startKoin { modules(productTicketsModule, remoteConfigModule) }

// After (one block inside any existing Koin module)
val networkModule = module {
    remoteConfig {
        supabaseUrl = "https://YOUR_PROJECT.supabase.co"
        supabaseKey = "YOUR_ANON_KEY"
        action(ActionType.PREMIUM) { _, _ -> AppNavigator.navigateTo("paywall") }
        action("open_downloads")    { v, _ -> AppNavigator.navigateTo("downloads/${v.orEmpty()}") }
    }
    // … your other bindings …
}
startKoin { modules(networkModule, /* other modules */) }   // remoteConfigModule no longer exists
```


### Added
- **New Module: kmp-open-url** (`io.github.mobilebytelabs:kmp-open-url:3.2.1`)
  - Cross-platform URL opening for all 14 KMP targets
  - `openUrl(url)` — open with default platform handler, never throws
  - `openInBrowser(url)` — force system browser, bypasses app-association rules
  - `openWithApp(url, AppHint)` — open with preferred app category, returns `OpenUrlResult`
  - `canOpen(url)` — check URL handleability without opening
  - `AppHint` sealed class: DEFAULT, BROWSER, EMAIL, MAPS, PHONE, SMS, Custom(packageName)
  - `OpenUrlResult` sealed class: Success, NoHandler, Error(message)
  - Android: `Intent.ACTION_VIEW` + ContentProvider auto-init (zero setup)
  - iOS/macOS: `UIApplication.openURL` / `NSWorkspace.openURL` via shared `appleMain`
  - tvOS/watchOS: graceful `NoHandler` — no crash
  - JVM Desktop: `Desktop.getDesktop().browse()` + `xdg-open` headless fallback
  - JS Browser: `window.open(url, "_blank")`
  - wasmJs: `window.open` via `@JsFun` interop
  - Linux Native: `xdg-open` via `platform.posix.system`
  - Windows Native: `ShellExecuteW` via Win32 API
  - wasmWasi: deliberate no-op (no display concept)

- **New Module: kmp-clipboard** (`io.github.mobilebytelabs:kmp-clipboard:0.1.0`)
  - Extracted clipboard functionality into standalone module
  - ClipboardObserver for monitoring clipboard changes with app foreground detection
  - Platform-specific implementations: Android (ClipboardManager + ProcessLifecycleOwner), iOS (UIPasteboard), macOS (NSPasteboard), JVM (AWT FlavorListener), JS (navigator.clipboard), Linux (xclip/xsel), Windows (Win32 API)
  - All 20+ platform targets supported

- **New Module: kmp-toast** (`io.github.mobilebytelabs:kmp-toast:0.1.0`)
  - Pure Compose Multiplatform toast/snackbar notifications
  - Duration options: SHORT (2s), MEDIUM (3.5s), LONG (5s), INDEFINITE
  - Position options: TOP, CENTER, BOTTOM
  - Style options: DEFAULT, SUCCESS, ERROR, WARNING, INFO
  - Action button support and swipe-to-dismiss

- **New Module: kmp-in-app-update** (`io.github.mobilebytelabs:kmp-in-app-update:0.5.0`)
  - Extracted from cmp-library into dedicated module
  - Check for app updates with GitHub Releases, Supabase, or custom backends
  - Platform-specific implementations for Play Store, App Store, Mac App Store
  - Update types: NONE, FLEXIBLE, IMMEDIATE

- **Template Module: cmp-library** (`io.github.mobilebytelabs:kmp-template:1.0.0-template`)
  - Converted to template/reference module for creating new libraries
  - Includes TEMPLATE_README.md with step-by-step instructions
  - Sample expect/actual pattern with Greeting.kt and Platform.kt

- **Sample Apps**
  - `sample-clipboard`: Clipboard + Observer + Toast integration demo
  - `sample-in-app-update`: In-App Update demo with GitHub Releases

### Changed
- **Modular Architecture**: KMP Toolkit now uses a modular architecture
  - Each feature is a standalone library module
  - Consumers can import only the features they need
  - GitHub Actions auto-discovers and publishes all cmp-* modules

### Documentation
- Added `MODULE_STRUCTURE.md` documenting the modular architecture pattern
- Updated `README.md` with new installation instructions for individual modules
- Added `TEMPLATE_README.md` for creating new library modules

---

## kmp-in-app-update

### [0.5.0] - 2026-03-04

#### Added
- Initial release as standalone module (extracted from cmp-library)
- Cross-platform in-app update checking:
  - `AppUpdate.checkForUpdate(config)` - Check for available updates
  - `AppUpdateConfig.builder()` - Configure update sources
  - `GitHubResolver` - Check updates via GitHub Releases
  - `SupabaseResolver` - Check updates via Supabase backend

#### Platform Support
| Platform | Native Store | Custom Resolver |
|----------|:------------:|:---------------:|
| Android | ✅ Play Store | ✅ |
| iOS | ✅ App Store | ✅ |
| macOS | ✅ Mac App Store | ✅ |
| JVM | ❌ | ✅ |
| Linux | ❌ | ✅ |
| Windows | ❌ | ✅ |
| JS | ❌ | ❌ |
| Wasm | ❌ | ❌ |

---

## kmp-toast

### [0.1.0] - 2026-03-04

#### Added
- Initial release as pure Compose Multiplatform module
- Toast/Snackbar notifications:
  - `ToastHost` - Container composable for toasts
  - `rememberToastHostState()` - State management
  - `showToast()` - Display toast with customization

#### Features
- **Duration**: SHORT (2s), MEDIUM (3.5s), LONG (5s), INDEFINITE
- **Position**: TOP, CENTER, BOTTOM
- **Style**: DEFAULT, SUCCESS, ERROR, WARNING, INFO
- Action button support
- Swipe-to-dismiss gesture

#### Platform Support
Works on all Compose Multiplatform targets (Android, iOS, macOS, JVM, Web)

---

## kmp-clipboard

### [0.1.0] - 2026-03-04

#### Added
- Initial release as standalone module
- Cross-platform clipboard utilities:
  - `copyToClipboard(text: String): Boolean` - Copy text to clipboard
  - `getFromClipboard(): String?` - Read text from clipboard
  - `hasClipboardText(): Boolean` - Check if clipboard has text
  - `clearClipboard()` - Clear clipboard contents

#### Platform Support
| Platform | Copy | Read | Notes |
|----------|:----:|:----:|-------|
| Android | ✅ | ✅ | Auto-initialized via ContentProvider |
| iOS | ✅ | ✅ | Uses UIPasteboard |
| macOS | ✅ | ✅ | Uses NSPasteboard |
| JVM | ✅ | ✅ | Uses AWT Toolkit |
| Linux | ✅ | ✅ | Requires xclip or xsel |
| Windows | ✅ | ✅ | Uses Win32 API |
| JS | ✅ | ❌ | Async API, write-only for sync |
| Wasm JS | ❌ | ❌ | Complex JS interop required |
| Wasm WASI | ❌ | ❌ | No clipboard in WASI runtime |
| tvOS | ❌ | ❌ | No pasteboard support |
| watchOS | ❌ | ❌ | No pasteboard support |

---

## kmp-template (cmp-library)

### [1.0.0-template] - 2026-03-04

#### Changed
- Converted from production library to template/reference module
- All production code moved to dedicated modules:
  - Clipboard → `kmp-clipboard`
  - App Update → `kmp-in-app-update`
  - Toast → `kmp-toast`

#### Purpose
- Reference implementation for creating new KMP library modules
- Sample expect/actual pattern with `Greeting.kt` and `Platform.kt`
- Step-by-step instructions in `TEMPLATE_README.md`

### [0.5.0] - Previous

- App Update functionality (now in kmp-in-app-update)

### [0.4.0] - Previous

- Initial release with Clipboard and App Update features combined

---

## Migration Guide

### From kmp-toolkit 0.4.x/0.5.x to Modular Libraries

The monolithic `kmp-toolkit` has been split into focused, independent modules. Choose only the modules you need:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Before (all features bundled)
            // implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")

            // After (import only what you need)
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")      // Clipboard utilities
            implementation("io.github.mobilebytelabs:kmp-toast:0.1.0")          // Toast/Snackbar UI
            implementation("io.github.mobilebytelabs:kmp-in-app-update:0.5.0")  // App update checking
        }
    }
}
```

### API Changes

| Feature | Old Import | New Import |
|---------|------------|------------|
| Clipboard | `com.mobilebytelabs.kmptoolkit.*` | `com.mobilebytelabs.kmptoolkit.clipboard.*` |
| Toast | N/A (new) | `com.mobilebytelabs.kmptoolkit.toast.*` |
| App Update | `com.mobilebytelabs.kmptoolkit.appupdate.*` | `com.mobilebytelabs.kmptoolkit.appupdate.*` (unchanged) |

---

[Unreleased]: https://github.com/MobileByteLabs/KmpToolkit/compare/v0.5.0...HEAD
[kmp-in-app-update-0.5.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/in-app-update-v0.5.0
[kmp-toast-0.1.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/toast-v0.1.0
[kmp-clipboard-0.1.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/clipboard-v0.1.0
