# /sync-firebase-analytics - Full Instructions

> **Single source of truth** for `cmp-firebase` sync contract.
> The framework `/lib-sync cmp-firebase` delegates to this file.
> Update this file when the library version changes.

---

# /sync-firebase-analytics — cmp-firebase Sync

Verify-gated sync of `cmp-firebase` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: Firebase config files. Gate 3: DI wiring (Koin).
Gate 4: PROJECT_CONFIG.analytics block. Gate 5: MP API secret (conditional).

---

## Module Contract (update when library changes)

```yaml
module:    cmp-firebase
artifact:  io.github.mobilebytelabs:cmp-firebase
version:   1.0.0
package:   io.github.mobilebytelabs.kmptoolkit.firebase.analytics
supabase:  false
di:        true   # Koin module via AnalyticsModule.analyticsHelper(Mode)
nav:       false
config:    project_specific  # Each project supplies own google-services.json + GoogleService-Info.plist + (optional) MP API secret. Library bakes in NO defaults.

target_coverage: 21/21       # Full KMP — true coverage
target_tiers:
  firebaseMain:              # 11 targets — GitLive Firebase native SDK
    - androidMain
    - jvmMain
    - iosMain               # iosX64, iosArm64, iosSimulatorArm64
    - macosMain             # macosX64, macosArm64
    - tvosMain              # tvosX64, tvosArm64, tvosSimulatorArm64
    - jsMain
  nonFirebaseMain:           # 10 targets — Firebase Measurement Protocol via HTTP
    - watchosMain           # watchosX64, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64
    - linuxMain             # linuxX64, linuxArm64
    - mingwMain             # mingwX64
    - wasmJsMain
    - wasmWasiMain

api:
  # Helper interface (commonMain)
  - AnalyticsHelper interface
  - AnalyticsHelper.logEvent(event: AnalyticsEvent)
  - AnalyticsHelper.logEvent(type: String, vararg params: Pair<String, String>)
  - AnalyticsHelper.logEvent(type: String, params: Map<String, String>)
  - AnalyticsHelper.logScreenView(screenName, sourceScreen?)
  - AnalyticsHelper.logButtonClick(buttonName, screenName?)
  - AnalyticsHelper.logError(errorMessage, errorCode?, screen?)
  - AnalyticsHelper.logStateTransition(screenName, from, to)
  - AnalyticsHelper.logFeatureUsed(featureName, screen?)
  - AnalyticsHelper.setUserProperty(name, value)
  - AnalyticsHelper.setUserId(userId)

  # Types (commonMain)
  - data class AnalyticsEvent(type: String, extras: List<Param>)
  - AnalyticsEvent.withParam(key, value)
  - AnalyticsEvent.withParams(vararg Pair<String, String>)
  - AnalyticsEvent.withParams(Map<String, String>)
  - data class Param(key: String, value: String)
  - createParam(key, value): Param?  # safe factory
  - object EventTypes { SCREEN_VIEW, BUTTON_CLICK, FORM_COMPLETED, ERROR_OCCURRED, STATE_TRANSITIONED, ... }
  - object ParamKeys  { SCREEN_NAME, BUTTON_NAME, ERROR_MESSAGE, FROM_STATE, TO_STATE, ... }

  # Built-in helpers (commonMain)
  - class StubAnalyticsHelper           # logs to Kermit
  - object NoOpAnalyticsHelper          # discards events
  - class TestAnalyticsHelper           # captures events; events: List<AnalyticsEvent>, countOf, lastOf, clear
  - class EventValidator                # taxonomy + PII regex check
  - class PerformanceTracker            # start/stop/measure block timer
  - data class EventRegistry            # declared-event enforcement (opt-in)
  - class RegistryValidatingHelper      # debug-build wrapper

  # DSL (commonMain)
  - AnalyticsHelper.log(type: String, builder: AnalyticsEventBuilder.() -> Unit)
  - AnalyticsEventBuilder.param(key, value)         # String
  - AnalyticsEventBuilder.param(key, value: Number)
  - AnalyticsEventBuilder.param(key, value: Boolean)

  # Factory (commonMain expect / per-tier actual)
  - expect fun provideAnalyticsHelper(): AnalyticsHelper
    - firebaseMain  → FirebaseAnalyticsHelper(Firebase.analytics)  # GitLive
    - nonFirebaseMain → NoOpAnalyticsHelper  # default; apps wire MeasurementProtocolAnalyticsHelper for capture

  # DI factory (commonMain)
  - object AnalyticsModule
    - enum Mode { Firebase, Stub, NoOp }
    - analyticsHelper(mode: Mode = NoOp): AnalyticsHelper
    - performanceTracker(helper: AnalyticsHelper): PerformanceTracker

  # firebaseMain only
  - class FirebaseAnalyticsHelper(firebase: FirebaseAnalytics)
    # auto-truncates to Firebase limits (40/100/24/36/256 chars)

  # nonFirebaseMain — Measurement Protocol HTTP (also available on commonMain for cross-tier use)
  - class MeasurementProtocolAnalyticsHelper(config: MpConfig, settings: Settings, httpClient?, scope?)
    - suspend fun flush()
    - fun close()
  - data class MpConfig(measurementId: String, apiSecret: String, endpoint: String = DEFAULT_ENDPOINT)
    - DEFAULT_ENDPOINT = "https://www.google-analytics.com/mp/collect"
    - DEBUG_ENDPOINT   = "https://www.google-analytics.com/debug/mp/collect"
```

