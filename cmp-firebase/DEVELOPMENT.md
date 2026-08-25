---
module: cmp-firebase
artifact: io.github.mobilebytelabs:cmp-firebase
version: UNKNOWN
package: io.github.mobilebytelabs.kmptoolkit.firebase
api_tier: experimental
last_reviewed: 2026-08-25
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-firebase — Development

> Single source of truth for development state of `cmp-firebase` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-firebase` | `io.github.mobilebytelabs.kmptoolkit.firebase` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** Unified Firebase for Kotlin Multiplatform — **Analytics + Crashlytics** behind one in-library setup surface (`FirebaseKit`). The library spans **15 targets** (jvm · android · iosX64/Arm64/SimulatorArm64 · macosX64/Arm64 · tvosX64/Arm64/SimulatorArm64 · linuxX64/Arm64 · mingwX64 · js · wasmJs). Analytics uses GitLive on **10 targets** (android, ios×3, macos×2, tvos×3, js) with a Measurement-Protocol HTTP fallback on the remaining **5** (jvm, linux×2, mingw, wasmJs). Crashlytics uses GitLive on its **6 supported targets** (android, ios×3, macos×2) with a structured `LoggingCrashReporter` fallback on the other **9** (jvm, js, tvos×3, linux×2, mingw, wasmJs). Every crash tier produces the same `CrashReport` — exception class, message, full cause chain, and `file:line` stack frames — and mirrors a parallel `app_crash` GA4 event so both tiers share a single crash view in Analytics. Android needs zero setup code (auto-init `ContentProvider`); other platforms call `FirebaseKit.initialize()` once. Renamed from `cmp-firebase-analytics` on 2026-08-10. watchOS is explicitly **not** supported: the upstream `cmp-network-monitor` dependency publishes no watchOS artifact.

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 1 | 2026-08-25 | (legacy:full) | firebaseMain analytics + crashlyticsFirebaseMain |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-08-25 | (legacy:full) | firebaseMain analytics + crashlyticsFirebaseMain |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-08-25 | (legacy:full) | firebaseMain analytics + crashlyticsFirebaseMain |
| jvmMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | nonFirebaseMain analytics (MP) + crashlyticsFallbackMain |
| jsMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | firebaseMain analytics + crashlyticsFallbackMain |
| wasmJsMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | nonFirebaseMain analytics (MP) + crashlyticsFallbackMain |
| mingwMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | nonFirebaseMain analytics (MP) + crashlyticsFallbackMain |
| linuxMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | nonFirebaseMain analytics (MP) + crashlyticsFallbackMain |
| tvosMain | ✅ | 🟡 partial | 0 | 1 | 2026-08-25 | partial | firebaseMain analytics + crashlyticsFallbackMain |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV jvm baseline: api/jvm/cmp-firebase.api — regenerate via ./gradlew :cmp-firebase:apiDump, verify via :cmp-firebase:apiCheck -->

```kotlin
// ── Setup ──────────────────────────────────────────────────────────────────

object FirebaseKit {
    fun initialize()                          // non-Android: call once at startup
    fun initialize(config: FirebaseConfig)    // overload accepting full config
    val crashReporter: CrashReporter
    fun installUncaughtHandler()              // real on jvm/android; no-op elsewhere
}

class FirebaseConfig {                        // immutable; build via Builder
    class Builder {
        fun options(options: FirebaseOptions): Builder
        fun mpConfig(config: MpConfig): Builder
        fun build(): FirebaseConfig
    }
}

class FirebaseOptions                         // app-id, project-id, api-key, …
class MpConfig                                // GA4 Measurement Protocol: apiSecret, measurementId

// ── Analytics ──────────────────────────────────────────────────────────────

interface AnalyticsHelper {
    fun logEvent(event: AnalyticsEvent)
}

class FirebaseAnalyticsHelper : AnalyticsHelper           // GitLive — firebaseMain targets
class MeasurementProtocolAnalyticsHelper : AnalyticsHelper // HTTP GA4 MP — nonFirebaseMain targets
object NoOpAnalyticsHelper : AnalyticsHelper              // silent; used in tests/previews
class StubAnalyticsHelper : AnalyticsHelper               // records events; inspectable in tests
class TestAnalyticsHelper : AnalyticsHelper               // alias of StubAnalyticsHelper family

