# cmp-firebase — Manual Setup Guide

End-to-end manual integration. For AI-assisted setup, use [`/sync-firebase-analytics`](CLAUDE_AI_SETUP.md).

---

## 1. Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
cmp-firebase = "1.0.0"

[libraries]
cmp-firebase = { module = "io.github.mobilebytelabs:cmp-firebase", version.ref = "cmp-firebase" }
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

### Analytics is org-global — which secrets live where (→ vault)

All apps in the workspace stream into **ONE** Firebase project (`prod-applications-c7f87`)
linked to **ONE** GA4 property. So the analytics/crash *engine* is configured with just two
**org-level** secrets — the single global analytics point — and each app only supplies its own
**project-level** stream identity. Every secret maps to a vault alias (canonical naming SoT:
`core/registries/SECRETS_ORG_ALIAS_CANONICAL.yaml`; resolve with `/secrets pull`,
RULE-SECRETS-VAULT-001 — never `.env`, never `gh secret set`):

| Scope | Secret | Vault alias | Category | Used by |
|-------|--------|-------------|----------|---------|
| **ORG** (workspace) | Firebase / GCP service-account JSON | `<ws>-firebase-sa` (e.g. `mbs-firebase-sa`) | `google_services_sa` | growth GA4 Data API read · Firebase Management API · app discovery |
| **ORG** (workspace) | Shared GA4 **property id** (numeric, e.g. `473327398`) | `<ws>-ga4-property-id` (e.g. `mbs-ga4-property-id`) | `env_var` | growth fetch (slices per-app by `streamId`) |
| **PROJECT** (per app) | Android `google-services.json` | `<proj>-firebase-google-services` | `firebase_config_android` | native Android SDK init |
| **PROJECT** (per app) | iOS/macOS `GoogleService-Info.plist` | `<proj>-firebase-ios-plist` | `firebase_config_ios` | native Apple SDK init |
| **PROJECT** (per app) | Firebase app id (android / ios) | `<proj>-firebase-android-app-id` · `<proj>-firebase-ios-app-id` | `env_var` | per-app identity in the shared project |
| **PROJECT** (per app) | GA4 **measurement id** (`G-XXXXXXXX`, per data stream) | `<proj>-ga4-measurement-id` | `env_var` | MP tier (jvm/linux/mingw) |
| **PROJECT** (per app) | Measurement-Protocol API secret (per stream) | `<proj>-mp-api-secret` | `env_var` | MP tier |

> **Property id vs measurement id** — the *property id* is the org-shared numeric GA4 property
> (`<ws>-ga4-property-id`); the *measurement id* is the per-app data-stream key `G-XXXX`
> (`<proj>-ga4-measurement-id`). They are different values at different scopes — don't conflate.
> The native SDK reads its stream from the per-app config file (google-services.json / plist);
> only the MP tier passes the measurement id + api secret explicitly via `MpConfig`.

**Automatic resolution — the tooling knows where to look.** Each alias carries a machine-readable
`scope` (`workspace` | `project`) in `SECRETS_ALIAS_REGISTRY.yaml`, and the resolver reports it:

```bash
core/scripts/secrets-resolve-path.sh mbs-firebase-sa            --emit tier   # → workspace
core/scripts/secrets-resolve-path.sh mbs-ga4-property-id        --emit tier   # → workspace
core/scripts/secrets-resolve-path.sh <proj>-firebase-google-services --emit tier  # → project
```

So `/secrets pull` and every consumer route automatically — **workspace**-tier secrets are ONE
shared value (direct-wired from the vault, never duplicated per project); **project**-tier secrets
materialize into the bound project's `source/<repo>/secrets/live/…`. Nothing hard-codes a path or a
project name — the alias's scope decides. (Single GA4: all apps push project-wise via their own data
stream, then platform-wise via `kmp_platform`, into the one org property.)

### Library initialization

Call `FirebaseKit.initialize()` once at app startup, before any event logging. Android auto-initializes via a `ContentProvider` — no explicit call needed there. All other platforms:

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import dev.gitlive.firebase.FirebaseOptions
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig

// No-arg: each platform reads its own config file (plist / google-services.json)
FirebaseKit.initialize()

// OR — builder form: required for JS/web; also used to pass MpConfig for non-Firebase targets
FirebaseKit.initialize(
    FirebaseConfig.builder()
        .android(FirebaseOptions(/* optional override */))
        .apple(FirebaseOptions(/* optional override */))
        // `web` serves BOTH the `js` AND `wasmJs` targets — see the wasmJs note below.
        .web(FirebaseOptions(
            apiKey        = "YOUR_WEB_API_KEY",
            applicationId = "YOUR_APP_ID",
            projectId     = "your-firebase-project",
            authDomain    = "your-firebase-project.firebaseapp.com",
        ))
        .measurementProtocol(MpConfig(measurementId = "G-XXXX", apiSecret = "..."))
        .build()
)

// Optional: global fatal-exception capture (real on JVM/Android; no-op on native/JS/wasm)
FirebaseKit.installUncaughtHandler()
```

> **Crash → GA4**: every captured crash emits an `app_crash` GA4 event with `kmp_platform`, `exception_type`, and `fatal` params — visible in the same GA4 property and BigQuery export across all platforms. On the non-Firebase tier this requires `MpConfig` to be set (otherwise no-op).

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

### iOS / macOS / tvOS — two supported paths

Pick **one**. Path A is the library's own commonMain surface and needs no plist and no Swift
code; Path B is the classic native-config route. Do not do both.

#### Path A (recommended) — programmatic, no plist, no Swift

Pass `apple` options to `FirebaseKit.initialize(config)` from commonMain. Firebase is configured
programmatically, so there is **no `GoogleService-Info.plist` and no `FirebaseApp.configure()`
line at all**:

```kotlin
FirebaseKit.initialize(
    FirebaseConfig.builder()
        .apple(FirebaseOptions(
            applicationId = "1:123:ios:abc",   // required
            apiKey        = "YOUR_API_KEY",     // required
            projectId     = "your-firebase-project",
            gcmSenderId   = "123",              // required by Apple's native FIROptions
        ))
        .build()
)
```

> `gcmSenderId` is **not optional on Apple** — the native `FIROptions` constructor requires it.
> Values come from the same Firebase Console screen that would have produced the plist.

#### Path B — `GoogleService-Info.plist` (classic)

Use the no-arg `FirebaseKit.initialize()` and let the platform read its own config file:

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

#### Linking (applies to BOTH paths)

**No Podfile.** From GitLive `3.0.0` the native `firebase-ios-sdk` is linked via **SwiftPM**, not
CocoaPods, and it flows across the Maven boundary automatically — do **not** re-declare it.

- Build your shared framework **static**: `iosArm64().binaries.framework { isStatic = true }`
  (Firebase's SwiftPM products are static libraries; a dynamic framework crashes at runtime).
- In Xcode use **direct integration** — add the `embedAndSignAppleFrameworkForXcode` run-script
  build phase. This replaces `pod install`.
- Set the deployment target to **iOS 15.0** / macOS 10.15 / tvOS 15.0 (`firebase-ios-sdk` 12.x minimum).

Full steps: [`cmp-firebase/README.md`](../../cmp-firebase/README.md#ios--macos--tvos-swiftpm--gitlive-3x).

### JS — Firebase config object

Pass a web `FirebaseOptions` via `FirebaseKit.initialize(config)` (the cleaner library-level path); `Firebase.initialize(options=...)` from GitLive also works as a lower-level alternative.

```kotlin
// jsMain — typically in your app entry point
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import dev.gitlive.firebase.FirebaseOptions

FirebaseKit.initialize(
    FirebaseConfig.builder()
        .web(FirebaseOptions(
            apiKey        = "YOUR_API_KEY",
            applicationId = "YOUR_APP_ID",
            projectId     = "your-firebase-project",
            // ... see Firebase Console → Project settings → Your web app
        ))
        .build()
)
```

> ### ⚠️ BREAKING for wasmJs — `wasmJs` moved to the native Firebase tier
>
> **From GitLive `3.0.0-alpha02` (KmpToolkit 3.5.21+), `wasmJs` is no longer a Measurement-Protocol
> target.** Upstream [PR #832](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/832) gives wasmJs
> full parity with the JS target, backed by the same Firebase **JS** SDK, so `cmp-firebase` promotes
> `wasmJsMain` onto `firebaseMain`.
>
> **What you must change:** a wasmJs app that previously supplied only `measurementProtocol` must now
> also supply **`web`** (`apiKey`, `applicationId`, `projectId`, `authDomain`). `wasmJs` reads the
> *same* `FirebaseConfig.web` entry as `js` — there is no separate `wasmJs` entry.
>
> **If you skip it:** native init is silently skipped and analytics NoOps — you will not get an
> exception at config time, so this fails quietly. Supply `web` (or keep the target off the tier by
> pinning `gitliveFirebase` below `3.0.0-alpha02`).
>
> **Unchanged:** Crashlytics. `firebase-crashlytics` did *not* gain `wasmjs` upstream, so wasmJs stays
> on `crashlyticsFallbackMain` (`LoggingCrashReporter`). `measurementProtocol` is still required for
> jvm / linux / mingw.
>
> Every section of this guide has been updated for this change (2026-09-03).

### JVM / Linux / mingw — MP tier

GitLive doesn't ship usable native Firebase Analytics for `jvm`, `linuxX64`, `linuxArm64`, or `mingwX64`. For event capture on these 4 targets, use **Firebase Measurement Protocol** — see [§7 below](#7-non-firebase-platforms-jvm--linux--mingw).

> `wasmJs` was on this tier before GitLive `3.0.0-alpha02`; it is now a native Firebase target and reads `FirebaseConfig.web` instead.

---

## 3. Wire DI (Koin)

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.PerformanceTracker
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule
import io.github.mobilebytelabs.kmptoolkit.firebase.crash.di.CrashReporterModule

val firebaseModule = module {
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            mode   = if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
                     else                   AnalyticsModule.Mode.Firebase,
            config = null,  // pass MpConfig here for non-Firebase targets (see §7)
        )
    }
    single { AnalyticsModule.performanceTracker(get()) }
    single {
        CrashReporterModule.crashReporter(
            mode = if (BuildConfig.DEBUG) CrashReporterModule.Mode.Stub
                   else                   CrashReporterModule.Mode.Firebase
        )
    }
}

// Add to startKoin
startKoin {
    modules(firebaseModule, /* ... */)
}
```

