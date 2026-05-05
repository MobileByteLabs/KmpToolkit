# cmp-firebase-analytics — Manual Setup Guide

End-to-end manual integration. For AI-assisted setup, use [`/sync-firebase-analytics`](CLAUDE_AI_SETUP.md).

---

## 1. Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
cmp-firebase-analytics = "1.0.0"

[libraries]
cmp-firebase-analytics = { module = "io.github.mobilebytelabs:cmp-firebase-analytics", version.ref = "cmp-firebase-analytics" }
```

```kotlin
// build.gradle.kts (KMP shared module)
commonMain.dependencies {
    implementation(libs.cmp.firebase.analytics)
}
```

That's the only Gradle dependency. The library auto-bundles Ktor + GitLive Firebase Analytics + multiplatform-settings transitively.

---

## 2. Project-Specific Firebase Config (per consuming app)

Every consuming project provides its own Firebase credentials. The library bakes in **none**.

### Android — `google-services.json`

1. Firebase Console → Project Settings → General → Your apps → Android → Download `google-services.json`
2. Drop into `androidApp/google-services.json`
3. Apply the plugin in `androidApp/build.gradle.kts`:

   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```

4. Add Firebase BoM + Analytics SDK:

   ```kotlin
   dependencies {
       implementation(platform("com.google.firebase:firebase-bom:32.0.0"))
       implementation("com.google.firebase:firebase-analytics")
   }
   ```

### iOS / macOS / tvOS — `GoogleService-Info.plist`

1. Firebase Console → Project Settings → Your apps → iOS → Download `GoogleService-Info.plist`
2. Drop into the corresponding app target (drag into Xcode project)
3. In your `@main App` (or AppDelegate):

   ```swift
   import FirebaseCore

   @main
   struct MyApp: App {
       init() {
           FirebaseApp.configure()
       }
       // ...
   }
   ```

4. Pod dependencies are auto-bundled by GitLive — see your project's `cmp-ios/Podfile`.

### JS — Firebase config object

```kotlin
// jsMain — typically in your app entry point
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize

Firebase.initialize(
    options = FirebaseOptions(
        apiKey = "YOUR_API_KEY",
        applicationId = "YOUR_APP_ID",
        projectId = "your-firebase-project",
        // ... see Firebase Console for full config
    )
)
```

### JVM — limited support

GitLive's JVM Firebase Analytics is best-effort; without configuration it falls back to no-op gracefully. Most JVM apps target `jvm()` for desktop Compose — those pull `androidMain` Firebase via JetBrains Compose Desktop.

### Non-Firebase platforms — watchOS / Linux / Windows / wasm

GitLive doesn't ship Firebase Analytics on these targets. For event capture, use **Firebase Measurement Protocol** — see [§7 below](#7-non-firebase-platforms-watchos--linux--windows--wasm).

---

## 3. Wire DI (Koin)

```kotlin
import io.github.mobilebytelabs.kmptoolkit.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.analytics.PerformanceTracker
import io.github.mobilebytelabs.kmptoolkit.analytics.di.AnalyticsModule

val analyticsModule = module {
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
            else                   AnalyticsModule.Mode.Firebase
        )
    }
    single { AnalyticsModule.performanceTracker(get()) }
}

// Add to startKoin
startKoin {
    modules(analyticsModule, /* ... */)
}
```

`AnalyticsModule.Mode.Firebase` resolves via `provideAnalyticsHelper()` — returns `FirebaseAnalyticsHelper` on firebaseMain platforms, `NoOpAnalyticsHelper` on the others (until you wire MP — see §7).

---

## 4. Basic Usage

```kotlin
import io.github.mobilebytelabs.kmptoolkit.analytics.*

class SettingsViewModel(private val analytics: AnalyticsHelper) {

    init {
        analytics.logScreenView("settings", sourceScreen = "home")
    }

    fun onSaveClick() {
        analytics.logButtonClick("save", screenName = "settings")
    }

    fun onError(msg: String) {
        analytics.logError(msg, errorCode = "E001", screen = "settings")
    }
}
```

### Direct event logging

```kotlin
analytics.logEvent(EventTypes.BUTTON_CLICK,
    ParamKeys.BUTTON_NAME to "save",
    ParamKeys.SCREEN_NAME to "settings",
)

analytics.logStateTransition("settings", from = "loading", to = "content")
```

### Builder DSL

```kotlin
analytics.log(EventTypes.FORM_COMPLETED) {
    param(ParamKeys.FORM_NAME, "registration")
    param(ParamKeys.COMPLETION_TIME, 45)  // numbers auto-stringified
}
```

