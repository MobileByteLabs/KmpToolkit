# cmp-bubble

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Floating bubbles, overlays and heads-up UI from `commonMain`, degrading honestly on every target.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-bubble:<version>")
```

## Capability, not a promise

A real floating window exists only on some platforms. `createBubble` always returns a working
`Bubble`; ask it what it can actually do before you offer the affordance:

```kotlin
val bubble = createBubble()
when (bubble.capability) {
    BubbleCapability.Bubble              -> // real OS bubble (Android)
    BubbleCapability.FloatingWindow      -> // desktop floating window
    BubbleCapability.Overlay             -> // drawn inside your own window
    BubbleCapability.Notification,
    BubbleCapability.BrowserNotification -> // degrades to a notification
    BubbleCapability.None                -> // nothing available; capabilityReason says why
}
```

`capabilityReason` is a human-readable sentence you can log or surface in a diagnostics screen.

## Key API

| API | Purpose |
|---|---|
| `createBubble(config)` | Platform bubble; never null, never throws |
| `Bubble.show(...)` / `showScreen(...)` / `showPersistent(...)` | Present a bubble |
| `Bubble.update(title, message, actions)` | Mutate the visible bubble |
| `Bubble.state: StateFlow<BubbleState>` | Observe shown / dismissed |
| `Bubble.capability` / `capabilityReason` | What this target supports, and why |
| `createBubblePermission()` | Runtime permission gate where the OS requires one |
| `bubbleModule` | Koin module |
| `FakeBubble` | Test double, shipped in the main artifact |

## Usage

```kotlin
val bubble = createBubble(BubbleConfig(style = BubbleStyle.Floating))
bubble.show(title = "Upload", message = "3 of 12 files", actions = listOf(BubbleAction("Pause")))
```

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
