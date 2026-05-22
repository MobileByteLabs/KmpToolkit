# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added — cmp-pdf-generator (new module, v0.1.0)
- **New module `cmp-pdf-generator`** — cross-platform PDF generation library.
  Coordinates: `io.github.mobilebytelabs:kmp-pdf-generator`.
- **Input modes (v0.1):** HTML string, Markdown (via `MarkdownPdfAdapter`), DSL (`pdf { … }`).
  Composable snapshot + image-to-PDF deferred to v0.2.
- **Output destinations (v0.1):** File, ByteArray, platform URI, Share, Print, Save.
- **Page config:** A3/A4/A5/B5/LETTER/LEGAL/TABLOID/STATEMENT + custom size + portrait/landscape + per-edge margins + optional per-page header/footer/page-numbers.
- **Branding:** injectable `PdfBranding(logo, poweredByText, theme, dateFormatter, watermark)`.
  De-branded `HtmlTemplateGenerator` base class.
- **Pre-built templates:** `InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`, `StatementTemplate`, `LetterTemplate`.
- **Error model:** sealed `PdfError` hierarchy + cancellation + `Flow<PdfProgressEvent>` progress.
- **Platform tiers:**
  - Tier-1 (functional): Android, iOS (14+), macOS (11+), JVM, JS, wasmJs
  - Not targeted (per Kotlin Multiplatform target tiers + upstream library coverage):
    tvOS, watchOS, Linux native, mingwX64, wasmWasi — kotlinx-html and
    org.intellij.markdown don't publish artifacts for those.
- **Marker:** all public symbols `@ExperimentalPdfGeneratorApi` until v1.0.
- **PLAN:** `plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-pdf-generator/` (epic, 12 sub-plans, 266 tasks).
- **NOTE:** initial commit lays down the full source structure across 22 source sets.
  Production smoke tests on each platform are pending. v0.1.0 release is gated on green CI.

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