`AnalyticsModule.analyticsHelper()` is a shared process singleton via `provideAnalyticsHelper()` — returns `FirebaseAnalyticsHelper` on firebaseMain platforms (Android, iOS, macOS, tvOS, JS), `NoOpAnalyticsHelper` on the non-Firebase tier (until you wire `MpConfig` — see §7).

---

## 4. Basic Usage

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.*

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
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.TestAnalyticsHelper

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

## 7. Non-Firebase platforms (JVM / Linux / mingw)

GitLive doesn't ship usable native Firebase Analytics on these 4 targets: `jvm`, `linuxX64`, `linuxArm64`, `mingwX64`. Use **Firebase Measurement Protocol over HTTP** to land events in the same Firebase property + same BigQuery export.

> **`wasmJs` is no longer in this list.** GitLive `3.0.0-alpha02` gave it full JS parity, so it runs the native Firebase JS SDK and needs `FirebaseConfig.web`. Crashlytics is the exception — upstream did not add `wasmjs` there, so wasmJs still uses the logging fallback for crash reporting.

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
   - **Web stream** → for browser deploys (`js`; and `wasmJs` pre-alpha02)
   - **Android stream** → not relevant here (Android uses native SDK)
   - For `jvm` / `linuxX64` / `linuxArm64` / `mingwX64`, use whichever stream represents your "desktop" presence (often Web)
7. Scroll to **Measurement Protocol API secrets** (near the bottom of the page)
8. Click **Create**
9. Give it a descriptive name (e.g., `jvm-prod`, `linux-staging`)
10. **Copy the secret value immediately — it is shown ONCE.** If you lose it, you create a new one.

### Where the secret goes

**Recommended (vault-first):** load from the org vault via `/secrets pull` — never commit raw values.

```bash
# Materialize org-scoped secrets locally (per RULE-SECRETS-VAULT-001 — no .env, no gh secret set)
/secrets pull
# Vault aliases used by cmp-firebase (org-global analytics, see §2 table):
#   mbs-firebase-sa           → Firebase / GCP service-account JSON        (ORG / workspace)
#   mbs-ga4-property-id        → shared GA4 PROPERTY id, numeric e.g. 473327398  (ORG / workspace)
#   <proj>-ga4-measurement-id → this app's GA4 stream MEASUREMENT id, G-XXXXXXXX (PROJECT)
#   <proj>-mp-api-secret      → this app's Measurement-Protocol API secret       (PROJECT)
```

> The SAME GA4 property the library writes to (via native SDK or MP) is what the framework's growth dashboard reads via the GA4 Data API — so `app_crash` events, retention, and per-platform engagement all appear in one BigQuery export once the stream is enabled.

Fallback (e.g., initial local dev before vault onboarding):

```bash
# release-layer/.env  (gitignored — verify it is in .gitignore)
MP_API_SECRET=abc123...
```

```yaml
# idea-layer/PROJECT_CONFIG.yaml
analytics:
  envs:
    prod:
      property_id: G-XXXXXXXX                       # GA4 measurement ID (not a secret)
      measurement_protocol:
        api_secret_secret_ref: MP_API_SECRET        # env var name; resolved at runtime
```

### Wire `MeasurementProtocolAnalyticsHelper` in Koin

```kotlin
import com.russhwolf.settings.Settings
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MeasurementProtocolAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig

val analyticsModule = module {
    single<AnalyticsHelper> {
        // Pick per-platform helper:
        //  - firebaseMain platforms: provideAnalyticsHelper() returns FirebaseAnalyticsHelper
        //  - nonFirebaseMain platforms (jvm/linux/mingw): wire MP explicitly
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

- **Use the vault**: load via `/secrets pull` (aliases `mbs-firebase-sa`, `mbs-ga4-property-id`, `<proj>-mp-api-secret`). Never `.env` as primary, never `gh secret set` (RULE-SECRETS-VAULT-001).
- **Never commit** the secret value. `release-layer/.env` must be in `.gitignore` (fallback only).
- **Never log it.** The library never prints it; you shouldn't either.
- **Rotate** if it leaks: Firebase Console → revoke the old secret → create a new one → `/secrets pull` → redeploy.
- **Per environment**: create separate secrets for prod/staging/dev. Don't share across environments.
- **Per project**: each Firebase project has its own MP secrets. `mood-movies`'s secret won't work for `reels-downloader`.
- **CI**: `/secrets pull` in CI via the vault. Never hard-code in `build.gradle.kts` or YAML.

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
| Persistent client_id | ✅ from GitLive | ✅ via multiplatform-settings (Apple/JS); in-memory on Linux/mingw |
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
