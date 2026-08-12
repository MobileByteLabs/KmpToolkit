# cmp-firebase

Firebase Analytics for **Kotlin Multiplatform** — true 21/21 KMP target coverage. GitLive Firebase native SDK on the 11 platforms it ships; Firebase Measurement Protocol over HTTP on the remaining 10. Single Maven coordinate, single helper interface.

## Documentation

- [SETUP.md](SETUP.md) — Manual integration guide (per-platform setup + MP wiring + DI examples)
- [ANALYTICS.md](ANALYTICS.md) — **Analytics engine consumption guide** (opt-out, trackers, funnels, offline queue, network telemetry, Compose auto-tracking)
- [ANALYTICS_DEVELOPMENT.md](ANALYTICS_DEVELOPMENT.md) — **Developer guide** (architecture, opt-out contract, adding backends/trackers, BCV, release)
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup via `/sync-firebase-analytics`

## Quick Start

```kotlin
// 1. Add to gradle/libs.versions.toml
[versions]
cmp-firebase = "1.0.0"

[libraries]
cmp-firebase = { module = "io.github.mobilebytelabs:cmp-firebase", version.ref = "cmp-firebase" }

// 2. Add to commonMain in build.gradle.kts
commonMain.dependencies {
    implementation(libs.cmp.firebase.analytics)
}
```

```kotlin
// 3. Wire in your Koin module (Firebase tier — Android/iOS/macOS/tvOS/JVM/JS)
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule

val analyticsModule = module {
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
            else                   AnalyticsModule.Mode.Firebase
        )
    }
}

// 4. Use anywhere
class SettingsViewModel(private val analytics: AnalyticsHelper) {
    fun onSaveClick() {
        analytics.logButtonClick("save", screenName = "settings")
    }
}
```

## Modules

| Module | Purpose | Targets |
|---|---|:-:|
| `cmp-firebase` | Single module — interface + Stub/NoOp/Test + GitLive Firebase impl + MP HTTP impl | **21** (full KMP) |

## Platform Support — 21/21

| Tier | Targets | Count | Backend |
|---|---|:-:|---|
| **firebaseMain** | Android · JVM · iOS (×3) · macOS (×2) · tvOS (×3) · JS | 11 | GitLive Firebase Kotlin SDK (full native: DebugView, automatic events, A/B Testing, demographics, ATT) |
| **nonFirebaseMain** | watchOS (×4) · Linux (×2) · mingwX64 · wasmJs · wasmWasi | 10 | Firebase Measurement Protocol via HTTP (events land in same Firebase property + same BigQuery dataset) |

Trade-offs vs native SDK on the 10 MP-only platforms documented in [SETUP.md §Non-Firebase platforms](SETUP.md#non-firebase-platforms-watchos--linux--windows--wasm).

## Project-specific keys (zero library defaults)

The library bakes in **no Firebase keys, no MP secrets, no project IDs**. Every consuming app supplies its own:

- `google-services.json` (Android) — from your Firebase Console
- `GoogleService-Info.plist` (iOS / macOS / tvOS) — from your Firebase Console
- Firebase config object (JS) — from your Firebase Console
- MP API secret (only if targeting non-Firebase platforms) — generated at Firebase Console → GA4 → Data Streams → Measurement Protocol API secrets

Secrets stay in your app's secrets store (`release-layer/.env`, encrypted prefs, keychain) — **never committed**.

## Sync command

For framework users (claude-product-cycle):

```bash
/sync-firebase-analytics           # Verify-gated sync into the active project
/sync-firebase-analytics --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for the full Gate breakdown.
