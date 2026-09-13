---
module: cmp-pdf-generator
artifact: io.github.mobilebytelabs:cmp-pdf-generator
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.pdf.generator
api_tier: stable  # @ExperimentalPdfGeneratorApi retained as a deprecated no-op for source compat
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-pdf-generator — Development

> Single source of truth for development state of `cmp-pdf-generator` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-pdf-generator` | `com.mobilebytelabs.kmptoolkit.pdf.generator` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-pdf-generator) | 2026-05-30 | stable |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-pdf-generator/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 4 | 2026-09-13 | (legacy:full) | — |
| iosMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | (legacy:full) | — |
| macosMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | (legacy:full) | — |
| jvmMain | ✅ | ✅ real | 0 | 3 | 2026-09-13 | (legacy:full) | — |
| jsMain | ✅ | ✅ real | 0 | 4 | 2026-09-13 | (legacy:full) | — |
| wasmJsMain | ✅ | ✅ real | 0 | 2 | 2026-09-13 | (legacy:full) | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
internal fun TextStyle.toCssStyle(): String {
internal fun ImageSource.toSrc(): String = when (this) {
public object MarkdownPdfAdapter {
public sealed class PdfLogo {
public fun defaultDateFormat(date: LocalDate): String {
public sealed class ImageSource {
public sealed class PdfElement {
public sealed class PdfProgressEvent {
public class ReportTemplate(branding: PdfBranding, public val report: ReportData) : HtmlTemplateGenerator(branding) {
public class ReceiptTemplate(branding: PdfBranding, public val receipt: ReceiptData) :
public class StatementTemplate(branding: PdfBranding, public val statement: StatementData) :
public class LetterTemplate(branding: PdfBranding, public val letter: LetterData) : HtmlTemplateGenerator(branding) {
public class InvoiceTemplate(branding: PdfBranding, public val invoice: InvoiceData) :
public class PdfDocumentBuilder internal constructor() {
public class PdfPageBuilder internal constructor() {
public class TableBuilder internal constructor() {
public class TableRowBuilder internal constructor() {
public fun pdf(block: PdfDocumentBuilder.() -> Unit): PdfDocument {
public sealed class PdfOutput {
public sealed class PdfResult {
internal fun emptyProgressFlow(): Flow<PdfProgressEvent> = emptyFlow()
internal fun String.injectPageConfigCss(pageConfig: PageConfig): String {
public sealed class PdfError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
public fun Throwable.toPdfError(): PdfError = when (this) {
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
| (no open PRs labeled `cmp-pdf-generator` — refresh via `gh pr list --label cmp-pdf-generator` then re-run scan) | — | — | — | — |

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

- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — **single source of truth** for which KMP targets
  this module must ship (21 headless / 7 Compose) and how to handle a dependency that blocks one.
  Upstream reference: <https://kotlinlang.org/docs/native-target-support.html>.

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d). Wired 2026-09-13, when `cmp-observe`
reached all 21 targets and every module could depend on it from `commonMain`.

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0 (Crashlytics attribution) | enabled | `custom_key: library:cmp-pdf-generator@<version>` — set by `FirebaseCrashlyticsAttributionHook` (`cmp-observe-firebase`) |
| T1 (init + version health) | n/a | not applicable — this module has no initialisation step of its own |
| T2 (lifecycle events) | enabled | `pdf_generated` |
| T3 (performance traces) | opted-out | opt-in per consumer; `FirebasePerformanceHook` wraps init |
| T4 (full API usage) | opted-out | opt-in per consumer + per end-user; iOS ATT prompt required |

**Payload policy.** Events carry operation *shape*, never operation *content*. Enforced by review,
and by `.github/scripts/assert-observability-wired.sh` refusing a module that generates
`CmpMetadata` but never reports.

```yaml
# DEVELOPMENT_OBSERVABILITY.schema.yaml-conformant block
tiers:
  T0: enabled
  T1: not-applicable
  T2: enabled
  T3: opted-out
  T4: opted-out
event_schema:
  reports_init: false
  lifecycle_events:
    - pdf_generated
```