/** Memoized process-singleton — consent + client_id managed internally. */
fun provideAnalyticsHelper(): AnalyticsHelper

object AnalyticsModule                                    // Koin module with provideAnalyticsHelper()

object EventTypes {
    const val APP_CRASH: String
    // … additional standard event name constants
}

object ParamKeys {
    const val EXCEPTION_TYPE: String
    const val FATAL: String
    // … additional standard param key constants
}

// ── Crash Reporting ────────────────────────────────────────────────────────

interface CrashReporter {
    fun recordException(t: Throwable, fatal: Boolean)
    fun recording(fatal: Boolean): CoroutineScope        // structured recording scope
    fun asCoroutineExceptionHandler(fatal: Boolean): CoroutineExceptionHandler
}

class FirebaseCrashReporter : CrashReporter              // GitLive Crashlytics — crashlyticsFirebaseMain
class LoggingCrashReporter : CrashReporter               // JSON log via Kermit — crashlyticsFallbackMain
object NoOpCrashReporter : CrashReporter                 // silent no-op

object CrashReporterModule                               // Koin module with provideCrashReporter()

// ── Expects (one actual per source-set tier) ───────────────────────────────

// Analytics split: firebaseMain / nonFirebaseMain
expect fun platformInitializeFirebase(options: FirebaseOptions?)    // PlatformInit.kt
internal expect fun createPlatformAnalyticsHelper(): AnalyticsHelper // AnalyticsProvider.kt

// Crash split: crashlyticsFirebaseMain / crashlyticsFallbackMain
expect fun provideCrashReporter(): CrashReporter          // CrashReporter.kt

// Platform identity (one actual per leaf target)
expect val kmpPlatform: Platform                          // Platform.kt

// Uncaught handler: real on jvm+android, no-op in nativeMain/jsMain/wasmJsMain
internal expect fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit) // UncaughtHandler.kt

// ── Internal bridge ────────────────────────────────────────────────────────

