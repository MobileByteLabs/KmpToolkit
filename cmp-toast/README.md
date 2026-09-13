# cmp-toast

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Toasts and snackbars for Compose Multiplatform, injectable from a ViewModel.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-toast:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `ToastDispatcher` | The type to depend on from a ViewModel |
| `ToastHostState` | `ToastDispatcher` implementation plus Compose state |
| `ToastHost(...)` | Place once near the root of your UI |
| `rememberToastHostState()` / `rememberToastDispatcher()` | Compose accessors |
| `ProvideToastDispatcher(...)` / `LocalToastDispatcher` | CompositionLocal |
| `ToastDuration` / `ToastPosition` / `ToastStyle` | Presentation |
| `ClipboardToastHost` / `showCopiedToast(...)` | Ready-made "Copied" feedback |
| `FakeToastDispatcher` | Test double, shipped in the main artifact |

## Usage

```kotlin
val toasts = rememberToastHostState()
ToastHost(state = toasts)               // once, near the root

// anywhere, with the dispatcher injected
class CheckoutViewModel(private val toasts: ToastDispatcher) {
    suspend fun onPaid() = toasts.showToast("Payment received", duration = ToastDuration.SHORT)
}
```

Depending on `ToastDispatcher` rather than `ToastHostState` keeps ViewModels free of Compose and
substitutable in tests via `FakeToastDispatcher`.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
