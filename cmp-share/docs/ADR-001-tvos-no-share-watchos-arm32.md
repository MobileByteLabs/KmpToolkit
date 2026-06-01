# ADR-001 — tvOS no-share + watchOS arm32 binary-share policy

- **Status:** accepted
- **Date:** 2026-06-01
- **Supersedes:** none
- **Superseded by:** none
- **Related:** [DEVELOPMENT.md §2](../DEVELOPMENT.md#2-per-platform-parity-matrix-auto-gen) · `cmp-intent-share-coverage-trueup` epic (GOAL.md at `plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md`)

## Context

`cmp-share` declares 10 KMP target groups (android, ios×3, macos×2, jvm, js, wasmJs, linux, mingw, tvos×3, watchos×3+device). After the 2026-06-01 coverage truth-up (LD-2 Coverage column added), two rows are flagged `wontfix-*`:

1. **tvOS share** (`wontfix-OS`) — Apple's tvOS framework does NOT include `UIActivityViewController`. The optional `CmpShareTvosBridge.swift` consumer-provided shim (probed via ObjC runtime in `Share.tvos.kt`) can route `SharePayload.Text` to `UIPasteboard` but cannot synthesize a native share-sheet UI. Verified against `xcrun --sdk appletvos --show-sdk-platform-path` headers as of Xcode 26.5 (May 2026).

2. **watchOS binary file share** (`wontfix-infra`, policy-deferred) — `WCSession.transferFile()` accepts a `NSURL`, which has incompatible bit-widths on `watchosArm32` (32-bit `NSUInteger`) vs `watchosArm64` (64-bit). Kotlin Native's cinterop cannot reconcile this at compile time without dropping `watchosArm32` from the build target list. Text/Url share via `WCSession.transferUserInfo()` (which packs into a `[String: Any]` payload — no `NSURL` bit-width issue) remains functional on both archs.

## Decision

| Row | Status | Conditions to revisit |
|---|---|---|
| tvOS share (Image / File / Multi) | `wontfix-OS` (permanent) | Apple ships `UIActivityViewController` on tvOS (extremely unlikely) |
| tvOS share (Text / Url, via optional consumer Swift bridge) | `partial` — bridge is consumer-opt-in | n/a — bridge can be enriched by consumer apps |
| watchOS binary share via WCSession | `wontfix-infra` (policy-deferred to v0.5) | (a) `watchosArm32` is dropped from the kmp-toolkit build target list, OR (b) Apple discontinues 32-bit watchOS support naturally (Series 0-3 hardware fades from active use) |
| watchOS Text/Url share via WCSession.transferUserInfo() | `partial` (working today on both archs) | n/a |

## Consequences

- **§2 marks the affected rows explicitly** — `wontfix-OS` (tvOS) and `wontfix-infra` (watchOS arm32 binary).
- **Consumer apps shipping for tvOS** that need richer share UX must author + register the `CmpShareTvosBridge.swift` shim (documented in DEVELOPMENT.md §5). Without it, `Share.share(SharePayload.Image | File | Multi)` returns `Failed(UnsupportedPlatform)`.
- **Consumer apps shipping for watchOS** can use text/url share today on both archs. Binary share returns `Failed(UnsupportedPlatform)` on `watchosArm32`; works on `watchosArm64` IF the consumer-side build also drops `watchosArm32` from its target list (composite-build constraint).
- **The watchOS arm32 decision is reversible** — when the kmp-toolkit ecosystem decides Series 0-3 support is no longer worth the friction, dropping `watchosArm32` from the toolkit's `gradle/libs.versions.toml` + per-module `build.gradle.kts` blocks unblocks binary share on watchosArm64 universally.

## Alternatives considered

- **Force `watchosArm32` consumers onto `WCSession.transferUserInfo()` for binary too.** Rejected — `transferUserInfo()` has a 65 KB payload limit; insufficient for image/file binary payloads larger than thumbnail-size.
- **Ship our own watchOS share-sheet via `WKInterfaceImage` trickery.** Rejected — that's not a share UI; it's an image preview. Misleads consumers into thinking the API delegated to a system share-sheet.
- **Bundle `CmpShareTvosBridge.swift` inside cmp-share itself.** Rejected — Apple forbids redistributing Swift source as a Kotlin library asset; consumers must opt in by authoring the shim themselves (documented in DEVELOPMENT.md §5).
- **Drop `tvosArm64` + `tvosX64` + `tvosSimulatorArm64` from the build entirely.** Rejected — text/url share via the optional Swift bridge IS a valid use case for early adopters; dropping tvOS would regress that.

## Authored

By [cmp-intent-share-coverage-trueup](../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md) (sub-plan 05) on 2026-06-01.
