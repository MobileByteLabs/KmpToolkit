---
module: cmp-remote-config-compose
artifact: io.github.mobilebytelabs:cmp-remote-config-compose
version: UNKNOWN
package: com.mobilebytelabs.remoteconfig  # ui / dynamic / di / dispatch — same root as the core module
api_tier: experimental
last_reviewed: 2026-09-12
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-remote-config-compose — Development

> Single source of truth for development state of `cmp-remote-config-compose` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-remote-config-compose` | `com.mobilebytelabs.kmptoolkit.remote.config.compose` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-remote-config-compose) | 2026-09-12 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-09-12. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-remote-config-compose/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-09-13 | — | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
fun RemoteConfigHost(
internal fun RemoteConfigDialog(
internal fun RemoteConfigBanner(config: RemoteConfig, onPrimaryAction: () -> Unit, onDismiss: () -> Unit) {
internal fun RemoteConfigFullScreen(
class RemoteConfigViewModel(
internal fun RemoteConfigBottomSheet(
fun Module.remoteConfig(block: RemoteConfigBuilder.() -> Unit) {
class RemoteConfigBuilder internal constructor() {
fun DynamicUiRenderer(node: UiNode, onAction: (UiAction) -> Unit) {
internal object ActionDispatcher {
```

---

## §4 Spec Snapshot (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-09-12 -->

**Problem this module solves:** _TBD by author._

**Core invariants:**
- _TBD by author._

**Out of scope (by design):**
- _TBD by author._

---

## §5 Extension Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-09-12 -->

### Recipe: Add a new platform actual

1. _TBD by author._
2. _TBD by author._
3. _TBD by author._

### Recipe: Extend the public API

1. _TBD by author._
2. _TBD by author._

### Recipe: Add a new variant under an existing platform (e.g. tvosArm64)

1. _TBD by author._
2. _TBD by author._

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-remote-config-compose` — refresh via `gh pr list --label cmp-remote-config-compose` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-09-12 -->

### Pattern: _Pattern name TBD_

**When to use:** _TBD_
**Code shape:**
```kotlin
// TBD
```

---

## §8 Related

- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — **single source of truth** for which KMP targets
  this module must ship (21 headless / 7 Compose) and how to handle a dependency that blocks one.
  Upstream reference: <https://kotlinlang.org/docs/native-target-support.html>.

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) + [RULE-LIB-OBSERVABILITY-SURFACE-001](../../../../../../layers/framework/rules/RULE-LIB-OBSERVABILITY-SURFACE-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d).

**This module reports nothing, by design.** Every composable here delegates to
[`cmp-remote-config`](../cmp-remote-config/DEVELOPMENT.md), which reports the operation. Reporting again in the
Compose wrapper would emit two events for one user action and double every count a
consumer's hook sees.

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0–T4 | delegated | see [`cmp-remote-config`](../cmp-remote-config/DEVELOPMENT.md) §9 |

```yaml
tiers:
  T0: delegated
  T1: delegated
  T2: delegated
  T3: delegated
  T4: delegated
delegates_to: cmp-remote-config
```