---

## Usage

```bash
/sync-firebase-analytics           # Full sync
/sync-firebase-analytics --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-firebase-analytics
        |
        v
+---------------------------------------------------------------+
|  GATE 1: Gradle Dependency                                    |
|  Check: cmp-firebase:1.0.0 in libs.versions.toml   |
|  Check: used in commonMain.dependencies                       |
|  Fix:   Auto-insert correct entries                           |
|  Result: PASS / FIXED / BLOCKED                               |
+--------------------------+------------------------------------+
                           | PASS
                           v
+---------------------------------------------------------------+
|  GATE 2: Firebase Config Files                                |
|  Check: androidApp/google-services.json (Android target)      |
|  Check: cmp-ios/{app}/GoogleService-Info.plist (Apple target) |
|  Check: jsMain Firebase.initialize(...) (JS target)           |
|  Fix:   Surface UI download steps; library NEVER auto-creates |
|  Result: PASS / MISSING (with remediation) / SKIPPED          |
+--------------------------+------------------------------------+
                           | PASS
                           v
+---------------------------------------------------------------+
|  GATE 3: DI Wiring (Koin)                                     |
|  Check: AnalyticsModule import in app's KoinModules.kt        |
|  Check: AnalyticsHelper bound (single<AnalyticsHelper>)       |
|  Fix:   Auto-insert AnalyticsModule + Mode.Firebase wiring    |
|  Result: PASS / FIXED / SKIPPED (no Koin detected)            |
+--------------------------+------------------------------------+
                           | PASS
                           v
+---------------------------------------------------------------+
|  GATE 4: PROJECT_CONFIG.analytics block (optional)            |
|  Check: idea-layer/PROJECT_CONFIG.yaml has analytics block    |
|  Fix:   Auto-scaffold from template (property_id required)    |
|  Result: PASS / FIXED / SKIPPED                               |
+--------------------------+------------------------------------+
                           | PASS
                           v
+---------------------------------------------------------------+
|  GATE 5: MP API Secret (conditional — runs only if nonFirebase
|          platforms targeted)                                  |
|  Trigger: any of watchOS×4 / Linux×2 / mingwX64 / wasmJs /    |
|           wasmWasi present in build.gradle.kts kotlin { ... } |
|  Check: release-layer/.env contains MP_API_SECRET             |
|  Check: PROJECT_CONFIG.analytics.measurement_protocol         |
|         .api_secret_secret_ref is set                         |
|  Fix:   Surface UI step (Firebase Console → MP Secrets);      |
|         prompt for value; write to .env (never to git)        |
|  Result: PASS / FIXED / NOT_NEEDED                            |
+--------------------------+------------------------------------+
                           |
                           v
                     SYNC COMPLETE
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   -> search "cmp-firebase"
   -> if found: verify version = 1.0.0
   -> if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   -> search "cmp.firebase.analytics"
   -> if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
cmp-firebase = "1.0.0"

# libs.versions.toml [libraries]
cmp-firebase = { module = "io.github.mobilebytelabs:cmp-firebase", version.ref = "cmp-firebase" }
```

