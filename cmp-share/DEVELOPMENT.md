---
module: cmp-share
artifact: io.github.mobilebytelabs:cmp-share
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.share
api_tier: stable  # @ExperimentalShareApi retained as a deprecated no-op for source compat
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
| `io.github.mobilebytelabs:cmp-share` | `com.mobilebytelabs.kmptoolkit.share` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share) | 2026-05-30 | stable |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-share/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 4 | 2026-09-13 | full | — |
| iosMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | full | — |
| macosMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | full | — |
| jvmMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | full | — |
| jsMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | full | — |
| wasmJsMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | full | — |
| mingwMain | 🟡 | 🟡 partial | 2 | 2 | 2026-09-13 | partial | — |
| linuxMain | ✅ | ✅ real | 2 | 2 | 2026-09-13 | full | — |
| tvosMain | ✅ | ✅ real | 0 | 3 | 2026-09-13 | (legacy:full) | — |
| watchosMain | 🟡 | 🟡 partial | 4 | 2 | 2026-09-13 | partial | — |

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
  - watchOS — `watchosMain/Share.watchos.kt` (`WCSession.transferUserInfo` for text/url; binary blocked by the arm32 bit-width split — see ADR-001). All five architectures are declared and build. NOTE: `watchosSimulatorArm64Test` cannot execute on a dev box with no watchOS simulator runtime installed (Xcode reports "does not support simulator tests for watchos_simulator_arm64"); execution happens in the `watchos` job of `.github/workflows/native-tests.yml`.
  - wasmWasi — `wasmWasiMain/Share.wasmWasi.kt` (every payload → `UnsupportedPlatform`; `ShareCapabilities.None`). Asserted by `wasmWasiTest/ShareWasmWasiTest.kt` so the declared-failure contract cannot silently become a no-op.

- **Step 5 contract tests:** mirror `FakeShareLauncher` + `ShareContractTest`. Verify your impl returns the same sealed-result types as the Fake's scripted results (`Completed` / `Cancelled` / `Failed(typed cause)`).

- **Step 7 ADR:** if your graduation reverses a row from [ADR-001](docs/ADR-001-tvos-no-share-watchos-arm32.md), supersede ADR-001 with a new ADR explaining what changed.

### Recipe: Add a capability to the manager facade

`ShareManager` is the injectable surface; `Share` is the engine. Adding to the facade:

1. Add the method to `ShareManager` **as an interface default** that routes to `share(payload, options)`.
   Keep `capabilities` and `share` the only abstract members — that is what makes a test double two
   lines, and `FakeShareManager` gets your new method for free.
2. If the method takes a message alongside a payload, bundle through the private `withMessage`
   helper rather than building a `SharePayload.Multi` at the call site — a blank message must not
   produce a bundle carrying an empty `Text` item.
3. If the new capability is not universal, add a field to `ShareCapabilities` and update **all nine**
   `ShareCapabilities.<platform>.kt` actuals. Do not guess a value: read that platform's
   `when (payload)` block and record what it really does.
4. Extend `supports()` for the new payload kind. For anything bundle-shaped, recurse — a bundle is
   supported only when bundling *and* every item is.
5. Test against `FakeShareManager` in `commonTest`, asserting the PAYLOAD the method builds. That
   mapping is the whole value of the facade and is where a regression goes unnoticed.
6. Refresh BCV: `./gradlew :cmp-share:apiDump`.

Compose-only surface (anything typed in `androidx.compose.*`) belongs in `cmp-share-compose`, not
here — the headless artifact ships to tvOS, Linux and Windows, which have no Compose at all.

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

### Pattern: Declared capability, never a silent no-op

**When to use:** whenever a capability is real on some targets and absent on others — which is the
normal case for anything touching the platform.

The failure mode this avoids: a UI renders a share button on tvOS, the user selects it, and nothing
happens (or it fails after they have already picked a target). The fix is to let callers ask BEFORE
they offer, and to make the answer come from the same place the behaviour does.

**Code shape** — an `expect val` descriptor, one `actual` per source set, plus a derived query:

```kotlin
// commonMain
public expect val platformShareCapabilities: ShareCapabilities

// tvosMain — reflects what Share.tvos.kt's `when (payload)` actually does
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.TextAndUrlOnly
```

```kotlin
// derived in commonMain — no extra actuals, and bundles recurse
public fun supports(payload: SharePayload): Boolean = when (payload) {
    is SharePayload.Multi -> capabilities.multi && payload.items.all { supports(it) }
    ...
}
```

**Rules that make it hold:**

- The descriptor is written by READING each `actual`, never assumed. Every value in
  `ShareCapabilities.<platform>.kt` carries a KDoc line saying why.
- `true` means *the implementation attempts it*, not *it will succeed*. Per-call outcomes stay in
  `ShareResult` — a browser without Web Share Level 2 still reports `NoHandler`.
- A capability that is false must return a typed `ShareError.UnsupportedPlatform`, never `Unit` and
  never a swallowed exception.
- The published README table is generated from the same facts, so documentation cannot drift from
  behaviour.

Applies unchanged to the other platform-facade modules — `cmp-intent-launcher`, `cmp-app-intents`,
`cmp-clipboard`, `cmp-in-app-update`.

---

## §8 Related

- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — **single source of truth** for which KMP targets
  this module must ship (21 headless / 7 Compose) and how to handle a dependency that blocks one.
  Upstream reference: <https://kotlinlang.org/docs/native-target-support.html>.

| Type | Reference |
|------|-----------|
| GOAL.md (consumer-library-ai-bridge) | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/archive/2026-05/consumer-library-ai-bridge/GOAL.md) |
| GOAL.md (cmp-intent-share-coverage-trueup) | [cmp-intent-share-coverage-trueup](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-intent-share-coverage-trueup/GOAL.md) |
| ADRs | **[ADR-001 — tvOS no-share + watchOS arm32 binary-share policy](docs/ADR-001-tvos-no-share-watchos-arm32.md)** (2026-06-01) — locks tvOS share (wontfix-OS), watchOS arm32 binary share (wontfix-infra, policy-deferred to v0.5). |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d). Wired 2026-09-13, when `cmp-observe`
reached all 21 targets and every module could depend on it from `commonMain`.

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0 (Crashlytics attribution) | enabled | `custom_key: library:cmp-share@<version>` — set by `FirebaseCrashlyticsAttributionHook` (`cmp-observe-firebase`) |
| T1 (init + version health) | enabled | `lib_init`, `lib_init_complete`, `lib_init_failure` — emitted by `observeInit` at module initialisation |
| T2 (lifecycle events) | enabled | `shared` |
| T3 (performance traces) | opted-out | opt-in per consumer; `FirebasePerformanceHook` wraps init |
| T4 (full API usage) | opted-out | opt-in per consumer + per end-user; iOS ATT prompt required |

**Payload policy.** Events carry operation *shape*, never operation *content*. Enforced by review,
and by `.github/scripts/assert-observability-wired.sh` refusing a module that generates
`CmpMetadata` but never reports.

```yaml
# DEVELOPMENT_OBSERVABILITY.schema.yaml-conformant block
tiers:
  T0: enabled
  T1: enabled
  T2: enabled
  T3: opted-out
  T4: opted-out
event_schema:
  reports_init: true
  lifecycle_events:
    - shared
```
