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

## Platform Support — 15/15

| Tier | Targets | Count | Backend |
|---|---|:-:|---|
| **firebaseMain** | Android · iOS (×3) · macOS (×2) · tvOS (×3) · JS · **wasmJs** | 11 | GitLive Firebase Kotlin SDK (full native: DebugView, automatic events, A/B Testing, demographics, ATT) |
| **nonFirebaseMain** | JVM · Linux (×2) · mingwX64 | 4 | Firebase Measurement Protocol via HTTP (events land in same Firebase property + same BigQuery dataset) |

> **Changed in GitLive 3.0.0-alpha02 (2026-09-03):** `wasmJs` moved from the MP tier to the native
> Firebase tier and now reads `FirebaseConfig.web` — see [SETUP.md](SETUP.md) for the migration note.
> The same revision corrects two long-standing errors in this table: **JVM** was listed under
> `firebaseMain` although the build file puts it on the MP tier (GitLive's JVM analytics is a stub),
> and **watchOS / wasmWasi** were listed as shipping targets although `cmp-firebase` does not
> declare them at all — which is also why the total is **15**, not the 21 this heading claimed.
>
> Target math: firebaseMain 11 (android · ios×3 · macos×2 · tvos×3 · js · wasmJs) +
> nonFirebaseMain 4 (jvm · linux×2 · mingw) = **15**, matching
> [`cmp-firebase/DEVELOPMENT.md`](../../cmp-firebase/DEVELOPMENT.md).

Trade-offs vs the native SDK on the 4 MP-only platforms are documented in [SETUP.md §7 Non-Firebase platforms](SETUP.md#7-non-firebase-platforms-jvm--linux--mingw).

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
