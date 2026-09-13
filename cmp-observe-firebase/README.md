# cmp-observe-firebase

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Ready-made [`LibraryObservationHook`](../cmp-observe/) implementations that forward KmpToolkit
library lifecycle events to Firebase.

Split out of `cmp-observe` on 2026-09-13. `cmp-observe` is now pure stdlib on all 21 targets, so
every `cmp-*` module can depend on it to report itself; the Firebase hooks live here, where the
Firebase SDKs constrain the target set to 4.

## Targets

Android, iOS (arm64, x64, simulatorArm64) — the targets the GitLive Firebase SDK supports. This is
the documented exception in [TARGET_MATRIX.md](../TARGET_MATRIX.md): the module ships fewer than 21
because a third-party dependency, not a Kotlin tier, sets the ceiling.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-observe-firebase:<version>")
```

`cmp-observe` comes transitively (declared `api`) — a consumer registering a hook holds
`LibraryObservationHook` from it, so it must be on the compile classpath.

## Hooks

| Hook | What it does |
|---|---|
| `FirebaseAnalyticsHealthHook` | Logs library init / lifecycle events as Analytics events |
| `FirebaseCrashlyticsAttributionHook` | Sets Crashlytics keys so a crash names the library that was initialising |
| `FirebasePerformanceHook` | Opens a Performance trace per library init |

## Usage

```kotlin
// once at startup, after Firebase.initialize(...)
LibraryObservation.register(FirebaseCrashlyticsAttributionHook())
LibraryObservation.register(FirebaseAnalyticsHealthHook())
```

Register before the libraries you want attributed. Hooks registered later will not see init events
that already fired — Android `ContentProvider`-based library init runs before `Application.onCreate`.

## What gets reported

Every `cmp-*` module reports its own identity (name, version, artifact) plus operation **shape** —
never operation content. A clipboard copy reports success, not the text; a toast reports duration
and style, not the message; a PDF reports page count and result class, not the document. See each
module's `## §9 Observability Surface` section in its `DEVELOPMENT.md`.

## No BCV baseline

This module has no `api/` baseline, unlike every other published module. Binary-compatibility-validator
emits a dump only for a JVM-like target, and this module declares none — GitLive publishes no `jvm`
variant for `firebase-crashlytics` or `firebase-perf` (analytics only), so one cannot be added.

The practical consequence: `apiCheck` will not catch a breaking change to these three classes. What
does constrain them is `LibraryObservationHook` in [cmp-observe](../cmp-observe/README.md), which
*is* BCV-guarded — these are implementations of that interface and little else. Treat a change to
their constructors or public members as a breaking change by inspection.

## Related

- [cmp-observe](../cmp-observe/) — the hook interface + `observeInit` / `observeLifecycle` helpers (21 targets, no dependencies)
- [cmp-observe-koin](../cmp-observe-koin/) — Koin module wiring for hook registration
- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state
