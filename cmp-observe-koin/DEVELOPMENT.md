---
module: cmp-observe-koin
artifact: io.github.mobilebytelabs:cmp-observe-koin
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.observe.koin
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-observe-koin — Development

> Single source of truth for development state of `cmp-observe-koin` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-observe-koin` | `com.mobilebytelabs.kmptoolkit.observe.koin` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-observe-koin) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-observe-koin/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
public fun observeKoinModule(
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
| (no open PRs labeled `cmp-observe-koin` — refresh via `gh pr list --label cmp-observe-koin` then re-run scan) | — | — | — | — |

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
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) + [RULE-LIB-OBSERVABILITY-SURFACE-001](../../../../../../layers/framework/rules/RULE-LIB-OBSERVABILITY-SURFACE-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30. Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d). -->

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-observe-koin@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
| T1 (config + version health)  | enabled | events: `lib_init_success`, `lib_init_failure` (FirebaseAnalyticsHealthHook) |
| T2 (lifecycle events)         | opted-out | (author when ready — populate event_schema YAML below + flip to enabled) |
| T3 (performance traces)       | opted-out | (opt-in per consumer; FirebasePerformanceHook wraps `*_start` / `*_end` lifecycle events) |
| T4 (full API usage)           | opted-out | opt-in per consumer + per end-user; iOS ATT prompt required |

```yaml
# DEVELOPMENT_OBSERVABILITY.schema.yaml-conformant block
tiers:
  T0: enabled
  T1: enabled
  T2: opted-out
custom_key_format: "library:cmp-observe-koin@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-observe-koin.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-observe-koin.observability_opt_in=true` in consumer's `lib-integrate.properties`.
