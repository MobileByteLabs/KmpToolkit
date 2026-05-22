# cmp-pdf-generator

Cross-platform PDF generation library for Kotlin Multiplatform.

> **Status:** v0.1.0 — Experimental. All public APIs marked `@ExperimentalPdfGeneratorApi`.

## Features

- **Input modes**: HTML string, Markdown, Composable snapshot, programmatic DSL, image
- **Output destinations**: File, ByteArray, platform URI, Share intent, Print dialog, Save dialog
- **Page configuration**: A3/A4/A5/B5/Letter/Legal/Tabloid/Statement/Custom + portrait/landscape + per-edge margins + page numbers + headers/footers
- **Branding**: injectable logo + theme colors + typography + powered-by footer (fully de-branded base; mifos defaults available via `PdfBranding.mifosDefault()`)
- **Pre-built templates**: Invoice, Report, Receipt, Statement, Letter
- **Error handling**: typed `PdfError` sealed hierarchy + cancellation + `Flow<PdfProgressEvent>` progress

## Platform support

| Platform | HTML route | DSL route | Notes |
|----------|------------|-----------|-------|
| Android | WebView + PrintManager | `android.graphics.pdf.PdfDocument` | Min SDK from kmp-toolkit policy |
| JVM (Desktop) | OpenHTMLToPDF | Apache PDFBox direct | ~10MB transitive |
| iOS | `WKWebView.createPDF` | `PDFKit` | iOS 14+ |
| macOS | `WKWebView.createPDF` | `PDFKit` | macOS 11+ |
| JS (Browser+Node) | iframe + `window.print()` | `pdf-lib` (npm) | Browser: needs user gesture for print |
| wasmJs (Browser+Node) | iframe + `window.print()` | `pdf-lib` (npm) | Browser only practical |

> **Not targeted:** tvOS, watchOS, Linux native, mingwX64, wasmWasi.
> Per the [Kotlin Multiplatform target tiers](https://kotlinlang.org/docs/native-target-support.html),
> these are Tier-2 / Tier-3 native targets. `kotlinx-html` and `org.intellij.markdown` —
> our HTML compiler + Markdown adapter dependencies — don't publish artifacts there.
> Adding them later requires upstream library coverage first.

## Install

```kotlin
// build.gradle.kts (your consumer app)
dependencies {
    implementation("io.github.mobilebytelabs:kmp-pdf-generator:0.1.0")
}
```

## Quick start

### HTML route

```kotlin
val generator = rememberPdfGenerator()
val html = "<h1>Hello</h1><p>World</p>"
val config = PageConfig(size = PageSize.A4, orientation = Orientation.PORTRAIT)
generator.generateAndSharePdf(html, fileName = "hello", pageConfig = config)
```

### DSL route

```kotlin
val document = pdf {
    pageConfig(PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(20)))
    branding(PdfBranding.none())
    page {
        heading(level = 1, "Invoice")
        text("Bill to: Acme Corp")
        table {
            row { cell("Item"); cell("Qty"); cell("Total") }
            row { cell("Widget"); cell("3"); cell("$30") }
        }
    }
}
val result: PdfResult = generator.generate(document, PdfOutput.ByteArrayOutput)
```

### Pre-built template

```kotlin
val invoice = InvoiceTemplate(
    branding = PdfBranding.none(),
    invoice = InvoiceData(/* ... */),
)
generator.generate(invoice.toDocument(), PdfOutput.Share)
```

## Docs

- [SPEC](../idea-layer/modules/cmp-pdf-generator/SPEC.md) — full design intent
- [API](../idea-layer/modules/cmp-pdf-generator/API.md) — every public symbol
- [ADRs](../idea-layer/modules/cmp-pdf-generator/adrs/) — engine choices, branding model, error model, target tiers, DSL shape
- [Cookbook](docs/cookbook/) — invoice, receipt, report, image, composable-snapshot recipes
- [Migration from mifos-x](docs/migration/from-mifos-x.md)

## License

Apache 2.0 — see [LICENSE](../../LICENSE).
