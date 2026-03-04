# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
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
