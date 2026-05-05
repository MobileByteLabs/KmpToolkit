# cmp-firebase-analytics — Claude AI Setup Guide

`/sync-firebase-analytics` is a verify-gated sync that wires `cmp-firebase-analytics` into a consuming KMP app. Run from anywhere in the project; Claude detects the active project and walks the gates.

## Quick Commands

```bash
/sync-firebase-analytics           # Full sync — Gradle + Firebase config + DI + (optional) MP secret
/sync-firebase-analytics --check   # Dry run — show status, no writes
/lib-sync cmp-firebase-analytics   # Same as /sync-firebase-analytics
```

## What the Skill Does

```
/sync-firebase-analytics
        |
        v
+------------------------------------------------------------+
|  GATE 1: Gradle Dependency                                 |
|  Check: cmp-firebase-analytics:VERSION in libs.versions    |
|  Check: used in commonMain.dependencies                    |
|  Fix:   Auto-insert correct entries                        |
|  Result: PASS / FIXED / BLOCKED                            |
+----------------------------+-------------------------------+
                             | PASS
                             v
+------------------------------------------------------------+
|  GATE 2: Firebase Config Files (per platform)              |
|  Check: androidApp/google-services.json (Android target)   |
|  Check: cmp-ios/{app}/GoogleService-Info.plist (Apple)     |
|  Check: jsMain init (JS target)                            |
|  Fix:   Surface manual download steps if missing.          |
|         Library NEVER auto-creates these — they contain    |
|         your Firebase project's API keys.                  |
|  Result: PASS / MISSING (with remediation) / SKIPPED       |
+----------------------------+-------------------------------+
                             | PASS
                             v
+------------------------------------------------------------+
|  GATE 3: DI Wiring (Koin)                                  |
|  Check: AnalyticsModule import in app's KoinModules.kt     |
|  Check: AnalyticsHelper bound in module {}                 |
|  Fix:   Auto-insert AnalyticsModule + Mode.Firebase wiring |
|  Result: PASS / FIXED / SKIPPED (no Koin detected)         |
+----------------------------+-------------------------------+
                             | PASS
                             v
+------------------------------------------------------------+
|  GATE 4: PROJECT_CONFIG.analytics block (optional)         |
|  Check: idea-layer/PROJECT_CONFIG.yaml has analytics block |
|  Fix:   Auto-scaffold from template (property_id required) |
|  Result: PASS / FIXED / SKIPPED                            |
+----------------------------+-------------------------------+
                             | PASS
                             v
+------------------------------------------------------------+
|  GATE 5: MP API Secret (conditional)                       |
|  Trigger: app targets watchOS×4 / Linux×2 / mingwX64 /    |
|           wasmJs / wasmWasi (any of the 10 nonFirebase)    |
|  Check: release-layer/.env contains MP_API_SECRET          |
|  Check: PROJECT_CONFIG.analytics.measurement_protocol.     |
|         api_secret_secret_ref is set                       |
|  Fix:   Surface UI step (Firebase Console → MP Secrets);   |
|         prompt for value; write to .env                    |
|  Result: PASS / FIXED / NOT_NEEDED                         |
+------------------------------------------------------------+
                             |
                             v
                       SYNC COMPLETE
```

## State Summary Output

```
+======================================================================+
|  /sync-firebase-analytics — COMPLETE                                  |
+======================================================================+
|  GATE 1   Gradle           [OK]  cmp-firebase-analytics:1.0.0        |
|  GATE 2   Firebase config  [OK]  google-services.json + Plist + JS   |
|  GATE 3   DI wiring        [OK]  AnalyticsHelper bound in Koin       |
|  GATE 4   PROJECT_CONFIG   [OK]  analytics block present             |
|  GATE 5   MP secret        N/A   no nonFirebase platforms targeted   |
+----------------------------------------------------------------------+
|  Targets covered: 21/21                                              |
|    firebaseMain (11): Android, JVM, iOS x3, macOS x2, tvOS x3, JS    |
|    nonFirebaseMain (10): N/A — not targeted by this app              |
+----------------------------------------------------------------------+
|  Docs: docs/firebase-analytics/SETUP.md                              |
|  Next: /idea analytics --setup  (framework: enable BigQuery readback)|
+======================================================================+
```

