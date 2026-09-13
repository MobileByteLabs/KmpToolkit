# cmp-observe

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Library lifecycle observability for the whole toolkit — the hook interface plus the helpers every `cmp-*` module reports through. Zero dependencies, all 21 targets.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-observe:<version>")
```

## Why

Every `cmp-*` module reports its own init, failures and operations. Register one hook and you see the
whole toolkit: which library initialised, which failed and why, and what shape of operation ran.

## Key API

| API | Purpose |
|---|---|
| `LibraryObservationHook` | Implement to receive events (`onInitStart`, `onInitComplete`, `onInitFailure`, `onLifecycleEvent`, `onClose`) |
| `LibraryObservation.register(hook)` | Register; exceptions thrown by hooks are isolated |
| `observeInit(meta) { … }` | Report init / complete / failure around a block, rethrowing |
| `observeLifecycle(meta, event, payload)` | Report one operation |
| `observeClose(meta)` | Report teardown |
| `CmpMetadata` | Library identity (name, version, artifact) |
| `resetLibraryObservation()` | Test helper |

## Usage

```kotlin
LibraryObservation.register(object : LibraryObservationHook {
    override fun onInitStart(meta: CmpMetadata) = log(meta.name + " " + meta.version + " starting")
    override fun onInitComplete(meta: CmpMetadata) = Unit
    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) = report(throwable)
    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) = Unit
    override fun onClose(meta: CmpMetadata) = Unit
})
```

Register before the libraries you want attributed. Android library init runs from a
`ContentProvider`, i.e. before `Application.onCreate`, so a hook registered there will not see those
init events.

## Payload policy

Events carry operation *shape*, never content. A clipboard copy reports success but not the text; a
toast reports duration and style but not the message; a PDF reports page count and result class but
not the document. The CI gate `.github/scripts/assert-observability-wired.sh` fails any module that
generates `CmpMetadata` but never reports.

## Ready-made hooks

- [cmp-observe-firebase](../cmp-observe-firebase/README.md) — Analytics, Crashlytics attribution, Performance
- [cmp-observe-koin](../cmp-observe-koin/README.md) — register hooks through Koin

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
