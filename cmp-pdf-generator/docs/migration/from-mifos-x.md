# Migration: mifos-x `core/ui/util/pdf` → `cmp-pdf-generator`

Apps currently using the private `core/ui/util/pdf` utility in `mifos-x-field-officer-app` or `mifos-mobile` can adopt the public `cmp-pdf-generator` library by:

## 1. Add the dependency

```kotlin
// build.gradle.kts — uses the shared kmp-toolkit version
dependencies {
    val kmptoolkit = "3.2.8" // or latest from Maven Central
    implementation("io.github.mobilebytelabs:cmp-pdf-generator:$kmptoolkit")
}
```

## 2. Class mapping

| Mifos-x class | cmp-pdf-generator equivalent |
|---|---|
| `com.mifos.core.ui.util.pdf.PdfGenerator` | `com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGenerator` |
| `com.mifos.core.ui.util.pdf.PdfGeneratorHelper.rememberPdfGenerator()` | `createPdfGenerator(context)` (Android) / `PdfGenerator()` (other) |
| `com.mifos.core.ui.util.pdf.PageConfig` | `com.mobilebytelabs.kmptoolkit.pdfgenerator.PageConfig` |
| `com.mifos.core.ui.util.pdf.PageSize` | `com.mobilebytelabs.kmptoolkit.pdfgenerator.PageSize` (+ A3, A5, B5, TABLOID, STATEMENT) |
| `com.mifos.core.ui.util.pdf.Orientation` | `com.mobilebytelabs.kmptoolkit.pdfgenerator.Orientation` |
| `com.mifos.core.ui.util.pdf.HtmlTemplateGenerator` | `com.mobilebytelabs.kmptoolkit.pdfgenerator.HtmlTemplateGenerator` — **now takes `PdfBranding` constructor arg** |

## 3. Re-enable mifos branding

The reference base class had a hard-coded mifos logo and `Res.string.powered_by`. To preserve that branding in your migrated app, inject:

```kotlin
val mifosBranding = PdfBranding.mifosDefault(
    logo = PdfLogo.Svg(yourMifosSvgBytes),
)
class YourTemplate : HtmlTemplateGenerator(mifosBranding) {
    override fun getTitle() = "..."
    override fun BODY.generateBody() = ...
}
```

`mifosDefault()` reuses the mifos blue palette (`#33618D`, `#1976d2`) so visual output should match the previous private utility.

## 4. Margins: `marginMm: Int` → `EdgeMargins`

Old:
```kotlin
PageConfig(marginMm = 8)
```

New:
```kotlin
PageConfig(margins = EdgeMargins.uniform(8))
// or per-edge:
PageConfig(margins = EdgeMargins(top = 10, right = 8, bottom = 10, left = 8))
```

## 5. iOS impl is now real

The reference had `PdfGenerator.native.kt` as a `TODO()` stub. The library impl uses `WKWebView.createPDF` (iOS 14+) — works without code changes.

## 6. New surface to consider

After migrating, you also gain:

- **`PdfOutput.ByteArrayOutput`** — get raw bytes for server upload
- **`Flow<PdfProgressEvent>`** — show progress during large doc render
- **DSL route** — `pdf { page { … } }` for programmatic PDFs without HTML
- **`MarkdownPdfAdapter`** — Markdown → PDF
- **Pre-built templates** — `InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`, `StatementTemplate`, `LetterTemplate`

## 7. Delete the private utility

Once your app is on the library:

```bash
rm -rf core/ui/src/{commonMain,androidMain,desktopMain,iosMain,jsMain,wasmJsMain,nativeMain}/kotlin/com/mifos/core/ui/util/pdf/
```

…and remove any `kotlinx-html`, `openhtmltopdf-pdfbox`, `openhtmltopdf-svg-support` deps that were only there for PDF — they're transitively pulled by `cmp-pdf-generator`.
