# cmp-in-app-update

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Check for and start app updates from `commonMain` — Play, App Store, GitHub or your own endpoint.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-in-app-update:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `AppUpdateManager` / `AppUpdateManagerImpl` | Injectable facade |
| `check()` → `UpdateOutcome` | Is an update available |
| `checkAndStart(updateType)` | Check, then start if available |
| `start(updateType)` / `openStore()` | Start the flow, or fall back to the store listing |
| `currentVersion()` → `AppVersion` | The running version |
| `VersionResolver` (`GitHubResolver`, `SupabaseResolver`) | Where "latest" comes from |
| `appUpdateModule` | Koin module |
| `FakeAppUpdateManager` | Test double, shipped in the main artifact |

## Usage

```kotlin
when (val outcome = updates.check()) {
    is UpdateOutcome.Available -> updates.start(UpdateType.FLEXIBLE)
    UpdateOutcome.UpToDate     -> Unit
    is UpdateOutcome.Failed    -> log(outcome)
}
```

`UpdateType.IMMEDIATE` blocks the app until the update completes (Play only); `FLEXIBLE` downloads
in the background. Targets with no OS update mechanism resolve through `VersionResolver` +
`openStore()`, so the user still reaches the new build.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
