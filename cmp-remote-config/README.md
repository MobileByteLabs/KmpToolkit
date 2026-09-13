# cmp-remote-config

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Remote config, feature flags and server-driven UI documents — headless.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-remote-config:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `RemoteConfigService` | Fetch active configs, record impressions, dismiss |
| `RemoteConfigEvaluator` | Decide whether a config applies to this device / app / version |
| `RemoteConfig` / `DisplayType` | A delivered config and how it should present |
| `UiDocument` / `UiNode` / `UiAction` | Server-driven UI tree |
| `UiNodeParser` | Parse a document, emitting `UiNode.Unknown` for unknown types |
| `RemoteConfigLocalStore` / `DeviceIdProvider` | Local persistence |
| `ActionHandler` / `ActionContext` | Dispatch a `UiAction` |

## Forward compatibility

`UiNodeParser` never throws on an unrecognised node type — it emits `UiNode.Unknown(type, raw)`, so
one unknown node in a remotely delivered document cannot blank the screen. A host that does know the
type can still render it from `raw`.

## UI

Compose rendering (`RemoteConfigHost`, `DynamicUiRenderer`) lives in
[cmp-remote-config-compose](../cmp-remote-config-compose/README.md).

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
