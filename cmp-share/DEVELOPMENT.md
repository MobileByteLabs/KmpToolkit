---
module: cmp-share
artifact: io.github.mobilebytelabs:cmp-share
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.share
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-share — Development

> Single source of truth for development state of `cmp-share` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-share` | `com.mobilebytelabs.kmptoolkit.share` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-share/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 3 | 2026-06-01 | full | — |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | full | — |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | full | — |
| jvmMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | full | — |
| jsMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | full | — |
| wasmJsMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | full | — |
| mingwMain | 🟡 | 🟡 partial | 3 | 1 | 2026-06-01 | partial | — |
| linuxMain | ✅ | ✅ real | 2 | 1 | 2026-06-01 | full | — |
| tvosMain | 🟡 | 🟡 wontfix-OS | 2 | 1 | 2026-06-01 | wontfix-OS | — |
| watchosMain | 🟡 | 🟡 partial | 4 | 1 | 2026-06-01 | partial | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
public sealed class SharePayload {
public sealed class ShareResult {
public sealed class ShareError {
```

---

## §4 Spec Snapshot (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

**Problem this module solves:** _TBD by author._

**Core invariants:**
- _TBD by author._

**Out of scope (by design):**
- _TBD by author._

---

## §5 Extension Recipes (authored — cmp-intent-share-coverage-trueup, 2026-06-01)

### Recipe: How a platform graduates from stub → partial → full

Same procedure as `cmp-intent-launcher` — see [cmp-intent-launcher DEVELOPMENT.md §5](../cmp-intent-launcher/DEVELOPMENT.md#5-extension-recipes-authored--cmp-intent-share-coverage-trueup-2026-06-01) for the canonical 7-step playbook. `cmp-share`-specific adaptations:

- **Step 2 template files:**
  - Android — `androidMain/Share.android.kt` (Intent.ACTION_SEND + FileProvider for binary; EXTRA_TEXT for text/url).
  - iOS — `iosMain/Share.ios.kt` (UIActivityViewController via `suspendCancellableCoroutine`; resolves top-most VC via traversal).
  - macOS — `macosMain/Share.macos.kt` (NSSharingServicePicker; `showRelativeToRect` on anchor view).
  - JVM — `jvmMain/Share.jvm.kt` (AWT clipboard + OS-dispatch chain via ProcessBuilder).
  - JS/wasmJs — `jsMain/Share.js.kt` / `wasmJsMain/Share.wasmJs.kt` (`navigator.share` Web Share API; Level-2 file support via `Blob` / `File` + `navigator.canShare({files})` feature-detect; falls back to clipboard).
  - Linux — `linuxMain/Share.linux.kt` (POSIX `fopen`+`fwrite` to `$TMPDIR/cmp-share-*` for Image; `xclip` for text; `xdg-open` for url/file).
  - mingw — `mingwMain/Share.mingw.kt` (Win32 `ShellExecuteW` for url; clipboard API for text; binary blocked — see ADR-001).
  - tvOS — `tvosMain/Share.tvos.kt` (optional consumer-provided `CmpShareTvosBridge.swift` probed via ObjC runtime — Text/Url only).
  - watchOS — `watchosMain/Share.watchos.kt` (`WCSession.transferUserInfo` for text/url; binary blocked on arm32 — see ADR-001).

- **Step 5 contract tests:** mirror `FakeShareLauncher` + `ShareContractTest`. Verify your impl returns the same sealed-result types as the Fake's scripted results (`Completed` / `Cancelled` / `Failed(typed cause)`).

- **Step 7 ADR:** if your graduation reverses a row from [ADR-001](docs/ADR-001-tvos-no-share-watchos-arm32.md), supersede ADR-001 with a new ADR explaining what changed.

### Recipe: Add a new SharePayload subtype

1. Add `public {data} class NewType(...) : SharePayload()` to `commonMain/Share.kt`.
2. Add a DSL convenience helper: `public suspend fun Share.newtype(...) = share(SharePayload.NewType(...))`.
3. Per-platform — extend each `Share.{platform}.kt`'s `when (payload)` block to handle the new subtype. Platforms that can't route the new payload return `Failed(UnsupportedPlatform)`.
4. Update the per-platform LD-2-coverage annotation if any platform's coverage degrades from `full` → `partial`.
5. Add a contract test exercising the new subtype against `FakeShareLauncher`.
6. Refresh BCV baseline: `./gradlew :cmp-share:apiDump`.

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-share` — refresh via `gh pr list --label cmp-share` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

### Pattern: _Pattern name TBD_

**When to use:** _TBD_
**Code shape:**
```kotlin
// TBD
```

---

## §8 Related

| Type | Reference |
|------|-----------|
| GOAL.md (consumer-library-ai-bridge) | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/archive/2026-05/consumer-library-ai-bridge/GOAL.md) |
| GOAL.md (cmp-intent-share-coverage-trueup) | [cmp-intent-share-coverage-trueup](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md) |
| ADRs | **[ADR-001 — tvOS no-share + watchOS arm32 binary-share policy](docs/ADR-001-tvos-no-share-watchos-arm32.md)** (2026-06-01) — locks tvOS share (wontfix-OS), watchOS arm32 binary share (wontfix-infra, policy-deferred to v0.5). |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |
