# ADR-09 — Platform coverage decisions for cmp-intent-launcher

- **Status:** accepted
- **Date:** 2026-06-01
- **Supersedes:** none
- **Superseded by:** none
- **Related:** [DEVELOPMENT.md §2](../DEVELOPMENT.md#2-per-platform-parity-matrix-auto-gen) · `cmp-intent-share-coverage-trueup` epic (GOAL.md at `plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md`)

## Context

`cmp-intent-launcher` declares 10 KMP target groups (android, ios×3, macos×2, jvm, js, wasmJs, linux, mingw, tvos×3, watchos×3+device). After the 2026-06-01 coverage truth-up (LD-2 Coverage column added), three rows are flagged `wontfix-OS` or `wontfix-infra`:

1. **tvOS pickers** (`wontfix-OS`) — Apple's tvOS framework does NOT include `PHPickerViewController`, `UIDocumentPickerViewController`, or `CNContactPickerViewController`. No third-party shim exists at the framework layer. Verified against `xcrun --sdk appletvos --show-sdk-platform-path` headers as of Xcode 26.5 (May 2026).

2. **mingw Win32 file picker** (`wontfix-infra`) — `GetOpenFileNameW` is exposed by the Windows SDK's `Commdlg.h` + `Comdlg32.dll`. Kotlin/Native cinterop requires a Windows host (or a wine-based cross-compile shim) to resolve the symbol set against the SDK's `.def` definitions. CI runners today are macOS-only; cross-compiling on macOS yields a "symbol not found" link error on `mingwX64`. The URL-launch path (`cmd /c start <url>`) does NOT require Win32 SDK cinterop and remains functional.

3. **watchOS pickers** (`wontfix-OS`) — Apple's watchOS framework exposes only `WKExtension.openSystemURL()` for http/https/mailto URLs. There are no native `UIPicker*` view controllers on watchOS. Consumer apps that need rich picker UX delegate to the paired iPhone via `WCSession` (out of scope for this module).

## Decision

| Row | Status | Conditions to revisit |
|---|---|---|
| tvOS pickers | `wontfix-OS` (permanent) | Apple ships PHPicker / UIDocumentPicker / CNContactPicker on tvOS |
| mingw Win32 picker | `wontfix-infra` (deferred) | (a) CI adds a Windows-native runner, OR (b) a third-party Kotlin/Native Win32 bindings library ships with the cinterop wired |
| watchOS pickers | `wontfix-OS` (permanent) | Apple ships UIPicker* on watchOS (extremely unlikely given the form factor) |

URL launch via `cmd /c start` (mingw) and `WKExtension.openSystemURL` (watchOS) remains functional and reflected in §2 as `partial` (not `wontfix-*`).

## Consequences

- **§2 marks these rows explicitly** — `wontfix-OS` or `wontfix-infra` — preventing future maintainers from spending cycles trying to "complete" them.
- **Consumers targeting tvOS / watchOS pickers receive `IntentResult.Failed(IntentError.UnsupportedPlatform)`** with a clearly-named cause; they can branch on it gracefully and degrade to a Compose-only fallback UI.
- **The §5 "How a platform graduates from stub to real" recipe** explicitly documents that `wontfix-infra` rows can graduate to `full` if the infra blocker is resolved (drop the comment, run the scanner). `wontfix-OS` rows cannot graduate without Apple shipping new APIs — superseding ADR-09 with a new ADR is the path.

## Alternatives considered

- **Build our own tvOS picker shim.** Rejected. tvOS doesn't expose view controllers that could host a picker UI; this isn't a missing binding, it's a missing OS feature.
- **Cross-compile Win32 cinterop on macOS via wine.** Rejected. Brittle, unreproducible across developer machines, doesn't survive on macOS CI (no GUI), and lock-in to a specific wine version.
- **Drop mingw target entirely.** Rejected. URL launch IS functional on mingw; dropping the target would regress that use case for the small number of Windows-native Kotlin consumers.
- **Pivot to a synthetic "PickerUnavailable" sub-class hierarchy.** Rejected. The `IntentError.UnsupportedPlatform` typed result is sufficient; adding sub-types complicates the consumer's `when` statement without enabling new behavior.

## Authored

By [cmp-intent-share-coverage-trueup](../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md) (sub-plan 05) on 2026-06-01.