## Team Scenarios

### First-time integration on a fresh project

```bash
/sync-firebase-analytics
# Gate 1 adds dependency to libs.versions.toml + build.gradle.kts
# Gate 2 detects missing google-services.json — surfaces UI step
#        ("Download from Firebase Console → Project Settings → Your apps")
# User downloads the file, drops it in androidApp/, re-runs:
/sync-firebase-analytics
# Gate 2 PASS, Gate 3 wires Koin, Gate 4 scaffolds PROJECT_CONFIG, done.
```

### Version bump (cmp-firebase-analytics 1.0.0 → 1.1.0)

```bash
/sync-firebase-analytics
# Gate 1 detects old version, updates libs.versions.toml + verifies still used
# Other gates pass through (already wired).
```

### Adding watchOS to an existing iOS app

```bash
/sync-firebase-analytics
# Gates 1-4 already pass.
# Gate 5 detects new watchOS targets in build.gradle.kts → triggers MP setup.
# Surfaces the full step-by-step (see "How to generate an MP API secret" below).
# User pastes the secret; Gate 5 writes to release-layer/.env and updates PROJECT_CONFIG.
```

## How to generate an MP API secret

Required when Gate 5 fires (any of watchOS / Linux / Windows / wasm targets in your `build.gradle.kts`). Skip this entirely if your app only targets Android/iOS/macOS/tvOS/JVM/JS — those use native GitLive SDK auth.

### What it is

A short string token (typically 22 chars, looks like `abc1d2e3f4-XYZ_a8B7c6D5e4F3g2H`) that authorizes HTTP POSTs to Google's Measurement Protocol endpoint. It is the **least-privileged** Firebase credential — can only write events to one specific GA4 data stream, can't read anything, can't admin.

### Step-by-step (Firebase Console UI)

