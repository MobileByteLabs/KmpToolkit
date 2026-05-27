# Migrating from 3.2.x to 3.3.0

## TL;DR

**3.3.0 is source-compatible with 3.2.x.** No required code changes — bump the version, rebuild, retest.

Two HIGH-severity teardown / cold-start races (`M-001`, `M-002`) and four ergonomic gaps
(`M-003`, `M-004`, `M-005`, `M-007`) were addressed. Every new API is opt-in: existing call sites
behave identically. This guide explains what changed and shows the recommended call sites for the
new APIs when you want them.

> **Cross-reference**: per-change implementation notes live in
> [`CHANGELOG.md`](CHANGELOG.md). The per-platform test matrix lives in
> [`TESTING.md`](TESTING.md).

---

## 1. M-001 / M-002 HIGH fixes — automatic, no migration needed

| ID | Surface | What changed |
|----|---------|--------------|
| `M-001` | `JsNetworkMonitor.close()` | Removes `online` / `offline` event listeners BEFORE cancelling the coroutine scope. Handler bodies now defensively check `closed && scope.isActive`. Eliminates console-visible `CancellationException` during rapid app teardown. |
| `M-001 mirror` | `WasmJsNetworkMonitor.close()` | Same ordering fix + same defensive guard. |
| `M-002` | `AndroidNetworkMonitor` | Replaced `AtomicBoolean callbackReady` drop-gate with `CompletableDeferred<Unit> seedComplete` queue-gate. `NetworkCallback` events arriving during cold-start `seedInitialState()` now queue inside `scope.launch { seedComplete.await(); ... }` instead of being silently dropped. Seed runs in `try / finally` so the gate releases even if seeding throws. |

**Migration**: none. Bump to 3.3.0 and retest. Verified live in `jsBrowserTest` (4/4),
`wasmJsBrowserTest` (3/3), and via `SeedGatePatternTest` in `commonTest`.

---

## 2. M-003 — Debounced StateFlow helpers

### Before (cold `Flow`, still supported)

```kotlin
// Cold flow — late subscribers wait for the next change before seeing a value.
monitor.isOnlineDebounced(timeoutMs = 300L)
    .onEach { online -> updateUi(online) }
    .launchIn(viewModelScope)

monitor.debouncedNetworkStatus(timeoutMs = 300L)
    .onEach { status -> renderStatus(status) }
    .launchIn(viewModelScope)
```

### After (hot `StateFlow`, recommended for UI / late subscribers)

```kotlin
// Hot StateFlow — seeded with the monitor's current value; late subscribers
// immediately read it via .value or first collect.
val online: StateFlow<Boolean> =
    monitor.isOnlineDebouncedState(viewModelScope, timeoutMs = 300L)

val status: StateFlow<NetworkStatus> =
    monitor.networkStatusDebouncedState(viewModelScope, timeoutMs = 300L)

online.value           // sync read — no suspension
online.collect { ... } // late subscriber sees current value first
```

### Why both exist

| API | Shape | Late-subscriber behaviour |
|-----|-------|---------------------------|
| `isOnlineDebounced(timeoutMs)` | `Flow<Boolean>` (cold) | Waits for next emission. Source-compatible — kept for 3.2.x callers. |
| `debouncedNetworkStatus(timeoutMs)` | `Flow<NetworkStatus>` (cold) | Waits for next emission. Source-compatible. |
| `isOnlineDebouncedState(scope, timeoutMs)` | `StateFlow<Boolean>` (hot) | Sees the currently-settled value immediately. **New in 3.3.0.** |
| `networkStatusDebouncedState(scope, timeoutMs)` | `StateFlow<NetworkStatus>` (hot) | Sees the currently-settled value immediately. **New in 3.3.0.** |

> The hot variants share a single upstream collector while `scope` is active. Pass a
> lifecycle-bound scope (`viewModelScope`, `rememberCoroutineScope()`) to avoid leaks.

### When to migrate a call site

