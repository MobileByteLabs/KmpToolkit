---
module: cmp-pdf-generator
artifact: io.github.mobilebytelabs:cmp-pdf-generator
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.pdf.generator
api_tier: experimental
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
| `io.github.mobilebytelabs:cmp-pdf-generator` | `com.mobilebytelabs.kmptoolkit.pdf.generator` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-pdf-generator) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-pdf-generator/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| androidMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| macosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| jvmMain | ✅ | ✅ real | 0 | 2 | 2026-05-30 | — |
| jsMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |
| wasmJsMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

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

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |
