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

## Custom file name

`generate(...)` and `generateFromHtml(...)` take an optional `fileName` (default `"document.pdf"`)
used by the Share / Save / Print flows on every platform. A `.pdf` extension is appended
automatically when missing, so both forms are equivalent:

```kotlin
generator.generateFromHtml(html, PdfOutput.Save, fileName = "invoice-2026-0042")      // → invoice-2026-0042.pdf
generator.generateFromHtml(html, PdfOutput.Save, fileName = "invoice-2026-0042.pdf")  // → invoice-2026-0042.pdf
```

> wasmJs accepts `fileName` for API parity but ignores it — the browser controls the
> print/save dialog name.

## Five pre-built templates

`InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`, `StatementTemplate`, `LetterTemplate`.

## Platform support

| Platform | Engine |
|----------|--------|
| Android | WebView + PrintManager / native PdfDocument |
| iOS 14+ | WKWebView.createPDF / PDFKit |
| macOS 11+ | WKWebView.createPDF / PDFKit |
| JVM | OpenHTMLToPDF / Apache PDFBox |
| JS (browser+Node) | iframe+print / pdf-lib |
| wasmJs (browser+Node) | iframe+print / pdf-lib |

> Not targeted: tvOS, watchOS, Linux native, mingwX64, wasmWasi — upstream library coverage is incomplete on those.

## Links

- [Full README](../../cmp-pdf-generator/README.md)
- [SPEC](../../idea-layer/modules/cmp-pdf-generator/SPEC.md) — design intent
- [API](../../idea-layer/modules/cmp-pdf-generator/API.md) — public symbols
- [ADRs](../../idea-layer/modules/cmp-pdf-generator/adrs/) — engine choice, branding, error model, tiers, DSL shape
- [Cookbook](../../cmp-pdf-generator/docs/cookbook/) — invoice, receipt, markdown, DSL, image, composable
- [Migration from mifos-x](../../cmp-pdf-generator/docs/migration/from-mifos-x.md)