/** Mirrors every crash as a GA4 `app_crash` event so both crash tiers share one Analytics view. */
internal object CrashAnalyticsBridge {
    fun logCrash(t: Throwable, fatal: Boolean)            // → EventTypes.APP_CRASH
}
```

---

## §4 Spec Snapshot (authored — LLM-seeded)

<!-- Authored 2026-08-25 from verified source ground truth -->

**Problem this module solves:** KMP projects that use Firebase Analytics and Crashlytics must otherwise configure separate GitLive dependencies per platform, stub out unsupported targets manually, and accept that JVM/Linux/Windows/WasmJS builds simply fail or drop events silently. `cmp-firebase` solves this by (a) hiding all `expect`/`actual` wiring behind a single `commonMain` API surface, (b) providing a Measurement-Protocol HTTP fallback so every one of the 15 declared targets can log analytics events, and (c) mirroring every crash as a GA4 `app_crash` event so crash data appears in Analytics even on targets where Firebase Crashlytics has no native SDK.

**Core invariants:**

1. **Consumer API is 100% commonMain.** No consumer code touches a GitLive or platform type directly. All Firebase interaction flows through `expect`/`actual` bridged by `firebaseMain`/`nonFirebaseMain` (analytics) and `crashlyticsFirebaseMain`/`crashlyticsFallbackMain` (crash).
2. **Every target compiles and degrades gracefully.** The fallback tiers (`MeasurementProtocolAnalyticsHelper`, `LoggingCrashReporter`) never throw; they always produce structured output so the host app continues running.
3. **Crash→GA4 single view via `app_crash` mirror.** `CrashAnalyticsBridge.logCrash` fires an `EventTypes.APP_CRASH` GA4 event on every crash tier, so dashboards show unified crash counts regardless of the platform's crashlytics capability.
4. **`provideAnalyticsHelper()` is a shared process singleton.** Consent state and the `client_id` (loaded-or-created from `Settings`) are initialized once; all callers receive the same instance.
5. **Measurement Protocol payload uses GA4 snake_case.** The MP tier serializes `client_id`, `user_id`, and `user_properties` in GA4-canonical form so backend ingestion treats MP events identically to SDK events.
6. **API evolves additively (BCV enforced).** New functionality is delivered via overloads or secondary constructors. Adding a default parameter to an existing public function signature is forbidden — it is a binary-incompatible change under BCV. Every API surface change requires running `./gradlew :cmp-firebase:apiDump` and committing the updated `api/jvm/cmp-firebase.api` baseline.

**Out of scope (by design):**

- **watchOS.** The upstream `cmp-network-monitor` dependency publishes no watchOS artifact; adding watchOS support would require either removing that dependency or forking it. Not planned.
- **Firebase Remote Config / FCM / Auth.** This module covers Analytics + Crashlytics only. Other Firebase products belong in separate modules.
- **Direct GoogleServices JSON parsing.** `FirebaseOptions` is populated by the consumer (typically from `BuildConfig` or `/secrets pull` output); the module does not read `google-services.json` at runtime.
- **Crash symbolication upload.** dSYM/ProGuard mapping upload is a CI/CD concern handled by the Fastlane lane, not this library.

---

## §5 Extension Recipes (authored — LLM-seeded)

<!-- Authored 2026-08-25 from verified source ground truth -->

### Recipe: Add a platform actual for a new target

Use this when adding a Kotlin/Native or JS target that does not yet have an actual file for one or more expects.

1. **Identify which intermediate source set the new target will join.** Each expect is satisfied by one of the two-tier intermediate sets:
   - Analytics: `firebaseMain` (GitLive available) or `nonFirebaseMain` (MP fallback).
   - Crash: `crashlyticsFirebaseMain` (GitLive Crashlytics) or `crashlyticsFallbackMain` (logging fallback).
   - Uncaught handler: `nativeMain` (no-op actual) covers all Kotlin/Native leaves; `jsMain` / `wasmJsMain` carry their own no-op actuals.
   If the new target is a Kotlin/Native Apple target with GitLive support, add it to `firebaseMain` + `crashlyticsFirebaseMain`. Otherwise, add it to the `nonFirebaseMain` + `crashlyticsFallbackMain` tiers.
2. **Declare the target in `build.gradle.kts`.** Add the target name under `kotlin { }`. The intermediate source set hierarchy is already wired; the new target's `<target>Main` source set will automatically `dependsOn` the correct intermediate.
3. **Provide a `Platform.kt` actual** in `<targetName>Main/kotlin/.../Platform.kt` implementing `actual val kmpPlatform: Platform`.
4. **Verify compilation.** Run `./gradlew :cmp-firebase:compileKotlin<TargetName>` (e.g. `:compileKotlinTvosArm64`). No new actual files are needed for the expect functions if the target joins an existing intermediate tier — those actuals live in the intermediate source set, not the leaf.
5. **Confirm the BCV baseline is unchanged** with `./gradlew :cmp-firebase:apiCheck`. Platform actuals are internal; the jvm BCV baseline should not change.

### Recipe: Extend the public API additively and regenerate the BCV baseline

Use this when adding a new public function, overload, or secondary constructor.

1. **Add the new declaration in `commonMain`.** Never add a default parameter to an existing public function — that breaks binary compatibility. Use an overload (same name, different parameter list) or a secondary constructor instead.
2. **Implement platform actuals if the new declaration is an `expect`.** Follow the source-set tier mapping from Recipe 1.
3. **Regenerate the BCV baseline.**
   ```bash
   ./gradlew :cmp-firebase:apiDump
   ```
   This rewrites `api/jvm/cmp-firebase.api`. Review the diff: only additions should appear (no removals or signature changes from existing entries).
4. **Commit the updated `.api` file alongside the source change.** CI runs `./gradlew :cmp-firebase:apiCheck` and will fail if the baseline is stale.
5. **Run the full compile matrix to confirm no target is broken.**
   ```bash
   ./gradlew :cmp-firebase:compileKotlinJvm \
             :cmp-firebase:compileKotlinJs \
             :cmp-firebase:compileKotlinWasmJs \
             :cmp-firebase:compileKotlinMacosArm64 \
             :cmp-firebase:compileKotlinMetadata \
             :cmp-firebase:compileAndroidMain \
             :cmp-firebase:compileKotlinLinuxX64
   ```

### Recipe: Add a new variant under an existing Apple platform (e.g. a new tvOS sub-target)

Use this when a new simulator or architecture slice is needed for an already-modeled Apple platform family.

1. **Check whether the variant is already covered.** `tvosMain` already covers `tvosX64`, `tvosArm64`, and `tvosSimulatorArm64`. Adding a fourth `tvos*` slice means the target must be declared by the GitLive Firebase KMP library itself — verify the GitLive release notes first. If GitLive does not publish an artifact for the new slice, it cannot join `firebaseMain`; it must go into `crashlyticsFallbackMain` + `nonFirebaseMain` for analytics.
2. **Add the target to `build.gradle.kts`** under `kotlin { tvos { } }` or by explicitly naming it (e.g. `tvosSimulatorArm64()`). Gradle will wire it under the existing `tvosMain` intermediate source set automatically — no new intermediate source set or actual files are required.
3. **Confirm the `apple()` / `tvos()` shorthand includes the new slice** by checking that `tvosMain` is listed as a `dependsOn` parent for the new `<target>Main` in the configuration output (`./gradlew :cmp-firebase:outgoingVariants` or read the Gradle build report).
4. **Compile and verify** with `./gradlew :cmp-firebase:compileKotlin<NewTargetName>`.

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-firebase` — refresh via `gh pr list --label cmp-firebase` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

