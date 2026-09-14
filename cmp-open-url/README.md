# cmp-open-url

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Open URLs, email, maps, phone and SMS from `commonMain`, with a typed result instead of a silent no-op.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-open-url:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `openUrl(url)` / `openInBrowser(url)` | Open, returning whether it was handled |
| `openWithApp(url, hint)` → `OpenUrlResult` | Prefer a specific app (`AppHint`) |
| `canOpen(url)` | Ask before rendering an affordance |
| `String.open()` / `.openInBrowser()` / `.openWith(hint)` / `.canOpen()` | Extension sugar |
| `UrlLauncher` / `UrlLauncherImpl` | Injectable facade |
| `FakeUrlLauncher` | Test double, shipped in the main artifact |
| `openUrlModule` | Koin module — binds `UrlLauncher` |

## Usage

```kotlin
class SupportViewModel(private val urls: UrlLauncher) {
    fun openDocs() = urls.open("https://example.com/docs")
    fun email()    = urls.openWith("mailto:help@example.com", AppHint.EMAIL)
}
```

`OpenUrlResult` distinguishes "opened", "no handler" and "opened with a different app than
requested", so the UI can explain what happened instead of appearing to do nothing.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
