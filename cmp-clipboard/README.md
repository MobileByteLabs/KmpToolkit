# cmp-clipboard

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Copy, paste, observe and filter the system clipboard from `commonMain`.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-clipboard:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `copyToClipboard(text)` / `getFromClipboard()` | Top-level primitives |
| `hasClipboardText()` / `clearClipboard()` | Query / clear |
| `ClipboardManager` | Injectable facade (`copy`, `paste`, `hasText`, `clear`, async variants) |
| `platformClipboardCapabilities` | What THIS target supports |
| `createClipboardObserver()` | Observe external clipboard changes where the OS allows |
| `InAppClipboardMonitor` | In-process monitoring where it does not |
| `createClipboardHistory(maxSize)` | Bounded history of `ClipboardHistoryEntry` |
| `ClipboardFilter` / `ClipboardContentType` | Classify and filter (URL, email, phone…) |
| `createClipboardPermission()` | Permission gate where required |
| `clipboardModule` | Koin module |

## Usage

```kotlin
class ShareViewModel(private val clipboard: ClipboardManager) {
    fun copyInvite(code: String) = clipboard.copy("https://example.com/join/" + code)
}
```

## Observing changes

No platform lets you watch the clipboard freely — iOS gives no change callback, and browsers require
a user gesture. `createClipboardObserver()` uses the real OS signal where one exists;
`InAppClipboardMonitor` covers changes your own app makes. Check
`platformClipboardCapabilities` (`read`, `write`, `systemWide`, `readNeedsPermission`)
before offering a live-updating UI or a paste affordance.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