```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.cmp.firebase.analytics)
```

---

## Gate 2: Firebase Config Files

### Check (per platform target detected)

```
Android target detected (androidLibrary {} in build.gradle.kts):
  test -f androidApp/google-services.json
  → PASS or MISSING

Apple target detected (iosX64 / macosX64 / tvosX64 / etc.):
  find cmp-ios -name "GoogleService-Info.plist"
  → PASS or MISSING

JS target detected (js() in build.gradle.kts):
  grep "Firebase.initialize" jsMain/**/*.kt
  → PASS or MISSING
```

### Fix (NEVER auto-create — library has no keys)

For each MISSING:

```
ANDROID:
  Surface: "Download google-services.json from Firebase Console:
            Project Settings → Your apps → Android → google-services.json
            Drop into androidApp/google-services.json"
  Wait for user confirmation; re-check on next /sync-firebase-analytics run.

APPLE (iOS / macOS / tvOS):
  Surface: "Download GoogleService-Info.plist from Firebase Console:
            Project Settings → Your apps → iOS → GoogleService-Info.plist
            Drop into cmp-ios/{appName}/GoogleService-Info.plist
            Drag into Xcode project (Copy if needed)
            Add FirebaseApp.configure() in @main App init { }"

JS:
  Surface: "Add Firebase.initialize(FirebaseOptions(...)) in jsMain entry.
            Get config from Firebase Console → Project Settings → SDK setup."

JVM (limited):
  No-op — falls back to GitLive's no-op gracefully.
```

---

## Gate 3: DI Wiring (Koin)

### Check
```
1. Glob: **/KoinModules.kt OR **/AppModule.kt
2. Search for "AnalyticsHelper" binding
3. If missing: mark for fix
```

### Fix
```kotlin
// KoinModules.kt (or app's main DI module)
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule
import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
            else                   AnalyticsModule.Mode.Firebase
        )
    }
    single { AnalyticsModule.performanceTracker(get()) }
}

// Append `analyticsModule` to existing startKoin { modules(...) } call.
```

---

## Gate 4: PROJECT_CONFIG.analytics block

### Check
```
1. Read: workspaces/{ws}/{project}/idea-layer/PROJECT_CONFIG.yaml
2. yq: .analytics.envs.{env}.property_id present?
3. If missing: mark for fix
```

### Fix
```yaml
# idea-layer/PROJECT_CONFIG.yaml — add analytics block
analytics:
  configured: true
  configured_at: "{ISO_8601_NOW}"
  envs:
    prod:
      project_id: "{firebase_project_id}"     # prompt user
      property_id: "G-XXXXXXXX"               # prompt user — GA4 measurement ID
      bigquery_dataset: "analytics_{property_numeric}"  # auto-detect from BigQuery export
      lookback_days_default: 7
      top_n_default: 10
      # measurement_protocol block added by Gate 5 only if nonFirebase platforms targeted
```

---

## Gate 5: MP API Secret (conditional)

### Trigger detection

```
Scan source/{proj}/build.gradle.kts kotlin { ... } block for any of:
  watchosX64, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64
  linuxX64, linuxArm64
  mingwX64
  wasmJs (with browser/nodejs)
  wasmWasi

If ANY found → Gate 5 runs. Else → Gate 5 = NOT_NEEDED.
```

