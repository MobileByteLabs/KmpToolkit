---
module: cmp-intent-launcher
artifact: io.github.mobilebytelabs:cmp-intent-launcher
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.intent.launcher
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-intent-launcher — Development

> Single source of truth for development state of `cmp-intent-launcher` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-intent-launcher` | `com.mobilebytelabs.kmptoolkit.intent.launcher` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-intent-launcher) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-intent-launcher/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 6 | 2026-06-01 | full | — |
| iosMain | ✅ | ✅ real | 3 | 2 | 2026-06-01 | full | — |
| macosMain | ✅ | ✅ real | 3 | 2 | 2026-06-01 | full | — |
| jvmMain | ✅ | ✅ real | 3 | 2 | 2026-06-01 | full | — |
| jsMain | ✅ | ✅ real | 5 | 2 | 2026-06-01 | full | — |
| wasmJsMain | ✅ | ✅ real | 7 | 2 | 2026-06-01 | full | — |
| mingwMain | 🟡 | 🟡 wontfix-infra | 5 | 2 | 2026-06-01 | wontfix-infra | — |
| linuxMain | ✅ | ✅ real | 3 | 2 | 2026-06-01 | full | — |
| tvosMain | 🟡 | 🟡 wontfix-OS | 4 | 2 | 2026-06-01 | wontfix-OS | — |
| watchosMain | 🟡 | 🟡 partial | 5 | 2 | 2026-06-01 | partial | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
public sealed class IntentResult {
public sealed class IntentError {
public sealed interface ResultContract<R> {
public object ResultContracts {
public class IntentBuilder internal constructor() {
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

Use when adding (or completing) a platform impl for `cmp-intent-launcher`.

**Step 1 — Decide the LD-2 Coverage class.** Pick one:
- `full` — every public-API method (`launch`, `SystemIntents.*`) has a real OS-primitive-backed impl
- `partial` — most methods real; some return typed `IntentResult.Failed(IntentError.UnsupportedPlatform)` fallbacks for contracts that don't apply on this OS (e.g. macOS has no app-settings URI scheme)
- `wontfix-OS` — OS lacks the primitive (see [ADR-09](docs/ADR-09-platform-coverage.md))
- `wontfix-infra` — impl possible but CI/toolchain blocks it (see ADR-09)

**Step 2 — Author the platform actuals.** Mirror an existing impl as template:
- Apple platforms (ios/macos/tvos/watchos) → see `iosMain/IntentLauncher.ios.kt` for the `suspendCancellableCoroutine` + delegate-pinning pattern (UIKit weak-ref delegates need a strong-ref pin to survive ARC).
- JVM/desktop → see `jvmMain/IntentLauncher.jvm.kt` for the `Desktop.browse()` + `ProcessBuilder` OS-detect fallback chain.
- Web (js/wasmJs) → see `jsMain/IntentLauncher.js.kt` for `<input type=file>` + `UserGestureMissing` handling.
- Native (linux/mingw) → see `linuxMain/IntentLauncher.linux.kt` for `zenity --file-selection` subprocess + `xdg-open` URL launch.

**Step 3 — Annotate the LD-2 Coverage class.** Add a comment at the top of the platform's primary `.kt` file:
```kotlin
// LD-2-coverage: full        // or partial, wontfix-OS, wontfix-infra
```

**Step 4 — Run the scanner to refresh §2:**
```bash
bash .claude-runtime/scripts/development-md-scan.sh --workspace mbs/kmp-toolkit --apply
```
The scanner reads the comment from any `.kt` file under the platform's source-set dir; falls back to a legacy stub/real heuristic when absent. The §2 Coverage column updates in place.

**Step 5 — Add Fake-backed contract tests in `commonTest`.** Mirror `FakeIntentLauncher` + `IntentLauncherContractTest`. Verify your impl returns the same sealed-result types as the Fake's scripted results (Ok / Cancelled / Failed(typed cause)).

**Step 6 — Surface in the sample.** Add a button to `samples/sample-cmp-intent-launcher/composeApp/.../TryItPanel.kt` exercising the new platform's path. Manual smoke-test via `./gradlew :samples:sample-cmp-intent-launcher:composeApp:run` on JVM (or the equivalent per-platform run task per the sample README).

**Step 7 — Document any new ADR.** If this graduation reverses a prior `wontfix-*` row (e.g. Apple ships PHPicker on tvOS), supersede ADR-09 with a new ADR explaining what changed.

### Recipe: Add a new IntentLauncher overload / API surface

1. Add the `public expect fun newSurface(...)` to `commonMain/IntentLauncher.kt` (or a new top-level file in the same package).
2. Add an `actual` per platform in each `{platform}Main/IntentLauncher.{platform}.kt`. Apple platforms commonly share via `iosMain` + `macosMain` inheritance; verify `appleMain` doesn't already cover both.
3. Update the `IntentBuilder` fluent methods if the surface introduces new builder fields. Keep backing properties `internal var` — never expose them as `public var`.
4. Refresh the BCV baseline: `./gradlew :cmp-intent-launcher:apiDump`. Inspect `api/*.api` diff; commit alongside the impl.
5. Add a contract test exercising the new surface against `FakeIntentLauncher`.

### Recipe: Add a new variant under an existing platform (e.g. tvosArm64)

1. Add the target to `build.gradle.kts` `kotlin { ... }` block.
2. Verify the existing platform-group source-set (e.g. `tvosMain`) carries the actual; if the new variant needs distinct logic (rare), add a separate source-set.
3. Run `:cmp-intent-launcher:assemble` to verify the new variant compiles.
4. Re-run the scanner to refresh §2 with the new row.

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-intent-launcher` — refresh via `gh pr list --label cmp-intent-launcher` then re-run scan) | — | — | — | — |

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
| ADRs | **[ADR-09 — Platform coverage decisions](docs/ADR-09-platform-coverage.md)** (2026-06-01) — locks tvOS pickers (wontfix-OS), mingw Win32 cinterop (wontfix-infra), watchOS pickers (wontfix-OS). |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |
