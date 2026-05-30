---
module: cmp-network-monitor
artifact: io.github.mobilebytelabs:cmp-network-monitor
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.network.monitor
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-network-monitor — Development

> Single source of truth for development state of `cmp-network-monitor` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-network-monitor` | `com.mobilebytelabs.kmptoolkit.network.monitor` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-network-monitor) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-network-monitor/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| androidMain | ✅ | ✅ real | 0 | 7 | 2026-05-30 | — |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| jvmMain | ✅ | ✅ real | 0 | 5 | 2026-05-30 | — |
| jsMain | ✅ | ✅ real | 0 | 5 | 2026-05-30 | — |
| wasmJsMain | ✅ | ✅ real | 0 | 5 | 2026-05-30 | — |
| mingwMain | ✅ | ✅ real | 0 | 5 | 2026-05-30 | — |
| linuxMain | ✅ | ✅ real | 0 | 5 | 2026-05-30 | — |
| tvosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| watchosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
sealed class BackoffStrategy {
class RetryPolicyBuilder {
fun NetworkInfo.toQuality(): NetworkQuality {
fun NetworkStatus.toQuality(): NetworkQuality = when (this) {
fun provideNetworkMonitor(config: NetworkMonitorConfig? = null): NetworkMonitor =
fun NetworkMonitor.requireOnline() {
fun NetworkMonitor.onlyWhileOnline() = networkStatus.transformWhile { status ->
fun NetworkMonitor.isConnectedVia(type: NetworkType): Boolean {
fun NetworkMonitor.isOnlineDebounced(timeoutMs: Long = 300L): Flow<Boolean> =
fun NetworkMonitor.debouncedNetworkStatus(timeoutMs: Long = 300L): Flow<NetworkStatus> =
fun NetworkMonitor.isOnlineDebouncedState(scope: CoroutineScope, timeoutMs: Long = 300L): StateFlow<Boolean> = isOnline
fun NetworkMonitor.networkStatusDebouncedState(
fun NetworkMonitor.networkQuality(): Flow<NetworkQuality> = networkStatus.map { it.toQuality() }.distinctUntilChanged()
fun <T> Flow<T>.withNetworkState(monitor: NetworkMonitor): Flow<Pair<T, NetworkStatus>> =
fun <T> Flow<T>.onlyWhileOnline(monitor: NetworkMonitor): Flow<T> = combine(this, monitor.isOnline) { value, online ->
fun NetworkMonitor.addCallback(
fun interface CallbackHandle {
class NetworkUnavailableException(
sealed class NetworkStatus {
sealed class CaptivePortalResult {
class FakeNetworkMonitor(
internal class StubNetworkMonitor : NetworkMonitor {
interface NetworkMonitor {
object NetworkMonitorProvider {
fun createScopedNetworkMonitor(
internal class AdaptivePollingState(
internal class ValidationBackoffState(
sealed class NetworkConstraint {
fun NetworkMonitor.checkConstraints(vararg constraints: NetworkConstraint): Boolean {
sealed class NetworkChangeEvent {
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
| (no open PRs labeled `cmp-network-monitor` — refresh via `gh pr list --label cmp-network-monitor` then re-run scan) | — | — | — | — |

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
