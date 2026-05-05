# cmp-firebase-analytics

Firebase Analytics for **Kotlin Multiplatform** — single-module design. Interface + Stub/NoOp/Test variants on all 21 KMP targets, with [GitLive Firebase Analytics](https://github.com/GitLiveApp/firebase-kotlin-sdk) on **11 of 21 targets** (everything Apple/Android/JVM/JS — only watchOS, Linux, Windows, and wasm fall back to NoOp).

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-firebase-analytics)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase-analytics)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What's in the box

```
io.github.mobilebytelabs.kmptoolkit.analytics
├── AnalyticsHelper            — interface (logEvent, logScreenView, logError, ...)
├── AnalyticsEvent / Param     — type-safe event + param data classes (Firebase-aligned validation)
├── EventTypes / ParamKeys     — standard constants for cross-app consistency
├── StubAnalyticsHelper        — dev/Kermit logger
├── NoOpAnalyticsHelper        — release/test default; events silently discarded
├── TestAnalyticsHelper        — captures events for assertion in unit tests
├── EventValidator             — taxonomy regex + PII regex check (debug-build use)
├── PerformanceTracker         — start/stop timer that emits loading_time events
├── EventRegistry              — opt-in declared-event enforcement
├── AnalyticsExtensions        — `analytics.log("event") { param(...) }` DSL
├── AnalyticsProvider.kt       — `expect fun provideAnalyticsHelper(): AnalyticsHelper`
├── di/AnalyticsModule         — factory: Mode.Firebase | Mode.Stub | Mode.NoOp
└── firebase/
    └── FirebaseAnalyticsHelper  — GitLive-backed concrete impl (firebaseMain only)
```

## Targets — true 21/21 KMP coverage via two transport tiers

| Tier | Targets | Count | Recommended helper | Default `provideAnalyticsHelper()` |
|---|---|:-:|---|---|
| **firebaseMain** | Android, JVM, iOS (iosX64/iosArm64/iosSimulatorArm64), macOS (macosX64/macosArm64), tvOS (tvosX64/tvosArm64/tvosSimulatorArm64), JS | **11** | `FirebaseAnalyticsHelper` (GitLive — full native: DebugView, automatic events, A/B Testing, demographics) | `FirebaseAnalyticsHelper(Firebase.analytics)` |
| **nonFirebaseMain** | watchOS (×4), Linux (×2), mingwX64, wasmJs, wasmWasi | **10** | `MeasurementProtocolAnalyticsHelper` (HTTP POST to Firebase MP — events land in the SAME Firebase Analytics property + same BigQuery export) | `NoOpAnalyticsHelper` (until app wires MP — see below) |

GitLive Firebase Analytics 2.x ships on macOS and tvOS — they're real Apple targets that Firebase iOS SDK supports natively. Only watchOS sits outside on the Apple side (Firebase iOS SDK has no watchOS variant).

For the 10 non-Firebase platforms, `MeasurementProtocolAnalyticsHelper` provides event capture parity (custom events, user properties, persistent client_id). It uses Firebase's Measurement Protocol REST API — events land in the SAME property and BigQuery dataset as GitLive-emitted events. Trade-offs vs native SDK: no DebugView, no automatic events, no A/B tie-in, ~1h latency to BigQuery (same as GitLive).

`provideAnalyticsHelper()` defaults to NoOp on nonFirebase platforms because MP requires app-supplied config (`measurement_id` + `api_secret`). Apps that want analytics on watchOS / Linux / etc. wire `MeasurementProtocolAnalyticsHelper` directly in their Koin module — see "Setup → Non-Firebase platforms" below.

## Install

```kotlin
// gradle/libs.versions.toml
[versions]
cmpFirebaseAnalytics = "..."

[libraries]
cmp-firebase-analytics = { module = "io.github.mobilebytelabs:cmp-firebase-analytics", version.ref = "cmpFirebaseAnalytics" }
```

```kotlin
// build.gradle.kts (consumer module)
commonMain.dependencies {
    implementation(libs.cmp.firebase.analytics)
}
```

GitLive Firebase Analytics is brought in transitively as `api` on supported platforms. On non-supported platforms the dependency simply doesn't apply (Gradle source-set hierarchy handles it).

## Setup

### Android

