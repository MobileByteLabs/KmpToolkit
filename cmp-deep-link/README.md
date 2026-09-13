# cmp-deep-link

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Parse and route deep links from `commonMain`, with a declarative pattern DSL.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-deep-link:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `DeepLinkHandler` | Process-wide entry point; platforms feed it inbound links |
| `DeepLinkManager` / `DeepLinkManagerImpl` | Injectable facade |
| `DeepLinkManager.lastReceived: StateFlow<DeepLink?>` | Observe the latest link |
| `deepLinkParser { }` | Declarative pattern → a typed destination |
| `FakeDeepLinkManager` | Test double, shipped in the main artifact |

## Usage

```kotlin
@Serializable data class Order(val id: String)
@Serializable data class Promo(val code: String)

val parser = deepLinkParser {
    route<Order>("/order/{id}")
    route<Promo>("/promo/{code}")
}
```

On Android inbound links are picked up automatically by a `ContentProvider` +
`ActivityLifecycleCallbacks` pair, so there is nothing to wire into `Application`. Other platforms
call `DeepLinkHandler.handle(uri)` from their own entry point.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
