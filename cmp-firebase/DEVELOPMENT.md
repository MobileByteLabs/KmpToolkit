---
module: cmp-firebase
artifact: io.github.mobilebytelabs:cmp-firebase
version: UNKNOWN
package: io.github.mobilebytelabs.kmptoolkit.firebase
api_tier: experimental
last_reviewed: 2026-08-10
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-firebase — Development

> Single source of truth for development state of `cmp-firebase` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-firebase` | `io.github.mobilebytelabs.kmptoolkit.firebase` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** Unified Firebase for Kotlin Multiplatform — **Analytics + Crashlytics** behind one in-library setup surface (`FirebaseKit`). Analytics uses GitLive on 11 targets with a Measurement-Protocol HTTP fallback elsewhere; Crashlytics uses GitLive on its 6 supported targets (Android + iOS + macOS) with a structured, AI-feedable `CrashReport` (logged as JSON via Kermit) on the other 13. Every crash tier produces the same `CrashReport` — exception class, message, full cause chain, and `file:line` stack frames — serializable for hand-off to an AI. Android needs zero setup code (auto-init `ContentProvider`); other platforms call `FirebaseKit.initialize()` once. Renamed from `cmp-firebase-analytics` on 2026-08-10.

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| jvmMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| jsMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| wasmJsMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| mingwMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| linuxMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| tvosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |
| watchosMain | ✅ | ✅ real | 0 | 1 | 2026-06-01 | (legacy:full) | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
object AnalyticsModule {
class StubAnalyticsHelper(
internal fun AnalyticsEvent.withPlatform(platform: String): AnalyticsEvent =
class TestAnalyticsHelper : AnalyticsHelper {
object NoOpAnalyticsHelper : AnalyticsHelper {
fun createParam(key: String, value: String): Param? = runCatching { Param(key, value) }.getOrNull()
object EventTypes {
object ParamKeys {
class MeasurementProtocolAnalyticsHelper(
internal fun loadOrCreateClientId(settings: Settings): String {
interface AnalyticsHelper {
class EventValidator(
class RegistryValidatingHelper(
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
| (no open PRs labeled `cmp-firebase` — refresh via `gh pr list --label cmp-firebase` then re-run scan) | — | — | — | — |

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
