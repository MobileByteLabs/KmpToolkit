# cmp-pdf-generator

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Cross-platform PDF generation library for Kotlin Multiplatform.

> **Stable API.** `@ExperimentalPdfGeneratorApi` is retained as a deprecated no-op so existing
> `@OptIn(...)` call sites keep compiling; both it and the `-opt-in` compiler flag are now
> redundant and can be deleted.
> Ships alongside the other `cmp-*` modules at the shared `kmptoolkit.version`.

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
| wasmJs (Browser+Node) | iframe + `window.print()` | Deferred (pdf-lib wasmJs interop pending) | HTML route only in v1; needs `kotlinx-browser` dep |

> **Not targeted:** tvOS, watchOS, Linux native, mingwX64, wasmWasi.
> Per the [Kotlin Multiplatform target tiers](https://kotlinlang.org/docs/native-target-support.html),
> these are Tier-2 / Tier-3 native targets. `kotlinx-html` and `org.intellij.markdown` —
> our HTML compiler + Markdown adapter dependencies — don't publish artifacts there.
> Adding them later requires upstream library coverage first.

## Install

```kotlin
// build.gradle.kts (your consumer app)
dependencies {
    val kmptoolkit = "3.2.8" // or latest — see https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-pdf-generator
    implementation("io.github.mobilebytelabs:cmp-pdf-generator:$kmptoolkit")
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

## Dependency injection

`pdfModule` binds `PdfManager`, so a ViewModel or repository can inject it instead of reaching for the
top-level entry points. A `single`: the generator carries a progress flow callers subscribe to, and a second instance would emit to nobody.

```kotlin
startKoin { modules(pdfModule) }

class MyViewModel(private val manager: PdfManager) : ViewModel()
```

## Docs

- [DEVELOPMENT.md](DEVELOPMENT.md) — public API surface, per-platform parity, engine choices
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — which targets render HTML and which fall back to `TextPdfWriter`
- [Cookbook](docs/cookbook/) — invoice, receipt, report, image, composable-snapshot recipes
- [Migration from mifos-x](docs/migration/from-mifos-x.md)

## License

Apache 2.0 — see [LICENSE](../LICENSE).
