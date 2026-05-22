# Cookbook — DSL `pdf { … }`

The DSL route produces PDFs programmatically without an HTML round-trip. Useful when:
- You need raw bytes (`PdfOutput.ByteArrayOutput`) for server upload
- You want fine control over page breaks and table layout
- HTML is overkill (a receipt, a label, a checklist)

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
suspend fun generateReport(): ByteArray? {
    val doc = pdf {
        pageConfig(PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(20)))
        branding(PdfBranding.default())

        page {
            heading(1, "Q1 Report")
            text("Generated programmatically via the cmp-pdf-generator DSL.")
            spacer(8)

            heading(2, "Key metrics")
            table {
                header { cell("Metric"); cell("Value"); cell("Trend") }
                row { cell("Active users"); cell("12,345"); cell("+8%") }
                row { cell("Revenue");      cell("$50K");   cell("+12%") }
                row { cell("Churn");        cell("3.2%");   cell("-0.4%") }
            }
            divider()
            text("Footnote: data as of 2026-05-22", TextStyle(italic = true, size = 6))
        }

        page {
            heading(1, "Appendix A — Glossary")
            text("MAU: Monthly Active Users")
            text("DAU: Daily Active Users")
            text("ARR: Annual Recurring Revenue")
        }
    }

    val gen = PdfGenerator()  // JVM example; on Android use createPdfGenerator(context)
    return when (val r = gen.generate(doc, PdfOutput.ByteArrayOutput)) {
        is PdfResult.Success -> r.bytes
        is PdfResult.Failure -> { println(r.error.message); null }
    }
}
```

## All DSL primitives

```kotlin
page {
    heading(1, "H1") ; heading(2, "H2") ; heading(3, "H3")
    text("paragraph")
    text("styled", TextStyle(bold = true, italic = true, size = 10, colorHex = "#FF0000"))
    image(ImageSource.Bytes(pngBytes), widthMm = 40, heightMm = 40)
    image(ImageSource.Url("https://example.com/chart.png"))
    table {
        header { cell("A"); cell("B"); cell("C") }
        row { cell("1"); cell("2", colSpan = 2) }
    }
    spacer(mm = 8)
    divider()
    pageBreak()
    html("<p>fallback to HTML engine for this region</p>")
}
```

## Mixing HTML + DSL

If a `page { … }` contains any `PdfElement.Html` element, the generator routes the entire document through the HTML engine (better fidelity for complex content). Pure-DSL documents use the platform's native path (Android `PdfDocument`, JVM PDFBox direct, iOS `WKWebView`, JS pdf-lib).