### Performance timing

```kotlin
val tracker: PerformanceTracker = koinInject()
tracker.measure("settings_screen_render") {
    // render work — emits loading_time event with duration_ms
}
```

---

## 5. User Properties + User ID

```kotlin
// User attributes for segmentation
analytics.setUserProperty("user_type", "premium")
analytics.setUserProperty("preferred_language", "en")

// User ID — MUST be hashed/obfuscated. NEVER raw email/phone.
analytics.setUserId(hashedUserId)

// Clear on logout
analytics.setUserId("")
```

Firebase constraints (auto-truncated):
- User property name: ≤ 24 chars
- User property value: ≤ 36 chars
- User ID: ≤ 256 chars

---

## 6. Testing

```kotlin
import io.github.mobilebytelabs.kmptoolkit.analytics.TestAnalyticsHelper

@Test fun `clicking save logs button_click event`() {
    val analytics = TestAnalyticsHelper()
    val viewModel = SettingsViewModel(analytics)

    viewModel.onSaveClick()

    val event = analytics.events.single()
    assertEquals(EventTypes.BUTTON_CLICK, event.type)
    assertEquals("save", event.extras.first { it.key == ParamKeys.BUTTON_NAME }.value)

    // Convenience assertions
    assertEquals(1, analytics.countOf(EventTypes.BUTTON_CLICK))
    assertEquals("save", analytics.lastOf(EventTypes.BUTTON_CLICK)?.extras?.first()?.value)
}
```

---

## 7. Non-Firebase platforms (watchOS / Linux / Windows / wasm)

GitLive doesn't ship Firebase Analytics on these 10 targets. Use **Firebase Measurement Protocol over HTTP** to land events in the same Firebase property + same BigQuery export.

### What an MP API secret is

A short string token (typically 22 chars, looks like `abc1d2e3f4-XYZ_a8B7c6D5e4F3g2H`) that authenticates HTTP POSTs to Google's Measurement Protocol endpoint. It is the **only** Firebase credential that authorizes write-events-via-HTTP to a specific GA4 data stream — and it is the **least-privileged** Firebase credential (can't read analytics, can't admin, can't access other Firebase services).

Native Firebase SDKs (Android/iOS/JS) authenticate via `google-services.json` / `GoogleService-Info.plist` / Firebase Web Config. Those credentials don't reach `MeasurementProtocolAnalyticsHelper` — the HTTP path needs its own token, which is the MP API secret.

### Generate an MP API secret

Step-by-step (Firebase Console UI):

1. Open **Firebase Console** → click your project
2. Click the **gear icon** (top-left, next to "Project Overview") → **Project Settings**
3. Click the **Integrations** tab
4. Find the **Google Analytics** card → click **Manage** (or **Open in GA4**)
5. In the GA4 admin pane, navigate: **Admin → Data Streams**
6. Click your stream — pick the one that matches the platform:
   - **Web stream** → for `wasmJs` / browser deploys
   - **iOS stream** → for `watchOS` (uses the iOS app's stream)
   - **Android stream** → not relevant here (Android uses native SDK)
   - For Linux/Windows native targets, use whichever stream represents your "desktop" presence (often Web)
7. Scroll to **Measurement Protocol API secrets** (near the bottom of the page)
8. Click **Create**
9. Give it a descriptive name (e.g., `watchos-prod`, `linux-staging`)
10. **Copy the secret value immediately — it is shown ONCE.** If you lose it, you create a new one.

### Where the secret goes

Drop the value into your app's secrets store:

```bash
# release-layer/.env (gitignored)
MP_API_SECRET=abc123...
```

```yaml
# idea-layer/PROJECT_CONFIG.yaml
analytics:
  envs:
    prod:
      property_id: G-XXXXXXXX                       # GA4 measurement ID
      measurement_protocol:
        api_secret_secret_ref: MP_API_SECRET        # env var name
```

### Wire `MeasurementProtocolAnalyticsHelper` in Koin

```kotlin
import com.russhwolf.settings.Settings
import io.github.mobilebytelabs.kmptoolkit.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MeasurementProtocolAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MpConfig

val analyticsModule = module {
    single<AnalyticsHelper> {
        // Pick per-platform helper:
        //  - firebaseMain platforms: provideAnalyticsHelper() returns FirebaseAnalyticsHelper
        //  - nonFirebaseMain platforms (watchOS/Linux/etc.): wire MP explicitly
        if (BuildConfig.DEBUG) {
            StubAnalyticsHelper()
        } else {
            MeasurementProtocolAnalyticsHelper(
                config = MpConfig(
                    measurementId = "G-XXXXXXXX",                       // GA4 measurement ID
                    apiSecret     = SecureStore.read("MP_API_SECRET"),  // your secrets store
                ),
                settings = Settings(),                                  // multiplatform-settings
            )
        }
    }
}
```

`MeasurementProtocolAnalyticsHelper` accepts the SAME `AnalyticsHelper` interface — your ViewModels don't change. Only the DI wiring differs per platform.

### Secret hygiene

- **Never commit** the secret. `release-layer/.env` should be in `.gitignore`.
- **Never log it.** The library never prints it; you shouldn't either.
- **Rotate** if it leaks: Firebase Console → revoke the old secret → create a new one → redeploy.
- **Per environment**: create separate secrets for prod/staging/dev. Don't share across environments.
- **Per project**: each Firebase project has its own MP secrets. `mood-movies`'s secret won't work for `reels-downloader`.
- **CI**: load via your CI's secrets vault (GitHub Actions secrets, etc.). Never hard-code in `build.gradle.kts` or YAML.

### How MP secret differs from other Firebase credentials

| Credential | Used by | Where it lives | What it does |
|---|---|---|---|
| `google-services.json` | Native Android Firebase SDK | `androidApp/` | Auto-config: API key, app ID, project ID, sender ID |
| `GoogleService-Info.plist` | Native iOS/macOS/tvOS Firebase SDK | Xcode project | Same, for Apple platforms |
| Firebase Web Config object | Firebase JS SDK | `jsMain` init | Same, for browser |
| Firebase Service Account JSON | Server-side admin (Crashlytics fetch via `/idea firebase-crash`, etc.) | `secrets/firebaseAppDistributionServiceCredentialsFile.json` | High-privilege; signs JWTs for any Firebase API |
| **MP API secret** | HTTP `POST /mp/collect` only | `release-layer/.env` | **Only** authorizes writing events to one specific GA4 data stream |

The MP secret is the **least powerful** credential in this list. That's intentional — least-privilege for an HTTP fallback.

### What works vs native SDK

| Feature | firebaseMain (GitLive) | nonFirebaseMain (MP HTTP) |
|---|:-:|:-:|
| Custom event capture | ✅ | ✅ |
| User properties + user ID | ✅ | ✅ |
| Persistent client_id | ✅ from GitLive | ✅ via multiplatform-settings (Apple/JS); in-memory on Linux/mingw/wasmWasi |
| Async batching | ✅ native | ✅ 5s/25-event debounce |
| BigQuery export | ✅ | ✅ same dataset |
| DebugView | ✅ | ❌ |
| Automatic events (`first_open`, `session_start`, `in_app_purchase`) | ✅ | ❌ — manual log if needed |
| A/B Testing tie-in | ✅ | ❌ |
| Demographics inference | ✅ | ❌ |
| Latency to BigQuery | ~1h | ~1h |

---

## 8. Privacy

The library does NOT auto-redact, but provides primitives to enforce privacy:

- **`pii: true` in screen YAML** (claude-product-cycle framework) → codegen NEVER auto-instruments
- **`EventValidator`** — debug-build regex check for email/phone/SSN/credit-card patterns in param values
- **`setUserId(hashedUserId)`** — never pass raw PII; hash client-side first

Recommended boundaries:

```kotlin
// User ID — always hashed
analytics.setUserId(sha256(rawUserId).take(16))

// Event params — never raw user input. Categorize first.
analytics.logEvent(EventTypes.SEARCH_PERFORMED,
    ParamKeys.RESULT_COUNT to results.size.toString(),
    // DO NOT: ParamKeys.SEARCH_TERM to userInput  ← raw user input may include PII
)
```

---

## 9. Optional: EventRegistry (declared-event enforcement)

For apps with strict event taxonomy, install a registry to reject unregistered events at runtime:

```kotlin
val SettingsRegistry = EventRegistry(
    scope = "settings",
    events = setOf(
        "settings_screen_viewed",
        "settings_save_clicked",
    ),
)

val analytics: AnalyticsHelper = if (BuildConfig.DEBUG) {
    RegistryValidatingHelper(realHelper, SettingsRegistry) { violation ->
        Logger.w { "Analytics: $violation" }
    }
} else {
    realHelper
}
```

Production builds skip the wrapper for zero overhead.

---

## Reference

- [Firebase Analytics constraints](https://firebase.google.com/docs/reference/cpp/struct/firebase/analytics/parameter)
- [Firebase Measurement Protocol (GA4)](https://developers.google.com/analytics/devguides/collection/protocol/ga4)
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings)
