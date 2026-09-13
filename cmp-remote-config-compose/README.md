# cmp-remote-config-compose

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Compose Multiplatform rendering for [cmp-remote-config](../cmp-remote-config/README.md) — server-driven UI.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-remote-config-compose:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `RemoteConfigHost(viewModel, onAction)` | Drop-in host that fetches, evaluates and presents active configs; records impressions |
| `DynamicUiRenderer(node, onAction)` | Render a `UiNode` tree |
| `RemoteConfigViewModel` / `RemoteConfigState` | State holder |
| `Module.remoteConfig { }` | Koin wiring DSL |

## Usage

```kotlin
RemoteConfigHost(
    onAction = { actionType, actionValue -> handle(actionType, actionValue) },
)
```

`DynamicUiRenderer` renders nothing for `UiNode.Unknown` — a node type this version does not know.
That is a deliberate branch rather than an `else`, so a genuinely new `UiNode` variant that needs a
renderer fails the build instead of silently disappearing.

## Observability

This module reports nothing by design — every composable delegates to
[cmp-remote-config](../cmp-remote-config/README.md), which reports the operation. Reporting in both would double every count a
consumer's hook sees.

## Related

- [cmp-remote-config](../cmp-remote-config/README.md) — the headless core
- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