<!-- Authored 2026-08-25 from verified source ground truth -->

### Pattern: Two-tier real + fallback expect/actual (the analytics and crash split)

**When to use:** When a KMP library wraps a native SDK that only exists on a subset of targets. The two-tier pattern lets every declared target compile and produce real output, while avoiding the fragility of a single `commonMain` no-op that silently does nothing everywhere.

**How it works in `cmp-firebase`:**

The source-set graph for analytics has two orthogonal intermediate nodes, each `dependsOn commonMain`:

```
commonMain
├── firebaseMain          ← android, ios×3, macos×2, tvos×3, js
│     actual fun platformInitializeFirebase(...)  // GitLive Firebase.initialize()
│     internal actual fun createPlatformAnalyticsHelper() = FirebaseAnalyticsHelper(...)
└── nonFirebaseMain       ← jvm, linux×2, mingw, wasmJs
      actual fun platformInitializeFirebase(...)  // no-op (no SDK)
      internal actual fun createPlatformAnalyticsHelper() = MeasurementProtocolAnalyticsHelper(...)
```

The crash split follows the same shape:

```
commonMain
├── crashlyticsFirebaseMain   ← android, ios×3, macos×2
│     actual fun provideCrashReporter() = FirebaseCrashReporter()
└── crashlyticsFallbackMain   ← jvm, js, tvos×3, linux×2, mingw, wasmJs
      actual fun provideCrashReporter() = LoggingCrashReporter()
```

**Key implementation rules:**

- The `expect` declarations live in `commonMain` — consumers never see the split.
- Each intermediate source set owns exactly one `actual` per `expect` that belongs to its domain. Leaf targets (`androidMain`, `jvmMain`, etc.) carry only `Platform.kt` (the `kmpPlatform` actual) and no other actuals.
- The fallback tier (`nonFirebaseMain`, `crashlyticsFallbackMain`) must never throw. `LoggingCrashReporter` serializes the `CrashReport` as JSON via Kermit; `MeasurementProtocolAnalyticsHelper` sends an HTTP POST to the GA4 MP endpoint and silently swallows network errors.
- Both tiers share the same `CrashAnalyticsBridge.logCrash` call path, ensuring every crash — regardless of which reporter tier fires — produces an `app_crash` GA4 event.

### Pattern: Platform uncaught-handler — real on JVM/Android, no-op elsewhere

**When to use:** When you need to install a process-wide uncaught exception hook but the hook primitive only exists on JVM-family targets.

**How it works in `cmp-firebase`:**

```kotlin
// commonMain — FirebaseKit.installUncaughtHandler() delegates to:
internal expect fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit)

// jvmMain / androidMain — real implementation
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        onUncaught(throwable)
        previous?.uncaughtException(thread, throwable)
    }
}

// nativeMain — no-op (Kotlin/Native has no Thread.setDefaultUncaughtExceptionHandler)
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit) {
    // no-op: uncaught exceptions on Kotlin/Native are handled by the runtime
}

// jsMain / wasmJsMain — no-op
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit) {
    // no-op: JS/WasmJS process-level uncaught hook requires window.onerror wiring
    //        which is outside the scope of this library
}
```

**Why `nativeMain` rather than per-leaf actuals:** All Kotlin/Native leaves share the same behaviour (no-op), so a single actual in the `nativeMain` intermediate source set is sufficient. Only jvm and android need distinct real implementations; the rest are covered by the native and JS intermediate tiers.

---

## §8 Related

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |
