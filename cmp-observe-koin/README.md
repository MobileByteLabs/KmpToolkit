# cmp-observe-koin

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Register [cmp-observe](../cmp-observe/README.md) hooks through Koin.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-observe-koin:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `observeKoinModule(hooks)` | Koin module that registers each hook at startup |

## Usage

```kotlin
startKoin {
    modules(
        observeKoinModule(
            listOf(FirebaseCrashlyticsAttributionHook(), FirebaseAnalyticsHealthHook()),
        ),
    )
}
```

Koin starts before most library initialisation, which makes this a good place to register — but
Android `ContentProvider`-based library init still runs earlier, so those init events fire before
Koin exists. Register hooks you need attributed for Android init directly in a `ContentProvider` or
in `Application.attachBaseContext`.

Ships 20 targets rather than 21: koin-core has no wasmWasi build. See
[TARGET_MATRIX.md](../TARGET_MATRIX.md) for the escape-hatch table.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
