# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- **New Module: kmp-clipboard** (`io.github.mobilebytelabs:kmp-clipboard:0.1.0`)
  - Extracted clipboard functionality into standalone module
  - Independent Maven Central publishing
  - Same API as before: `copyToClipboard()`, `getFromClipboard()`, `hasClipboardText()`, `clearClipboard()`
  - All 20+ platform targets supported

### Changed
- **Modular Architecture**: KMP Toolkit now uses a modular architecture
  - Each feature is a standalone library module
  - Consumers can import only the features they need
  - Pattern established for future feature modules

### Documentation
- Added `MODULE_STRUCTURE.md` documenting the modular architecture pattern
- Updated `README.md` with new installation instructions for individual modules

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

## kmp-toolkit

### [0.5.0] - 2026-03-04

#### Changed
- Clipboard functionality moved to separate `kmp-clipboard` module
- Now focuses on App Update functionality

#### Migration
If you were using clipboard features from `kmp-toolkit`, add the new dependency:

```kotlin
// Before (clipboard was included)
implementation("io.github.mobilebytelabs:kmp-toolkit:0.4.0")

// After (add clipboard module separately)
implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")
implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")
```

### [0.4.0] - Previous

- Initial release with Clipboard and App Update features combined

---

## Migration Guide

### From 0.4.x to 0.5.x

1. **If you use clipboard features**: Add the new `kmp-clipboard` dependency
2. **If you only use app-update**: No changes required
3. **API unchanged**: All function signatures remain the same

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")  // Add if using clipboard
        }
    }
}
```

---

[Unreleased]: https://github.com/MobileByteLabs/KmpToolkit/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/MobileByteLabs/KmpToolkit/compare/v0.4.0...v0.5.0
[0.1.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/clipboard-v0.1.0