### Check
```
1. test -f workspaces/{ws}/{project}/release-layer/.env
2. grep "^MP_API_SECRET=" release-layer/.env
3. yq: .analytics.envs.{env}.measurement_protocol.api_secret_secret_ref present?
```

### Fix

```
Surface UI step:
  "Generate Firebase Measurement Protocol API secret:
     Firebase Console → Project Settings → Integrations → GA4 →
     Data Streams → {your stream} → Measurement Protocol API secrets →
     Create new secret. Copy the value (shown once)."

Prompt user: "Paste the MP API secret:"
Write to release-layer/.env (gitignored):
  MP_API_SECRET={pasted_value}

Update PROJECT_CONFIG:
  analytics.envs.{env}.measurement_protocol.api_secret_secret_ref: MP_API_SECRET
```

After Gate 5 passes, surface the wiring snippet for the app to install:

```kotlin
// In your nonFirebaseMain Koin wiring (e.g., watchosMain):
import com.russhwolf.settings.Settings
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.*

val analyticsModule = module {
    single<AnalyticsHelper> {
        MeasurementProtocolAnalyticsHelper(
            config = MpConfig(
                measurementId = "G-XXXXXXXX",                       // from PROJECT_CONFIG
                apiSecret     = SecureStore.read("MP_API_SECRET"),
            ),
            settings = Settings(),
        )
    }
}
```

---

## --check (Dry Run)

```
GATE 1   Gradle           [status]  cmp-firebase:1.0.0
GATE 2   Firebase config  [status]  google-services.json + Plist + JS init
GATE 3   DI wiring        [status]  AnalyticsHelper bound in Koin
GATE 4   PROJECT_CONFIG   [status]  analytics block present
GATE 5   MP secret        [status / N/A]
```

---

## State Summary Output

```
+======================================================================+
|  /sync-firebase-analytics — COMPLETE                                  |
+======================================================================+
|  GATE 1   Gradle           [OK]  cmp-firebase:1.0.0        |
|  GATE 2   Firebase config  [OK]  google-services.json + Plist + JS   |
|  GATE 3   DI wiring        [OK]  AnalyticsHelper bound in Koin       |
|  GATE 4   PROJECT_CONFIG   [OK]  analytics block present             |
|  GATE 5   MP secret        [N/A or OK]                               |
+----------------------------------------------------------------------+
|  Targets covered: 21/21                                              |
|    firebaseMain (11): Android, JVM, iOS x3, macOS x2, tvOS x3, JS    |
|    nonFirebaseMain (10): {N/A or active list}                        |
+----------------------------------------------------------------------+
|  Docs:    docs/firebase-analytics/SETUP.md                           |
|  Next:    /idea analytics --setup  (framework: enable BigQuery)     |
+======================================================================+
```

---

## How to Evolve This File

1. **Version bump** -> update `version: 1.0.0` above + Gate 1 Fix block
2. **New API method** -> update `api:` section
3. **New target tier** (e.g., GitLive adds wasmJs) -> update `target_tiers:` + Gate 2/5 trigger logic
4. **Additional gate** (e.g., ProGuard rules) -> add Gate 6 with check/fix sections

---

## Project-Specific Keys (Hard Rule)

The library bakes in **no Firebase keys, no MP secrets, no project IDs**. Every project supplies its own. Sync gates VERIFY presence and surface UI download steps when missing — they NEVER auto-generate keys (they don't exist to generate).

| Item | Where | Committed? | Gate |
|---|---|:-:|:-:|
| `google-services.json` | `androidApp/` | ❌ gitignored | 2 |
| `GoogleService-Info.plist` | `cmp-ios/{app}/` | ❌ gitignored | 2 |
| Firebase JS config | `jsMain` init code | ❌ keys via env | 2 |
| MP API secret | `release-layer/.env` | ❌ gitignored | 5 |
| GA4 `property_id` (G-XXXXXXXX) | `PROJECT_CONFIG.yaml` | ✅ (not a secret) | 4 |
| Firebase `project_id` | `PROJECT_CONFIG.yaml` | ✅ (not a secret) | 4 |