1. Add `google-services.json` to `app/`
2. Apply the plugin: `id("com.google.gms.google-services") version "..."`
3. Add Firebase BoM and Analytics:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:..."))
   implementation("com.google.firebase:firebase-analytics")
   ```

### iOS / macOS / tvOS

1. Add `GoogleService-Info.plist` to your app target
2. In your AppDelegate / `@main` App:
   ```swift
   import FirebaseCore
   FirebaseApp.configure()
   ```
3. Pod dependencies are bundled by GitLive — see your project's Podfile

### JS / JVM

Follow GitLive's docs: https://github.com/GitLiveApp/firebase-kotlin-sdk

### watchOS / Linux / Windows / wasm — Non-Firebase platforms

GitLive doesn't ship on these 10 targets, so use **Firebase Measurement Protocol over HTTP** for event capture parity.

1. **Generate an MP API secret** at Firebase Console → Project settings → Integrations → GA4 → Data Streams → {your stream} → **Measurement Protocol API secrets** → Create.

2. **Store the secret in your app's secrets store** — env var, encrypted prefs, keychain, or `release-layer/.env` (gitignored). NEVER hard-code or commit.

3. **Wire `MeasurementProtocolAnalyticsHelper` in your Koin module**:

   ```kotlin
   import com.russhwolf.settings.Settings
   import io.github.mobilebytelabs.kmptoolkit.analytics.AnalyticsHelper
   import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MeasurementProtocolAnalyticsHelper
   import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MpConfig

   val analyticsModule = module {
       single<AnalyticsHelper> {
           MeasurementProtocolAnalyticsHelper(
               config = MpConfig(
                   measurementId = "G-XXXXXXXX",                       // GA4 measurement ID
                   apiSecret     = SecureStore.read("MP_API_SECRET"),  // your secrets store
               ),
               settings = Settings(),                                  // multiplatform-settings
           )
       }
   }
   ```

4. Events from MP land in the SAME `analytics_*.events_*` BigQuery table as GitLive-emitted events. `/idea analytics --fetch` works identically across all 21 platforms.

**What you give up vs native SDK on these platforms:**
- No automatic events (`first_open`, `session_start`, `in_app_purchase`) — emit manually if needed
- No DebugView (events visible only in BigQuery, ~1h latency)
- No A/B Testing tie-in
- No demographics inference
- No platform-native session tracking — supply `engagement_time_msec` param manually if you need engagement metrics

**What still works:**
- Custom event capture with up to 25 params per event
- User properties + user ID
- Persistent client_id on platforms with KV storage (Apple, JS) — in-memory on Linux native / mingwX64 / wasmWasi
- Async batching (5s debounce or 25 events, whichever first)
- Silent failure on network errors (analytics never breaks the app)

## Usage

```kotlin
import io.github.mobilebytelabs.kmptoolkit.analytics.*
import io.github.mobilebytelabs.kmptoolkit.analytics.di.AnalyticsModule

// Easiest path — let the module pick per build:
val analyticsModule = module {
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
            else                   AnalyticsModule.Mode.Firebase
        )
    }
    single { AnalyticsModule.performanceTracker(get()) }
}
```

Then in your ViewModel:

```kotlin
class SettingsViewModel(private val analytics: AnalyticsHelper) : ViewModel() {
    init {
        analytics.logScreenView("settings", sourceScreen = "home")
    }

    fun onSaveClick() {
        analytics.logButtonClick("save", screenName = "settings")
        // ... save logic
    }
}
```

### Direct logging

```kotlin
analytics.logEvent(EventTypes.BUTTON_CLICK,
    ParamKeys.BUTTON_NAME to "save",
    ParamKeys.SCREEN_NAME to "settings",
)

// Convenience helpers
analytics.logScreenView("settings", sourceScreen = "home")
analytics.logError("Network timeout", errorCode = "NET_001", screen = "settings")
analytics.logStateTransition("settings", from = "loading", to = "content")

// Builder DSL
analytics.log(EventTypes.FORM_COMPLETED) {
    param(ParamKeys.FORM_NAME, "registration")
    param(ParamKeys.COMPLETION_TIME, 45)
}

// Performance timing
val tracker = PerformanceTracker(analytics)
tracker.measure("settings_screen_render") { /* render work */ }
```

### Direct Firebase access

If you need the underlying GitLive `FirebaseAnalytics` (e.g., custom user properties beyond the helper API):

```kotlin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import io.github.mobilebytelabs.kmptoolkit.analytics.firebase.FirebaseAnalyticsHelper

