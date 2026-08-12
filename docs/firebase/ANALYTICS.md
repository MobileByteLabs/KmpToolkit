# cmp-firebase — Analytics Engine (Consumption Guide)

How to **use** the analytics engine from a consumer app. `cmp-firebase` ships one `AnalyticsHelper`
interface plus a full set of **default-ON, opt-out** auto-trackers; `cmp-firebase-compose` adds
declarative Compose tracking. All of it is gated by a single master switch — one call turns
everything off when the user opts out.

> New here? Read [SETUP.md](SETUP.md) first (install + per-platform Firebase config). This guide
> covers the analytics **capabilities** on top of that. Extending the engine? See
> [ANALYTICS_DEVELOPMENT.md](ANALYTICS_DEVELOPMENT.md).

---

## 1. The one thing to understand: collection is opt-in-by-default, opt-out with one call

Every automatic tracker checks the master collection switch before it emits. Flip it off and the
whole engine — including Firebase's automatic `session_start` / `user_engagement` / `first_open`
events — goes quiet, persisted across sessions.

```kotlin
val analytics: AnalyticsHelper = koinInject()

analytics.setCollectionEnabled(false)   // user opted out → nothing is collected
analytics.setCollectionEnabled(true)    // user opted back in
```

For **GDPR opt-in-required** (start OFF, enable after consent), pass an `AnalyticsConfig`:

```kotlin
val analytics = AnalyticsModule.analyticsHelper(
    AnalyticsModule.Mode.Firebase,
    AnalyticsConfig(collectionEnabledByDefault = false),   // collect nothing until consent
)
// later, when the user consents:
analytics.setCollectionEnabled(true)
```

Granular consent (Firebase **Consent Mode**):

```kotlin
analytics.setConsent(analyticsStorage = true, adStorage = false)
```

### `AnalyticsConfig` — the capability flags (all default `true`)

| Flag | Governs |
|------|---------|
| `collectionEnabledByDefault` | Master switch state at startup (`true` = opt-in-by-default) |
| `autoScreenTracking` | Compose companion `screen_view` + `screen_transition` |
| `autoAppLaunchTiming` | `AppLifecycleTracker` cold-start `app_launch` timing |
| `autoPerformanceStats` | `PerformanceTracker` P95/P99 accumulation |
| `autoNetworkTelemetry` | Ktor per-request telemetry + online/offline transitions |
| `autoCrashCapture` | Route uncaught failures to the `CrashReporter` |
| `autoOfflineQueue` | Buffer events offline, flush on reconnect |
| `slowOperationThresholdMs` | Duration (ms) at/above which an op is tagged `slow` (default `1000`) |

Each auto-tracker calls `config.autoEnabled(capability, collectionEnabled)` before emitting, so both
the per-capability flag **and** the master switch must be on. Turn one capability off without
touching the others by flipping its flag.

---

## 2. Wire the helper once (DI)

```kotlin
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule

val analyticsModule = module {
    single {
        AnalyticsConfig(/* override any flag here */)
    }
    single<AnalyticsHelper> {
        AnalyticsModule.analyticsHelper(
            mode = if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub else AnalyticsModule.Mode.Firebase,
            config = get(),
        )
    }
}
```

| `Mode` | Backend |
|--------|---------|
| `Firebase` | Production — GitLive Firebase where supported, Measurement-Protocol HTTP elsewhere, NoOp on unsupported |
| `Stub` | Development — logs every event to Kermit (visible in IDE/console) |
| `NoOp` | Tests / Compose previews — silently discards |

Then just call it anywhere:

```kotlin
analytics.logScreenView("dashboard")
analytics.logButtonClick("save", screenName = "settings")
analytics.logEvent("loan_applied", "product" to "home_loan", "amount" to "25000")
analytics.logError("network timeout", errorCode = "ETIMEDOUT", screen = "checkout")
analytics.setUserId(hashedId)          // NEVER raw PII
analytics.setUserProperty("tier", "gold")
```

Every event automatically carries a `kmp_platform` dimension so you can slice one dashboard by
platform (see [SETUP.md](SETUP.md) §GA4).

---

## 3. Performance timing (P50/P95/P99)

```kotlin
val perf = AnalyticsModule.performanceTracker(analytics)

// measure a block (emits loading_time with a fast/slow/very_slow performance_level tag)
val result = perf.measure("load_dashboard") { repository.loadDashboard() }

// or manual start/stop
val h = perf.start("sync_accounts"); syncAccounts(); perf.stop(h, mapOf("count" to "42"))

// percentile snapshot across every recorded run
val stats: PerformanceStats? = perf.getPerformanceStats("load_dashboard")
// stats.count / averageMs / medianMs / p95Ms / p99Ms / minMs / maxMs

perf.logPerformanceSummary("load_dashboard")   // emit the percentiles as an event
```

---

## 4. App-launch + lifecycle timing

```kotlin
val lifecycle = AppLifecycleTracker(analytics)

// as early as possible (Application.onCreate / iOS @main init)
lifecycle.markAppLaunchStart()
// when the first frame / home screen is interactive
lifecycle.markAppLaunchComplete()          // emits app_launch with the elapsed ms

// from your platform lifecycle observer
lifecycle.onEnterBackground()
lifecycle.onEnterForeground()
```

---

## 5. Network telemetry

**Per-request** — install the Ktor client plugin; every response emits `http_request` with endpoint,
status, status class, latency ms, and a coarse latency bucket:

```kotlin
val client = HttpClient {
    install(analyticsTelemetryPlugin(analytics, enabled = { config.autoNetworkTelemetry }))
}
```

**Connectivity transitions** — emits `network.transition.offline_to_online` /
`…online_to_offline` (needs a `cmp-network-monitor`):

```kotlin
val job = analytics.attachNetworkTelemetry(networkMonitor, scope, enabled = { config.autoNetworkTelemetry })
```

---

## 6. Offline event queue

Wrap any helper so events buffer while offline and flush oldest-first on reconnect:

```kotlin
val analytics: AnalyticsHelper = OfflineEventQueue(
    delegate = firebaseHelper,
    monitor = networkMonitor,
    scope = appScope,
    maxBuffered = 500,          // bounded — oldest dropped past the cap
)
```

Because it *is* an `AnalyticsHelper`, inject it in place of the raw helper and every call site is
covered transparently.

---

## 7. Memory sampling

```kotlin
val memory = MemoryTracker(
    analytics,
    warnThresholdBytes = 256L * 1024 * 1024,
    usedMemoryBytes = { platformUsedMemory() },   // your expect/actual provider; null = skip
)
memory.logMemoryUsage("after_image_load")         // tags high/normal vs the threshold
```

---

## 8. Funnels + typed event catalogs

Conversion funnel:

```kotlin
val f = analytics.funnel("onboarding")
f.start(); f.step("enter_phone"); f.step("verify_otp"); f.complete()   // or f.abandon("otp_timeout")
```

Type-safe domain events — extend `EventCatalog` **in your app** (the library ships the pattern, not
your events):

```kotlin
object AppEvents : EventCatalog() {
    val LoanApplied = def("loan_applied", ParamKeys.FEATURE_NAME)
    fun loanApplied(product: String) = LoanApplied(ParamKeys.FEATURE_NAME to product)
}
analytics.logEvent(AppEvents.loanApplied("home_loan"))   // wrong keys fail fast
```

Batch + timed helpers:

```kotlin
analytics.batch().add("a", "k" to "v").add("b").flush()            // one drain
val t = analytics.startTiming("checkout"); /* ... */; t.complete("result" to "ok")
```

---

## 9. Compose auto-tracking (`cmp-firebase-compose`)

Add the companion and provide the helper once near the root:

```kotlin
implementation("io.github.mobilebytelabs:cmp-firebase-compose:<version>")
```

```kotlin
CompositionLocalProvider(LocalAnalyticsHelper provides analytics) { App() }
```

Whole-app screen tracking — one line next to your `NavHost` emits `screen_view` **and**
`screen_transition{from,to}` on every destination change:

```kotlin
val nav = rememberNavController()
nav.trackScreenViews()
NavHost(nav, startDestination = "home") { /* ... */ }
```

Per-screen / per-component / per-click:

```kotlin
@Composable fun ProfileScreen() {
    TrackScreenView("profile")
    TrackComposableLifecycle("profile_card")      // component_enter / component_exit
    Text("Save", Modifier.trackClick("save", rememberAnalyticsHelper(), "profile") { vm.save() })
}
```

Unset, `LocalAnalyticsHelper` defaults to `NoOpAnalyticsHelper`, so previews and tests never emit.

---

## 10. Testing your instrumentation

Use `TestAnalyticsHelper` (honours the opt-out cascade) to assert what your code logs:

```kotlin
val analytics = TestAnalyticsHelper()
viewModel.onSaveClicked()
assertEquals("button_click", analytics.loggedEvents.last().type)

analytics.setCollectionEnabled(false)
viewModel.onSaveClicked()
assertEquals(1, analytics.loggedEvents.size)   // suppressed while opted out
```

---

## Event reference (emitted by the auto-trackers)

| Event | Source | Key params |
|-------|--------|-----------|
| `screen_view` | `TrackScreenView` / `trackScreenViews()` | `screen_name`, `source_screen` |
| `screen_transition` | `trackScreenViews()` | `from`, `to` |
| `button_click` | `logButtonClick` / `Modifier.trackClick` | `button_name`, `screen_name` |
| `loading_time` | `PerformanceTracker` | `feature_name`, `loading_time_ms`, `performance_level` |
| `app_launch` | `AppLifecycleTracker` | elapsed ms |
| `http_request` | `analyticsTelemetryPlugin` | `endpoint`, `status`, `status_class`, `latency_ms`, `latency_bucket` |
| `network.transition.*` | `attachNetworkTelemetry` | — |
| `funnel_start/step/complete/abandon` | `Funnel` | `funnel`, `step`, `reason` |
| `component_enter` / `component_exit` | `TrackComposableLifecycle` | `component` |

All of the above are suppressed the instant `setCollectionEnabled(false)` is called.
