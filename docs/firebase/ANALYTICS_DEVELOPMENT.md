# cmp-firebase — Analytics Engine (Developer Guide)

For contributors **extending** the analytics engine — architecture, the opt-out contract every
tracker must honour, how to add a backend or a tracker, the Compose companion, and testing. For
*using* the engine, see [ANALYTICS.md](ANALYTICS.md). Module dev-state SoT (parity matrix, versions)
lives in [`cmp-firebase/DEVELOPMENT.md`](../../cmp-firebase/DEVELOPMENT.md) per
RULE-LIB-DEVELOPMENT-MD-001.

---

## Architecture at a glance

Two modules, one interface:

```
cmp-firebase                     — the engine (Compose-free, 19 KMP targets)
  analytics/
    AnalyticsHelper              — the single interface everything is written against
    AnalyticsEvent / Param       — validated event + param value types
    EventTypes / ParamKeys       — shared constants (cross-app consistency)
    AnalyticsConfig              — capability flags + autoEnabled() gate helper
    di/AnalyticsModule           — factory: Mode.Firebase | Stub | NoOp  (+ performanceTracker)
    Firebase/NoOp/Stub/Test helpers
    mp/                          — MeasurementProtocolAnalyticsHelper (HTTP fallback tier)
    PerformanceTracker           — P50/P95/P99, fast/slow/very_slow tagging
    AppLifecycleTracker          — app_launch cold-start + fg/bg
    MemoryTracker                — provider-based memory sampling
    OfflineEventQueue            — AnalyticsHelper decorator; buffers offline, flushes on reconnect
    Funnel / EventCatalog        — conversion funnels + typed-event base (events live in the app)
    AnalyticsExtensions          — batch(), startTiming(), event builder
    net/                         — analyticsTelemetryPlugin (Ktor) + attachNetworkTelemetry

cmp-firebase-compose             — Compose auto-tracking companion (~9 Compose targets)
  compose/
    AnalyticsCompose             — LocalAnalyticsHelper, TrackScreenView, TrackComposableLifecycle, Modifier.trackClick
    NavAnalytics                 — NavController.trackScreenViews()  → screen_view + screen_transition
```

**Why two modules:** the engine stays Compose-free so non-Compose consumers (services, CLIs, the 19
targets) never pull Compose. Compose reaches fewer targets, so `cmp-firebase-compose` is the only
place that depends on `compose.*` + `navigation-compose`. It `api`-depends on `cmp-firebase`.

**Design rule — the library ships patterns, never a consumer's domain events.** `EventCatalog` /
`Funnel` are *builders*; concrete event catalogs (e.g. a `MifosAnalyticsEvents`) live in the
consumer app, not in this library.

---

## The opt-out contract (the invariant every tracker must satisfy)

There is exactly **one** master switch — `AnalyticsHelper.setCollectionEnabled(Boolean)` — and every
automatic emitter must consult it (directly, or via an `AnalyticsConfig` flag) **before** emitting.

- `AnalyticsConfig.autoEnabled(capability, collectionEnabled)` returns `collectionEnabled && capability`.
- Trackers that take an `enabled: () -> Boolean` gate (`analyticsTelemetryPlugin`,
  `attachNetworkTelemetry`) must be passed `{ config.autoNetworkTelemetry }` (or equivalent) by the
  wiring so the master switch composes.
- `TestAnalyticsHelper.logEvent` early-returns when collection is off — the test-double models the
  same cascade, which is what the `optout_cascade_suppresses_auto_trackers` test asserts.

**When you add an emitter, you MUST wire it into this cascade.** A tracker that emits while
`setCollectionEnabled(false)` is a correctness bug and will fail the opt-out test.

---

## How to: add a new backend

1. Implement `AnalyticsHelper` (override `logEvent(AnalyticsEvent)` at minimum; override
   `setCollectionEnabled` / `setConsent` / `setUserId` / `setUserProperty` where the backend
   supports them).
2. If it's platform-specific, provide it through the `expect fun provideAnalyticsHelper()` seam in
   the relevant source set (mirror `FirebaseAnalyticsHelper` in `firebaseMain`).
3. Add a `Mode` entry in `di/AnalyticsModule` **only if** it's a first-class choice for consumers;
   otherwise consumers can pass their own instance.
4. Honour the opt-out contract (respect `setCollectionEnabled`).

## How to: add a new auto-tracker

1. Take `AnalyticsHelper` (or decorate it, like `OfflineEventQueue`) in the constructor.
2. Add a `Boolean` flag to `AnalyticsConfig` (default `true`) and gate emission on
   `config.autoEnabled(flag, collectionEnabled)` — or accept an `enabled: () -> Boolean`.
3. Emit through `helper.logEvent(...)` with `EventTypes` / `ParamKeys` constants (add new constants
   there rather than inline strings).
4. Add a test to `AnalyticsEngineTest` proving (a) it emits when on, (b) it's silent when the master
   switch is off.
5. Document it in [ANALYTICS.md](ANALYTICS.md) §Event reference.

## How to: extend the Compose companion

Everything reads the helper from `LocalAnalyticsHelper` (default `NoOpAnalyticsHelper`). New
composables should `rememberAnalyticsHelper()` rather than take a helper param, so previews/tests
stay silent by default. Keep the companion Compose-only — engine logic belongs in `cmp-firebase`.

---

## Testing

- Unit tests: `cmp-firebase/src/commonTest/.../analytics/AnalyticsEngineTest.kt` (opt-out cascade,
  percentile math, offline-queue buffer/flush) + `AnalyticsCollectionTest.kt`.
- Offline-queue tests use `FakeNetworkMonitor().setOnline(false/true)` from
  `cmp-network-monitor`'s `testing/` package (available via the `api` dep) with
  `runTest` / `runCurrent` / `backgroundScope`.
- Run: `./gradlew :cmp-firebase:jvmTest` (fast) — `Mode.Firebase` is unavailable off-device (GitLive
  JVM analytics is a stub), so tests use `TestAnalyticsHelper` / `Mode.NoOp`.

## API compatibility (BCV)

Both modules are under Binary Compatibility Validator. **Any public API change requires an
`apiDump`:**

```bash
./gradlew :cmp-firebase:apiDump :cmp-firebase-compose:apiDump
./gradlew :cmp-firebase:apiCheck :cmp-firebase-compose:apiCheck   # must be green before commit
```

Baselines: `cmp-firebase/api/jvm/*.api`, `cmp-firebase-compose/api/jvm/*.api`.

## Build gotchas (from the GitLive 3.0.0-alpha02 adoption)

- Kotlin `2.4.0` / Compose `1.11.1`. GitLive has **no watchOS**, and this module also omits
  `wasmWasi` on purpose (see the comment in `cmp-firebase/build.gradle.kts`). Keep target sets
  aligned with the `cmp-network-monitor*` siblings.
  <br>*(Corrected 2026-09-03: an earlier revision claimed alpha01 "dropped `iosX64`/`macosX64`".
  That was wrong — the module declares `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`,
  `macosX64()` and `macosArm64()`, and `:cmp-firebase:linkDebugTestMacosX64` builds green.)*
- **wasmJs moved to the native Firebase tier in alpha02** — it is no longer a
  Measurement-Protocol target and now reads `FirebaseConfig.web`. Crashlytics is the one
  exception: upstream did not add `wasmjs` there, so wasmJs keeps the logging fallback.
- After changing dependencies, JS/Wasm yarn locks need **both**
  `./gradlew kotlinUpgradeYarnLock` **and** `kotlinWasmUpgradeYarnLock` (the JS one alone is
  insufficient).
- Prefer targeted module builds (`:cmp-firebase:build`) over a full-repo build (OOM-prone).

## Release

Versioned by the `kmptoolkit.version` gradle property; published to Maven Central via the vanniktech
plugin (same pipeline as every `cmp-*` module). For local consumer verification (e.g. the template
migration) use `./gradlew :cmp-firebase:publishToMavenLocal :cmp-firebase-compose:publishToMavenLocal`
and consume from `mavenLocal()`.

---

## Related

- [ANALYTICS.md](ANALYTICS.md) — consumption guide
- [SETUP.md](SETUP.md) — install + per-platform Firebase config + GA4
- [`cmp-firebase/DEVELOPMENT.md`](../../cmp-firebase/DEVELOPMENT.md) — module dev-state SoT
- Design record: [`docs/superpowers/specs/2026-08-11-gitlive-firebase-3.0.0-alpha01-adoption-design.md`](../superpowers/specs/2026-08-11-gitlive-firebase-3.0.0-alpha01-adoption-design.md)