val helper = FirebaseAnalyticsHelper(Firebase.analytics)
```

`FirebaseAnalyticsHelper` is only available on firebaseMain (Android/iOS/JS/JVM). Cross-platform code should call `provideAnalyticsHelper()` instead — it returns the same helper on supported platforms and `NoOpAnalyticsHelper` elsewhere.

## Auto-injected `kmp_platform` param

Every event gets a `kmp_platform` param injected by the helper — disambiguates events in BigQuery / Firebase Console by platform-of-origin:

| Source target | `kmp_platform` value |
|---|---|
| `androidMain` | `"android"` |
| `iosMain` (×3) | `"ios"` |
| `macosMain` (×2) | `"macos"` |
| `tvosMain` (×3) | `"tvos"` |
| `watchosMain` (×4) | `"watchos"` |
| `jvmMain` | `"jvm"` |
| `jsMain` | `"js"` |
| `linuxMain` (×2) | `"linux"` |
| `mingwMain` | `"mingw"` |
| `wasmJsMain` | `"wasmjs"` |
| `wasmWasiMain` | `"wasmwasi"` |

Why a custom key, not GA4's built-in `platform`:
- GA4's auto-`platform` is coarse: only `"android" | "ios" | "web"`
- MP HTTP events don't get auto-`platform` unless we set it
- Sub-platforms (watchOS vs iOS, tvOS vs macOS — all "Apple") collapse to `"ios"` in GA4's field
- We need single signal that's reliable across native and MP transports

**Override per-helper** for finer-grained signal:

```kotlin
FirebaseAnalyticsHelper(Firebase.analytics, platformOverride = "android-tv")
StubAnalyticsHelper(platformOverride = "ios-tablet")
```

**Override per-event** by setting `kmp_platform` manually — auto-injection respects existing values:

```kotlin
analytics.logEvent(EventTypes.BUTTON_CLICK,
    ParamKeys.PLATFORM to "android-tablet",   // takes precedence over kmpPlatform
    ParamKeys.BUTTON_NAME to "save",
)
```

`TestAnalyticsHelper` does NOT auto-inject — keeps test assertions explicit.

## Firebase Analytics constraints

The adapter automatically truncates to Firebase's limits:

| Field | Max | What happens |
|---|---|---|
| Event name | 40 chars | truncated by `.take(40)` |
| Param key | 40 chars | truncated by `.take(40)` |
| Param value | 100 chars | truncated by `.take(100)` |
| User property name | 24 chars | truncated by `.take(24)` |
| User property value | 36 chars | truncated by `.take(36)` |
| User ID | 256 chars | truncated by `.take(256)` |
| Params per event | 25 | enforced upstream by `AnalyticsEvent.init` (throws on > 25) |

**Best practice**: design your event taxonomy to fit naturally. The bundled `EventValidator` enforces a stricter regex (`^[a-z][a-z0-9_]{1,39}$`) which keeps you in spec.

## Testing

```kotlin
@Test fun `clicking save logs button_click event`() {
    val analytics = TestAnalyticsHelper()
    val viewModel = SettingsViewModel(analytics)

    viewModel.onSaveClick()

    val event = analytics.events.single()
    assertEquals(EventTypes.BUTTON_CLICK, event.type)
    assertEquals("save", event.extras.first { it.key == ParamKeys.BUTTON_NAME }.value)
}
```

## Privacy

- Use the `pii: true` flag on params in your screen YAML (per framework `/idea analytics` schema) to mark sensitive fields — these are NEVER auto-instrumented
- Hash/obfuscate `user_id` before passing to `setUserId()` — never use raw email/phone
- Respect platform settings: iOS App Tracking Transparency (ATT), Android Limited Ad Tracking
- Provide an opt-out toggle in app settings; bind it to swap to `NoOpAnalyticsHelper` at runtime

## Project consumer pattern

```
my-project/source/my-project/
├── core/
│   └── analytics/                        ← thin glue layer per project
│       ├── build.gradle.kts              ← depends on cmp-firebase-analytics
│       └── di/AnalyticsModule.kt         ← Koin module: pick mode per build flavor
└── feature/settings/
    └── SettingsViewModel.kt              ← depends only on AnalyticsHelper interface
```

Heavy lifting is here. Per-project `core/analytics` is just Koin wiring + project-specific event taxonomy.

## Related

- Framework `/idea analytics` — auto-instrumentation generator + Claude-driven growth analysis
- GitLive Firebase Kotlin SDK — https://github.com/GitLiveApp/firebase-kotlin-sdk
- Plan: `plan-layer/plans/PLAN-fw-260504-idea-analytics.md` (in claude-product-cycle framework)

## License

Apache 2.0