1. Open **[Firebase Console](https://console.firebase.google.com/)** → click your project
2. Click the **gear icon** (top-left, next to "Project Overview") → **Project Settings**
3. Click the **Integrations** tab
4. Find the **Google Analytics** card → click **Manage** (or **Open in GA4**)
5. In the GA4 admin pane, navigate: **Admin → Data Streams**
6. Click your data stream — pick the one matching the platform tier you're enabling MP for:
   - **Web stream** → for `wasmJs` / browser deploys
   - **iOS stream** → for `watchOS` (uses the iOS app's stream)
   - For Linux/Windows native, use whichever stream represents your "desktop" presence (often Web)
7. Scroll to **Measurement Protocol API secrets** (bottom of the stream details page)
8. Click **Create**
9. Give it a descriptive name (e.g., `watchos-prod`, `linux-staging`)
10. **Copy the secret value immediately — it is shown ONCE.** If you lose it, create a new one.

### Where the secret goes

Paste it when `/sync-firebase-analytics` Gate 5 prompts you, OR add manually:

```bash
# release-layer/.env (gitignored — verify it's in your .gitignore)
MP_API_SECRET=abc1d2e3f4-XYZ_a8B7c6D5e4F3g2H
```

```yaml
# idea-layer/PROJECT_CONFIG.yaml
analytics:
  envs:
    prod:
      property_id: G-XXXXXXXX
      measurement_protocol:
        api_secret_secret_ref: MP_API_SECRET   # env-var name; resolved at runtime
```

### Secret hygiene (Hard Rules)

| Rule | Why |
|---|---|
| Never commit the value | A leaked secret lets anyone write fake events to your GA4 stream (skews your funnel/retention metrics) |
| Never log the value | App logs leak to crash reporters, third-party SDKs, dev consoles |
| Rotate on suspected leak | Firebase Console → revoke old → create new → redeploy app |
| One per environment | Separate secrets for prod / staging / dev; don't share across |
| One per project | Each Firebase project has its own; the `mood-movies` secret won't work for `reels-downloader` |
| Load from CI vault | GitHub Actions secrets, etc. Never hard-code in `build.gradle.kts` |

### How MP secret differs from other Firebase credentials

| Credential | Privilege | Used by |
|---|---|---|
| `google-services.json` / `GoogleService-Info.plist` | Native SDK auto-config | Android / iOS / macOS / tvOS native SDK |
| Firebase Web Config | Native SDK auto-config | Firebase JS SDK |
| Firebase Service Account JSON | High — signs JWTs for any Firebase API | Server-side (Crashlytics fetch, BigQuery, etc.) |
| **MP API secret** | **Lowest — write-only to one GA4 stream** | HTTP fallback for `MeasurementProtocolAnalyticsHelper` only |

### Dry run

```bash
/sync-firebase-analytics --check
# Shows current sync status across all 5 gates without modifying any files.
# Use to audit: PR review, CI pre-merge check, debugging "why aren't events flowing"
```

## Project-Specific Keys (Hard Rule)

The library bakes in **zero defaults** for project-specific config. Every project supplies its own:

| Item | Where it lives | Committed? |
|---|---|:-:|
| `google-services.json` | `androidApp/google-services.json` | ❌ gitignored |
| `GoogleService-Info.plist` | `cmp-ios/{app}/GoogleService-Info.plist` | ❌ gitignored |
| Firebase JS config | `jsMain` init code (loaded from env at build time) | ❌ keys via env |
| MP API secret | `release-layer/.env` → `MP_API_SECRET=...` | ❌ gitignored |
| GA4 `property_id` (G-XXXXXXXX) | `idea-layer/PROJECT_CONFIG.yaml` `analytics.envs.{env}.property_id` | ✅ (not a secret) |

`/sync-firebase-analytics` Gate 2 + Gate 5 verify presence and surface manual steps when missing — they NEVER auto-generate keys (they don't exist to generate).

## Library API Quick Reference

### Public types

```kotlin
io.github.mobilebytelabs.kmptoolkit.analytics.*
├── AnalyticsHelper                    interface — logEvent, logScreenView, logError, ...
├── AnalyticsEvent / Param             type-safe event + param data classes
├── EventTypes / ParamKeys             standard string constants
├── StubAnalyticsHelper                dev: logs to Kermit
├── NoOpAnalyticsHelper                release/test: discards events
├── TestAnalyticsHelper                tests: captures events for assertions
├── EventValidator                     taxonomy regex + PII regex (debug builds)
├── PerformanceTracker                 start/stop/measure block timer
├── EventRegistry                      declared-event enforcement (opt-in)
├── provideAnalyticsHelper()           expect/actual factory (firebaseMain → Firebase, else NoOp)
└── di.AnalyticsModule.Mode            { Firebase | Stub | NoOp }

io.github.mobilebytelabs.kmptoolkit.analytics.firebase.*
└── FirebaseAnalyticsHelper            firebaseMain only — wraps GitLive

io.github.mobilebytelabs.kmptoolkit.analytics.mp.*
├── MeasurementProtocolAnalyticsHelper commonMain — for nonFirebase platforms
├── MpConfig                           data class(measurementId, apiSecret, endpoint?)
└── ClientId                           internal — multiplatform-settings backed
```

### Convenience methods

```kotlin
analytics.logScreenView(screenName, sourceScreen?)
analytics.logButtonClick(buttonName, screenName?)
analytics.logError(errorMessage, errorCode?, screen?)
analytics.logStateTransition(screenName, from, to)
analytics.logFeatureUsed(featureName, screen?)
analytics.setUserProperty(name, value)
analytics.setUserId(hashedUserId)
```

### Builder DSL

```kotlin
analytics.log(EventTypes.FORM_COMPLETED) {
    param(ParamKeys.FORM_NAME, "registration")
    param(ParamKeys.COMPLETION_TIME, 45)        // numbers
    param("custom_metric", true)                // booleans
}
```

## Sister Commands (claude-product-cycle framework)

- `/idea analytics` — full growth pipeline (capture → BigQuery fetch → Claude analysis → ranked plan → optional codegen). See [IDEA_ANALYTICS_GUIDE.md](../../../../../../../docs/guides/IDEA_ANALYTICS_GUIDE.md) in the framework.
- `/idea firebase-crash` — same auth pattern, parallel command for Crashlytics.

## Docs

- [README](README.md) — Module overview
- [SETUP](SETUP.md) — Manual integration guide
- [CLAUDE_AI_SETUP](CLAUDE_AI_SETUP.md) — This file
