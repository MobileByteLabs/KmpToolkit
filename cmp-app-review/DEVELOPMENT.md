---
module: cmp-app-review
artifact: io.github.mobilebytelabs:cmp-app-review
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.appreview
api_tier: experimental
last_reviewed: 2026-09-12
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-app-review — Development

> Single source of truth for development state of `cmp-app-review` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-app-review` | `com.mobilebytelabs.kmptoolkit.app.review` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-app-review) | 2026-09-12 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-09-12. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-app-review/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | (legacy:full) | — |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-09-13 | (legacy:full) | — |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-09-13 | (legacy:full) | — |
| mingwMain | ✅ | ✅ real | 0 | 1 | 2026-09-13 | (legacy:full) | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
public interface AppReviewManager {
public class AppReviewManagerImpl(
public class FakeAppReviewManager(
public sealed class AppReviewResult {
public object AppReview {
```

---

## §4 Spec Snapshot (authored — LLM-seeded)

**What it does.** Asks the user to review the app, from `commonMain`, on 21 targets. One call routes
to the platform's native in-app review API where one exists, and to the configured store listing —
opened through `cmp-open-url` — where none does.

**Invariants**
- Never throws. Every outcome is an `AppReviewResult`; a failing native flow falls through to the
  store rather than surfacing an error, because the user's intent is satisfied either way.
- `NativeFlowRequested` means only that the request reached the OS. Neither Play nor StoreKit reports
  whether a prompt rendered or what the user did, and both silently no-op on quota. Nothing may be
  gated on it.
- The listing is resolved at CALL time, never captured at construction, so a DI graph built before
  configuration still works.
- No platform is a silent no-op. `wasmWasi` is the sole `AppReviewCapabilities.None`, and only because
  a WASI sandbox has no user, no store and no browser.

**Dependencies.** `cmp-open-url` (`api` — the fallback IS URL opening; the first cmp-to-cmp dependency
in the toolkit), `koin-core` on 20 targets via `koinMain`, `kotlinx-coroutines-core`, and
`com.google.android.play:review` on Android only.

## §5 Extension Recipes (authored — LLM-seeded)

### Recipe: consume this module (DI setup)

1. Add the dependency — `cmp-open-url` arrives with it, since the fallback is built on it.
   ```kotlin
   commonMain.dependencies { implementation(libs.cmp.app.review) }
   ```
2. Register the module where you set up DI, passing the stores you publish to. This is the only
   configuration step.
   ```kotlin
   startKoin {
       modules(
           platformModule,
           appReviewModule(
               StoreListing(
                   appStoreId = BuildKonfig.APP_STORE_ID,
                   webUrl = BuildKonfig.APP_WEB_URL,
               ),
           ),
       )
   }
   ```
3. Inject `AppReviewManager`, or call `AppReview.requestReview()` from shared code. Both read the
   same listing.
4. Android needs nothing further: a `ContentProvider` captures the application context and an
   `ActivityLifecycleCallbacks` tracks the Activity Play's flow launches into.

### Recipe: add a new platform actual

1. Decide which of the three shapes the target has, and put the actual in the matching source set —
   do NOT create a new one if an existing set already describes the behaviour:
   - native review API → its own set (`iosMain`, `macosMain`, `androidMain`)
   - no API but a store/browser → `appleStoreOnlyMain` (tvOS/watchOS) or `webFallbackMain`
     (JVM/Linux/JS/wasmJs), or its own set if the URL scheme differs (`mingwMain`)
   - no surface at all → `wasmWasiMain`
2. Implement all three actuals: `platformAppReviewCapabilities`, `requestNativeReview()`,
   `resolveStoreUrl()`.
3. `requestNativeReview()` returns `AppReviewResult.Failed` where no API exists — that is what makes
   `AppReviewManagerImpl` fall through to the store listing. Never throw.
4. Declare the target in `build.gradle.kts` and wire it into the intermediate source set if it shares
   one.
5. Run `./gradlew :cmp-app-review:assemble` — a missing actual fails there, not at publish.

### Recipe: extend the public API

1. Add to `commonMain`; keep platform knowledge behind the existing `expect` functions.
2. `./gradlew :cmp-app-review:apiDump` and review the baseline diff — removals are breaking.
3. Extend `FakeAppReviewManager` in step, or a consumer cannot test against the new surface.

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-app-review` — refresh via `gh pr list --label cmp-app-review` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-09-12 -->

### Pattern: _Pattern name TBD_

**When to use:** _TBD_
**Code shape:**
```kotlin
// TBD
```

---

## §8 Related

- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — **single source of truth** for which KMP targets
  this module must ship (21 headless / 7 Compose) and how to handle a dependency that blocks one.
  Upstream reference: <https://kotlinlang.org/docs/native-target-support.html>.

- [CONSUMPTION.md](CONSUMPTION.md) — consumer integration guide (DI setup, store ids, migration
  from a hand-written review wrapper).

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) + [RULE-LIB-OBSERVABILITY-SURFACE-001](../../../../../../layers/framework/rules/RULE-LIB-OBSERVABILITY-SURFACE-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d). Wired 2026-09-13, when `cmp-observe`
reached all 21 targets and every module could depend on it from `commonMain`.

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0 (Crashlytics attribution) | enabled | `custom_key: library:cmp-app-review@<version>` — set by `FirebaseCrashlyticsAttributionHook` (`cmp-observe-firebase`) |
| T1 (init + version health) | n/a | not applicable — this module has no initialisation step of its own |
| T2 (lifecycle events) | enabled | `review_requested`, `store_listing_opened` |
| T3 (performance traces) | opted-out | opt-in per consumer; `FirebasePerformanceHook` wraps init |
| T4 (full API usage) | opted-out | opt-in per consumer + per end-user; iOS ATT prompt required |

**Payload policy.** Events carry operation *shape*, never operation *content*. Enforced by review,
and by `.github/scripts/assert-observability-wired.sh` refusing a module that generates
`CmpMetadata` but never reports.

```yaml
# DEVELOPMENT_OBSERVABILITY.schema.yaml-conformant block
tiers:
  T0: enabled
  T1: not-applicable
  T2: enabled
  T3: opted-out
  T4: opted-out
event_schema:
  reports_init: false
  lifecycle_events:
    - review_requested
    - store_listing_opened
```