Migrate to the StateFlow variant when:
- The consumer is a Compose composable, a ViewModel state holder, or any other late-subscriber.
- You want a synchronous `.value` read at startup (e.g. inside a `LaunchedEffect`'s initial branch).

Stay on the cold variant when:
- You only need transient emissions for a side-effecting pipeline that subscribes immediately on
  construction and unsubscribes deterministically.

---

## 3. M-005 — Compose debounce config

Five Compose APIs gained an optional `debounceMs: Long = 0L` parameter. **Default `0L` is
identical to the pre-3.3.0 behaviour** — no migration needed unless you want flicker suppression.

### Before (raw, no debounce — still the default)

```kotlin
ConnectivityBanner()                                            // raw collectAsState
NetworkAwareContent(onlineContent = { /* ... */ })              // raw collectAsState

val online by monitor.collectIsOnlineAsState()                  // raw collectAsState
val status by monitor.collectNetworkStatusAsState()             // raw collectAsState
val quality by monitor.collectNetworkQualityAsState()           // raw collectAsState
```

### After (opt-in debounce — suppresses WiFi <-> Cellular handoff flicker)

```kotlin
ConnectivityBanner(debounceMs = 500L)                           // banner won't flash
NetworkAwareContent(
    debounceMs = 500L,
    onlineContent = { /* ... */ },
)

val online by monitor.collectIsOnlineAsState(debounceMs = 300L)
val status by monitor.collectNetworkStatusAsState(debounceMs = 300L)
val quality by monitor.collectNetworkQualityAsState(debounceMs = 300L)
```

### Notes

- Internally the Compose helpers route through the new `*DebouncedState` extensions when
  `debounceMs > 0L`. When `debounceMs <= 0L` the original `flow.collectAsState()` path is used
  byte-for-byte — there's no behavioural drift on default-argument call sites.
- `collectNetworkQualityAsState(debounceMs > 0L)` debounces the upstream `NetworkStatus`
  **before** mapping to `NetworkQuality`. The mapping is a pure function, so quality transitions
  reflect the settled connection rather than transient flicker.
- 300L–500L is a reasonable starting window for handoff suppression; 0L disables debounce.

---

## 4. M-004 — Reset-aware `rememberNetworkMonitor()`

### Behaviour change (no source change required)

`NetworkMonitorProvider` gained a `version: StateFlow<Int>` generation counter that increments
exactly once on:
- each successful `install()` that creates a **new** instance (redundant installs that return
  the cached instance do NOT bump it), and
- each `reset()` that had a **non-null** instance to tear down.

`rememberNetworkMonitor()` now keys `remember` on this version:

```kotlin
@Composable
fun rememberNetworkMonitor(config: NetworkMonitorConfig = NetworkMonitorConfig()): NetworkMonitor {
    val version by NetworkMonitorProvider.version.collectAsState()
    return remember(version) { NetworkMonitorProvider.install(config) }
}
```

### What this fixes

Pre-3.3.0, if your app called `NetworkMonitorProvider.reset()` (e.g. on logout, in test cleanup,
or during integration-test teardown), composables that had cached the old `NetworkMonitor` via
`rememberNetworkMonitor()` continued to hold the **closed** reference. Collecting on it would
silently see no further emissions.

In 3.3.0 the generation bump triggers recomposition and `remember(version)` re-runs
`NetworkMonitorProvider.install(config)`, returning the **fresh** instance.

### Migration

| Scenario | Action |
|----------|--------|
| App never calls `NetworkMonitorProvider.reset()` | No action — `version` stays at 1, no extra recomposition. |
| App calls `reset()` on logout / account switch | No code change — recomposition is now correct. Verify your logout flow re-renders downstream consumers (it now will). |
| Tests call `reset()` between cases | No code change — tests now observe the fresh instance per case without manual cache busting. |
| Code calls `reset()` defensively when no instance is installed | No-op now — `version` does NOT advance on a reset-while-empty, so reactive consumers won't see a spurious recomposition. |

### Thread-safety note

`NetworkMonitorProvider` KDoc was corrected: it is **NOT** thread-safe. Serialize `install()` /
`reset()` from a single thread (typically the main thread at app startup / shutdown or the
test-fixture's teardown thread). Reads via `get()` / `getOrNull()` / `version` / `currentStatus`
remain safe from any thread once installation has settled.

---

## 5. M-007 (residual) — `ProvideNetworkMonitor` + `rememberOrLocalNetworkMonitor`

`LocalNetworkMonitor` (a `CompositionLocal<NetworkMonitor>`) already existed in 3.2.x. 3.3.0
adds two ergonomic helpers around it for **subtree-scoped** monitor injection.

### Before (3.2.x — manual CompositionLocalProvider wrap)

```kotlin
val customMonitor = remember { createNetworkMonitor(myConfig) }
CompositionLocalProvider(LocalNetworkMonitor provides customMonitor) {
    MyScreenSubtree()
}

// Inside the subtree:
@Composable
fun OfflineHint() {
    val monitor = LocalNetworkMonitor.current   // throws if no provider
    val online by monitor.collectIsOnlineAsState()
    // ...
}
```

### After (3.3.0 — opt-in helpers)

```kotlin
// Wrap a subtree with a custom monitor (e.g. an in-test FakeNetworkMonitor)
val customMonitor = remember { createNetworkMonitor(myConfig) }
ProvideNetworkMonitor(customMonitor) {
    MyScreenSubtree()
}

// Inside the subtree, accept BOTH provider-injected and singleton-fallback callers
// without writing two overloads:
@Composable
fun OfflineHint(
    monitor: NetworkMonitor = rememberOrLocalNetworkMonitor(),
) {
    val online by monitor.collectIsOnlineAsState()
    // ...
}
```

### When to use which

| Call site | Recommended default |
|-----------|---------------------|
| App-level / single-monitor app | `rememberNetworkMonitor()` (singleton) — unchanged. |
| Reusable consumer composable (library / shared UI) | `rememberOrLocalNetworkMonitor()` — prefers `ProvideNetworkMonitor`'d subtree, falls back to singleton. |
| Subtree with isolated config (per-screen, per-test, multi-window) | Wrap with `ProvideNetworkMonitor(customMonitor) { ... }`. |
| Direct `CompositionLocalProvider(LocalNetworkMonitor provides ...)` | Still works — but `rememberOrLocalNetworkMonitor()` will fall through to the singleton instead of returning your manually-provided monitor. Prefer `ProvideNetworkMonitor` so the nullable mirror is populated in lock-step. |

> Implementation detail (informational): `rememberOrLocalNetworkMonitor()` reads a `private`
> `LocalNetworkMonitorOrNull` mirror that `ProvideNetworkMonitor` populates alongside
> `LocalNetworkMonitor`. This works around a Compose compiler restriction that prevents wrapping
> the throwing-default `LocalNetworkMonitor.current` access in `try` / `runCatching` inside a
> `@Composable`.

---

## Things to verify after upgrade

- [ ] App still builds and existing tests pass with `cmp-network-monitor` bumped to 3.3.0 and no
      other code changes (source compatibility check).
- [ ] If your app logs at WARN/ERROR during teardown, the `CancellationException`-on-close noise
      from `JsNetworkMonitor` / `WasmJsNetworkMonitor` is gone.
- [ ] On Android cold start, any UI that depends on the very first `NetworkCallback` event no
      longer sees a missed event (events that fire during `seedInitialState()` now queue instead
      of being dropped).
- [ ] If you use `NetworkMonitorProvider.reset()` (logout flow, test cleanup), downstream
      composables consuming via `rememberNetworkMonitor()` now correctly re-acquire the fresh
      instance after `reset()`.
- [ ] If you introduce `debounceMs > 0L` on `ConnectivityBanner` / `NetworkAwareContent` /
      `collect*AsState`, verify the chosen window matches your handoff characteristics (300L–500L
      is a reasonable starting point) and that no business logic depends on raw-flicker emissions.
