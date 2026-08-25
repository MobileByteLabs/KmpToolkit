# cmp-firebase

Firebase for **Kotlin Multiplatform** — **Analytics + Crashlytics** in one module with a single in-library setup surface ([`FirebaseKit`](#setup-stays-in-the-library)). Interface + Stub/NoOp/Test variants across all 15 supported KMP targets, backed by [GitLive Firebase](https://github.com/GitLiveApp/firebase-kotlin-sdk):

- **Analytics** — GitLive on **10 targets** (firebaseMain: Android, iOS×3, macOS×2, tvOS×3, JS), Measurement-Protocol HTTP fallback on the remaining 5 (JVM, Linux×2, mingwX64, wasmJs).
- **Crashlytics** — GitLive on **6 targets** (Android + iOS×3 + macOS×2); a structured, **AI-feedable `CrashReport`** (logged via Kermit) on the other 9. Every tier produces the same `CrashReport` JSON you can hand straight to an AI to diagnose and fix a crash.

> Renamed from `cmp-firebase-analytics` (the module now covers Crashlytics too). Package root: `io.github.mobilebytelabs.kmptoolkit.firebase`.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-firebase)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What's in the box

```
io.github.mobilebytelabs.kmptoolkit.firebase
├── FirebaseKit                 — single init surface: initialize() + crashReporter
├── analytics/
│   ├── AnalyticsHelper          — interface (logEvent, logScreenView, logError, ...)
│   ├── AnalyticsEvent / Param   — type-safe event + param data classes (Firebase-aligned validation)
│   ├── EventTypes / ParamKeys   — standard constants for cross-app consistency
│   ├── Stub/NoOp/Test helpers   — dev logger / silent default / unit-test capture
│   ├── EventValidator           — taxonomy regex + PII regex check (debug-build use)
│   ├── PerformanceTracker       — start/stop timer that emits loading_time events
│   ├── AnalyticsProvider.kt     — `expect fun provideAnalyticsHelper(): AnalyticsHelper`
│   ├── di/AnalyticsModule       — factory: Mode.Firebase | Mode.Stub | Mode.NoOp
│   ├── FirebaseAnalyticsHelper  — GitLive-backed concrete impl (firebaseMain only)
│   └── mp/                       — MeasurementProtocolAnalyticsHelper (HTTP fallback tier)
└── crashlytics/
    ├── CrashReporter            — interface (recordException, log, setCustomKey, setUserId, install)
    ├── CrashReport              — @Serializable AI-feedable model: class, message, cause chain, file:line frames → toJson()
    ├── CrashReportFactory       — Throwable.toCrashReport(...) (common; no expect/actual)
    ├── FirebaseCrashReporter    — GitLive-backed (crashlyticsFirebaseMain: android + apple)
    ├── LoggingCrashReporter     — structured JSON-to-Kermit fallback (crashlyticsFallbackMain)
    ├── NoOpCrashReporter        — silent default for tests/previews
    ├── di/CrashReporterModule   — factory: Mode.Firebase | Mode.Logging | Mode.NoOp
    └── CrashReporter extensions — asCoroutineExceptionHandler(), recording { }
```

## Targets — true 15/15 KMP coverage via two transport tiers

| Tier | Targets | Count | Recommended helper | Default `provideAnalyticsHelper()` |
|---|---|:-:|---|---|
| **firebaseMain** | Android, iOS (iosX64/iosArm64/iosSimulatorArm64), macOS (macosX64/macosArm64), tvOS (tvosX64/tvosArm64/tvosSimulatorArm64), JS | **10** | `FirebaseAnalyticsHelper` (GitLive — full native: DebugView, automatic events, A/B Testing, demographics) | `FirebaseAnalyticsHelper(Firebase.analytics)` |
| **nonFirebaseMain** | JVM, Linux (linuxX64/linuxArm64), mingwX64, wasmJs | **5** | `MeasurementProtocolAnalyticsHelper` (HTTP POST to Firebase MP — events land in the SAME Firebase Analytics property + same BigQuery export) | `NoOpAnalyticsHelper` (until app wires MP — see below) |

GitLive Firebase Analytics ships on Android, iOS, macOS, tvOS, and JS — the platforms the native Firebase SDK supports. JVM/desktop is intentionally on the nonFirebase tier: GitLive does not publish a JVM artifact.

For the 5 non-Firebase platforms (JVM, Linux×2, mingwX64, wasmJs), `MeasurementProtocolAnalyticsHelper` provides event capture parity (custom events, user properties, persistent client_id). It uses Firebase's Measurement Protocol REST API — events land in the SAME property and BigQuery dataset as GitLive-emitted events. Trade-offs vs native SDK: no DebugView, no automatic events, no A/B tie-in, ~1h latency to BigQuery (same as GitLive).

`provideAnalyticsHelper()` defaults to NoOp on nonFirebase platforms because MP requires app-supplied config (`measurement_id` + `api_secret`). Apps that want analytics on JVM / Linux / etc. wire `MeasurementProtocolAnalyticsHelper` directly in their Koin module — see "Setup → Non-Firebase platforms" below. `provideAnalyticsHelper()` is memoized process-wide, so the app's DI and the internal crash→GA4 bridge share ONE stable helper instance.

## Crashlytics — with AI-feedable crash reports

Crash reporting mirrors the analytics two-tier design, but with a **different, smaller** GitLive matrix:

| Tier | Targets | Count | Reporter |
|---|---|:-:|---|
| **crashlyticsFirebaseMain** | Android, iOS (×3), macOS (×2) | **6** | `FirebaseCrashReporter` — native Firebase Crashlytics (auto-captures uncaught crashes) **and** builds a structured `CrashReport` |
| **crashlyticsFallbackMain** | JVM, JS, tvOS (×3), Linux (×2), mingwX64, wasmJs | **9** | `LoggingCrashReporter` — no Crashlytics ingestion REST API exists, so it builds the same `CrashReport` and logs it as JSON via Kermit |

> GitLive Crashlytics 3.0.0 ships on Android + iOS + macOS only — **not** tvOS/JVM/JS/Linux/mingw/wasm (verified against its published artifacts). Analytics reaches more targets than Crashlytics, hence the separate split.

The whole point of `CrashReport` is **AI-feedability** — on *every* platform you get the exception class, the human message, the **full cause chain**, and stack frames broken out to **`file:line`**, serializable to JSON:

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit

try {
    riskyWork()
} catch (t: Throwable) {
    // fatal=false (default) = non-fatal; fatal=true = counts as crash in GA4/Crashlytics
    FirebaseKit.crashReporter.recordException(t, fatal = false)
    // Hand this straight to Claude: "explain and fix this crash"
    val json = FirebaseKit.crashReporter.lastReport?.toJson(pretty = true)
}

// Block-scoped capture with auto-record:
FirebaseKit.crashReporter.recording(fatal = true) { riskyWork() }

// Wire at app/top-level scope — pass fatal=true so GA4 `fatal` dimension is accurate:
val scope = CoroutineScope(SupervisorJob() + FirebaseKit.crashReporter.asCoroutineExceptionHandler(fatal = true))

// Opt-in global uncaught handler — returns true on JVM/Android, false (no-op) on native/js/wasm:
val installed: Boolean = FirebaseKit.installUncaughtHandler()
```

## Crash→GA4 single all-platform view

Every `recordException(...)` call also emits an `app_crash` GA4 event with three params:

| Param | Values |
|---|---|
| `kmp_platform` | auto-injected (android / ios / macos / tvos / js / jvm / linux / mingw / wasmjs) |
| `exception_type` | fully-qualified exception class name |
| `fatal` | `true` / `false` |

This means ALL platforms land in one GA4 / BigQuery table and can be segmented by `kmp_platform` and severity — whether the crash was captured by native Firebase Crashlytics or the `LoggingCrashReporter` fallback.

> **nonFirebase tier caveat**: on JVM, Linux, mingwX64, and wasmJs the `app_crash` event reaches GA4 only when an `MpConfig` is configured. Without it the analytics sink is `NoOpAnalyticsHelper` and the event is silently dropped. Configure `MpConfig` (see "Non-Firebase platforms" below) to get full cross-platform crash visibility.

## Setup stays in the library

`FirebaseKit.initialize()` enables crash reporting and wires the platform `crashReporter` in one idempotent call:

- **Android** — nothing to call. A `ContentProvider` runs `initialize()` at process start (Firebase auto-reads `google-services.json`). Zero app code.
- **iOS / macOS / tvOS** — call `FirebaseKit.initialize()` once from your entry point.
- **JVM / JS / Linux / Windows / wasmJs** — `initialize()` activates the structured logging reporter.

`FirebaseKit.installUncaughtHandler()` is an opt-in global uncaught-exception capture. Returns `true` on JVM/Android (chains into any existing `Thread.UncaughtExceptionHandler` and records the crash as `fatal = true`); returns `false` and is a no-op on all other targets — Apple native Crashlytics owns the global handler on iOS/macOS; no safe global hook exists on JS/wasm/Linux. Call after `initialize()`.

The only residual app-side steps are the ones Firebase itself requires (they identify *your* project): the config file (`google-services.json` / `GoogleService-Info.plist`), the Android `com.google.gms.google-services` plugin line, and — on Apple, per GitLive's documented path — the one Swift line `FirebaseApp.configure()` in your `@main init`.

## Install

```kotlin
// gradle/libs.versions.toml
[versions]
cmpFirebase = "..."

[libraries]
cmp-firebase = { module = "io.github.mobilebytelabs:cmp-firebase", version.ref = "cmpFirebase" }
```

```kotlin
// build.gradle.kts (consumer module)
commonMain.dependencies {
    implementation(libs.cmp.firebase)
}
```

GitLive Firebase Analytics is brought in transitively as `api` on supported platforms. On non-supported platforms the dependency simply doesn't apply (Gradle source-set hierarchy handles it).

## Setup

### One commonMain init (no native config files)

The entire Firebase setup for **every platform** can be a single `commonMain`
call — no `google-services.json`, no `GoogleService-Info.plist`, no Swift
`FirebaseApp.configure()` line. Pass one `FirebaseConfig` holding each platform's
keys; the library selects the running platform, initializes Firebase
programmatically on the GitLive-native tier (Android/iOS/macOS/tvOS/JS) and wires
the Measurement-Protocol transport on the fallback tier (JVM/Linux/Windows/wasm):

```kotlin
// commonMain — runs on every target
FirebaseKit.initialize(
    FirebaseConfig.builder()
        .android(FirebaseOptions(applicationId = "1:123:android:abc", apiKey = "AIza…", projectId = "my-proj"))
        .apple(FirebaseOptions(applicationId = "1:123:ios:def", apiKey = "AIza…", gcmSenderId = "123")) // ios/macos/tvos
        .web(FirebaseOptions(applicationId = "1:123:web:ghi", apiKey = "AIza…", authDomain = "my-proj.firebaseapp.com"))
        .measurementProtocol(MpConfig("G-XXXX", apiSecret = secureStore.read("MP_API_SECRET")))
        .build(),
)
```

Per-platform notes: Apple's native `FIROptions` requires `gcmSenderId`; a platform
with no options degrades to a NoOp analytics helper with a WARN (it never throws).
Firebase `apiKey`/`applicationId`/`projectId` are client identifiers (safe in
source); the Measurement-Protocol `apiSecret` is the only secret — load it from
your secrets store, never hard-code it. On Android the required `Context` is
captured internally by `FirebaseInitProvider`, so the call stays 100% commonMain.

The native-config-file paths below still work (the no-arg `FirebaseKit.initialize()`
keeps the legacy Android auto-init behavior) — use whichever fits your app.

### Keys & Secrets

Each consuming app supplies **its own** Firebase identity — these are not shared. Full walkthrough in `docs/firebase/SETUP.md`.

| Platform | What to supply |
|---|---|
| Android | `google-services.json` (app directory) + `com.google.gms.google-services` Gradle plugin |
| Apple (iOS/macOS/tvOS) | `GoogleService-Info.plist` (app target) + `FirebaseApp.configure()` in `@main init` |
| JS / Web | `FirebaseOptions(apiKey=…, authDomain=…, …)` passed to `FirebaseConfig.builder().web(…)` |
| JVM/Linux/mingwX64/wasmJs | `MpConfig(measurementId = "G-XXXX", apiSecret = <MP API secret>)` |

The **Measurement Protocol `apiSecret`** is the only value that is a real secret. Generate it at: Firebase Console → Project settings → Integrations → GA4 → Data Streams → {your stream} → Measurement Protocol API secrets → Create. **Never hard-code it** — load from a secrets store (`/secrets pull` in the framework, or your platform's secure store). All other Firebase values (`apiKey`, `applicationId`, `projectId`) are client identifiers and safe in source.

### Android

1. Add `google-services.json` to `app/`
2. Apply the plugin: `id("com.google.gms.google-services") version "..."`
3. Add Firebase BoM and Analytics:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:..."))
   implementation("com.google.firebase:firebase-analytics")
   ```

### iOS / macOS / tvOS (SwiftPM — GitLive 3.x)

GitLive 3.0.0 links the native Firebase iOS SDK via **SwiftPM** (not CocoaPods). `firebase-ios-sdk` flows across the Maven boundary automatically — **do not re-declare it**. In your app's shared KMP module:

1. Build the shared framework **static** — Firebase's SwiftPM products are static libraries; a dynamic framework crashes at runtime:
   ```kotlin
   // shared build.gradle.kts
   iosArm64().binaries.framework { baseName = "Shared"; isStatic = true }
   ```
2. In Xcode, use **direct integration** — add the `embedAndSignAppleFrameworkForXcode` run-script build phase (replaces `pod install`). On each build Gradle resolves the inherited `firebase-ios-sdk`, generates the synthetic Swift package, and embeds/signs the framework.
3. Set the deployment target to **iOS 15.0** (macOS 10.15, tvOS 15.0) — `firebase-ios-sdk` 12.x minimum.
4. Add `GoogleService-Info.plist` to your app target and call `FirebaseApp.configure()` once in your `@main` `init` (Firebase-mandated — it identifies *your* project):
   ```swift
   import FirebaseCore
   FirebaseApp.configure()
   ```

### JS

Follow GitLive's docs: https://github.com/GitLiveApp/firebase-kotlin-sdk

> **JVM note**: JVM is on the **nonFirebase** tier — GitLive does not publish a JVM artifact. JVM consumers receive `MeasurementProtocolAnalyticsHelper` when `MpConfig` is supplied, else `NoOpAnalyticsHelper`. See "Non-Firebase platforms" below.

### JVM / Linux / Windows / wasm — Non-Firebase platforms

GitLive doesn't ship on these 5 targets (JVM, linuxX64, linuxArm64, mingwX64, wasmJs), so use **Firebase Measurement Protocol over HTTP** for event capture parity.

1. **Generate an MP API secret** at Firebase Console → Project settings → Integrations → GA4 → Data Streams → {your stream} → **Measurement Protocol API secrets** → Create.

2. **Store the secret in your app's secrets store** — env var, encrypted prefs, keychain, or `release-layer/.env` (gitignored). NEVER hard-code or commit.

3. **Wire `MeasurementProtocolAnalyticsHelper` in your Koin module**:

   ```kotlin
   import com.russhwolf.settings.Settings
   import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
   import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MeasurementProtocolAnalyticsHelper
   import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig

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

4. Events from MP land in the SAME `analytics_*.events_*` BigQuery table as GitLive-emitted events. `/idea analytics --fetch` works identically across all 15 targets.

**What you give up vs native SDK on these platforms:**
- No automatic events (`first_open`, `session_start`, `in_app_purchase`) — emit manually if needed
- No DebugView (events visible only in BigQuery, ~1h latency)
- No A/B Testing tie-in
- No demographics inference
- No platform-native session tracking — supply `engagement_time_msec` param manually if you need engagement metrics

**What still works:**
- Custom event capture with up to 25 params per event
- User properties + user ID
- Persistent client_id on platforms with KV storage (JS) — in-memory on JVM / Linux native / mingwX64 / wasmJs
- Async batching (5s debounce or 25 events, whichever first)
- Silent failure on network errors (analytics never breaks the app)

## Usage

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.*
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule

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

You then depend only on the `AnalyticsHelper` **interface** — no Firebase types leak into your feature code — and just call `logEvent(...)` wherever you record.

### Opt-in / opt-out & consent

Firebase automatically collects **user-acquisition** (`first_open` source/medium/campaign) and **behaviour/engagement** (`session_start`, `user_engagement`, `screen_view`) events. Collection is **on by default (opt-in-by-default)**; you control it with two calls on `AnalyticsHelper`:

```kotlin
// Opt-in-required (GDPR) — start OFF, enable after the user consents:
val analytics = AnalyticsModule.analyticsHelper(
    AnalyticsModule.Mode.Firebase,
    AnalyticsConfig(collectionEnabledByDefault = false),
)

// End-user opts out in Settings → stops ALL collection incl. the auto acquisition/behaviour events:
analytics.setCollectionEnabled(false)   // native: persisted; MP tier: stops sending
analytics.setCollectionEnabled(true)    // opt back in

// Granular GDPR Consent Mode (native Firebase → ANALYTICS_STORAGE / AD_STORAGE):
analytics.setConsent(analyticsStorage = true, adStorage = false)
```

`setCollectionEnabled(false)` is honoured on **every** tier — native Firebase (`setAnalyticsCollectionEnabled`, persisted across restarts) and the Measurement-Protocol fallback (the helper simply stops POSTing). No need to swap the binding to `NoOpAnalyticsHelper`.

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
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.FirebaseAnalyticsHelper

val helper = FirebaseAnalyticsHelper(Firebase.analytics)
```

`FirebaseAnalyticsHelper` is only available on firebaseMain (Android/iOS/macOS/tvOS/JS). JVM is on the nonFirebase tier — cross-platform code should call `provideAnalyticsHelper()` instead, which returns the appropriate helper per platform and `NoOpAnalyticsHelper` on unconfigured nonFirebase targets.

## Auto-injected `kmp_platform` param

Every event gets a `kmp_platform` param injected by the helper — disambiguates events in BigQuery / Firebase Console by platform-of-origin:

| Source target | `kmp_platform` value |
|---|---|
| `androidMain` | `"android"` |
| `iosMain` (×3) | `"ios"` |
| `macosMain` (×2) | `"macos"` |
| `tvosMain` (×3) | `"tvos"` |
| `jvmMain` | `"jvm"` |
| `jsMain` | `"js"` |
| `linuxMain` (×2) | `"linux"` |
| `mingwMain` | `"mingw"` |
| `wasmJsMain` | `"wasmjs"` |

Why a custom key, not GA4's built-in `platform`:
- GA4's auto-`platform` is coarse: only `"android" | "ios" | "web"`
- MP HTTP events don't get auto-`platform` unless we set it
- Sub-platforms (tvOS vs iOS, macOS vs iOS — all "Apple") collapse to `"ios"` in GA4's field
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
- Provide an opt-out toggle in app settings; bind it to `analytics.setCollectionEnabled(false)` (halts collection incl. the auto acquisition/behaviour events on every tier), and/or `setConsent(analyticsStorage = false)` for GDPR Consent Mode

## Project consumer pattern

```
my-project/source/my-project/
├── core/
│   └── analytics/                        ← thin glue layer per project
│       ├── build.gradle.kts              ← depends on cmp-firebase
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
