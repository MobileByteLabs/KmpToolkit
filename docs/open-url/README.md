# cmp-open-url

Cross-platform URL opening for Kotlin Multiplatform — browser, email, maps, phone, SMS, and custom URI schemes across all 14 KMP targets.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-open-url)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-open-url)

---

## Installation

```kotlin
// build.gradle.kts
implementation("io.github.mobilebytelabs:kmp-open-url:<version>")
```

No additional setup required on Android — the library auto-initialises via a `ContentProvider` that is merged into your manifest automatically.

---

## Quick Start

```kotlin
import com.mobilebytelabs.kmptoolkit.openurl.*

// Open any URL with the default system handler
openUrl("https://github.com/MobileByteLabs")      // → browser

// Force browser (bypass app-association rules)
openInBrowser("https://accounts.google.com/...")  // → always browser

// Open with a specific app category
openWithApp("mailto:hello@example.com", AppHint.EMAIL)   // → Gmail / Apple Mail
openWithApp("geo:37.7749,-122.4194", AppHint.MAPS)       // → Google Maps / Apple Maps
openWithApp("tel:+1-800-555-0100", AppHint.PHONE)        // → phone dialer
openWithApp("sms:+1-800-555-0100", AppHint.SMS)          // → messaging app

// Android-only: explicit package name
openWithApp("https://maps.google.com", AppHint.Custom("com.google.android.apps.maps"))

// Check before opening
if (canOpen("myapp://deep-link/home")) {
    openUrl("myapp://deep-link/home")
}
```

---

## API Reference

### `openUrl(url: String): Boolean`

Opens the URL using the platform's default handler. Returns `true` if accepted, `false` otherwise. **Never throws.**

### `openInBrowser(url: String): Boolean`

Forces the URL to open in the system web browser, bypassing app-association rules. **Never throws.**

### `openWithApp(url: String, appHint: AppHint): OpenUrlResult`

Opens the URL with a preferred app category hint. Returns an `OpenUrlResult` for richer error handling. **Never throws.**

### `canOpen(url: String): Boolean`

Returns `true` if the platform can handle the URL without actually opening it. **Never throws.**

---

## AppHint

| Value | Effect |
|-------|--------|
| `AppHint.DEFAULT` | Let the OS choose the best handler |
| `AppHint.BROWSER` | Force system browser |
| `AppHint.EMAIL` | Prefer email client (`mailto:`) |
| `AppHint.MAPS` | Prefer maps app (`geo://`, `maps:`) |
| `AppHint.PHONE` | Prefer phone dialer (`tel:`) |
| `AppHint.SMS` | Prefer SMS app (`sms:`) |
| `AppHint.Custom(pkg)` | Android only — explicit package name |

---

## OpenUrlResult

```kotlin
sealed class OpenUrlResult {
    object Success                      // URL was opened
    object NoHandler                    // No app can handle it
    data class Error(val message: String) // Unexpected error
}

// Exhaustive matching
when (result) {
    OpenUrlResult.Success    -> showConfirmation()
    OpenUrlResult.NoHandler  -> showFallbackUI()
    is OpenUrlResult.Error   -> log(result.message)
}
```

---

## Platform Support

| Platform | `openUrl` | `openInBrowser` | `openWithApp` | `canOpen` |
|----------|:---------:|:---------------:|:-------------:|:---------:|
| Android | ✅ | ✅ | ✅ (all hints) | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ |
| macOS | ✅ | ✅ | ✅ | ✅ |
| tvOS | ❌ NoHandler | ❌ NoHandler | ❌ NoHandler | ❌ false |
| watchOS | ❌ NoHandler | ❌ NoHandler | ❌ NoHandler | ❌ false |
| JVM Desktop | ✅ | ✅ | ✅ | ✅ |
| JS Browser | ✅ | ✅ | ✅ | ✅ |
| JS Node | ✅ (via `open`) | ✅ | ✅ | ⚠️ |
| Linux Native | ✅ (xdg-open) | ✅ | ✅ | ✅ |
| Windows Native | ✅ (ShellExecuteW) | ✅ | ✅ | ✅ |
| wasmJs | ✅ | ✅ | ✅ | ✅ |
| wasmWasi | ❌ false | ❌ false | ❌ NoHandler | ❌ false |

---

## Important Notes

- **Never throws** — all functions catch exceptions internally and return `false` / `NoHandler`.
- **No URL encoding** — the library passes URLs as-is; encoding is the caller's responsibility.
- **Outgoing only** — this library does not handle incoming deep links into your app.
- **`AppHint.Custom` fallback** — if the specified Android package is not installed, the library retries with `AppHint.DEFAULT` before returning `NoHandler`.
