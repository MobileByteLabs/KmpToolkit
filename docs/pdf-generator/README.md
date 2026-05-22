# cmp-pdf-generator

Cross-platform PDF generation library — published as part of `kmp-toolkit`.

> Module README + cookbook + ADRs live at [`/cmp-pdf-generator/`](../../cmp-pdf-generator/).

## In a nutshell

```kotlin
val generator = createPdfGenerator(context)  // Android — or PdfGenerator() elsewhere
val doc = pdf {
    branding(PdfBranding.default())
    page {
        heading(1, "Hello")
        text("World")
    }
}
val result = generator.generate(doc, PdfOutput.Share)
```

## Three input modes

1. **HTML** — `generateFromHtml(html, output)` — pixel-perfect when fidelity matters
2. **Markdown** — `MarkdownPdfAdapter.markdownToHtml(md, branding)` → `generateFromHtml`
3. **DSL** — `pdf { page { … } }` — programmatic, type-safe, no HTML

## Six output destinations

`PdfOutput.File`, `.ByteArrayOutput`, `.Uri(callback)`, `.Share`, `.Print`, `.Save`.

## Five pre-built templates

`InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`, `StatementTemplate`, `LetterTemplate`.

## Platform support

| Platform | Tier | Engine |
|----------|:----:|--------|
| Android | 1 | WebView + PrintManager / native PdfDocument |
| iOS 14+ | 1 | WKWebView.createPDF / PDFKit |
| macOS 11+ | 1 | WKWebView.createPDF / PDFKit |
| JVM | 1 | OpenHTMLToPDF / Apache PDFBox |
| JS (browser+Node) | 1 | iframe+print / pdf-lib |
| wasmJs | 1 | iframe+print / pdf-lib |
| tvOS, watchOS, Linux, mingw, wasmWasi | 3 | throws `PdfError.UnsupportedPlatform` |

## Links

- [Full README](../../cmp-pdf-generator/README.md)
- [SPEC](../../idea-layer/modules/cmp-pdf-generator/SPEC.md) — design intent
- [API](../../idea-layer/modules/cmp-pdf-generator/API.md) — public symbols
- [ADRs](../../idea-layer/modules/cmp-pdf-generator/adrs/) — engine choice, branding, error model, tiers, DSL shape
- [Cookbook](../../cmp-pdf-generator/docs/cookbook/) — invoice, receipt, markdown, DSL, image, composable
- [Migration from mifos-x](../../cmp-pdf-generator/docs/migration/from-mifos-x.md)
