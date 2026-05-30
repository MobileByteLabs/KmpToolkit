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

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| androidMain | ✅ | ✅ real | 0 | 6 | 2026-05-30 | — |
| iosMain | 🟡 | 🟡 stub | 3 | 2 | 2026-05-30 | — |
| macosMain | 🟡 | 🟡 stub | 3 | 2 | 2026-05-30 | — |
| jvmMain | 🟡 | 🟡 stub | 3 | 2 | 2026-05-30 | — |
| jsMain | 🟡 | 🟡 stub | 5 | 2 | 2026-05-30 | — |
| wasmJsMain | 🟡 | 🟡 stub | 7 | 2 | 2026-05-30 | — |
| mingwMain | 🟡 | 🟡 stub | 5 | 2 | 2026-05-30 | — |
| linuxMain | 🟡 | 🟡 stub | 3 | 2 | 2026-05-30 | — |
| tvosMain | 🟡 | 🟡 stub | 4 | 2 | 2026-05-30 | — |
| watchosMain | 🟡 | 🟡 stub | 5 | 2 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

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

## §5 Extension Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

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
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |
