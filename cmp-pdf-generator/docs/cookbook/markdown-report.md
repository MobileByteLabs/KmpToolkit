# Cookbook — Markdown → PDF

Compile Markdown source to a styled PDF using `MarkdownPdfAdapter`. Uses GFM (GitHub-Flavored Markdown).

## Quick start

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
suspend fun markdownToPdf(generator: PdfGenerator, source: String): ByteArray? {
    val html = MarkdownPdfAdapter.markdownToHtml(
        markdown = source,
        branding = PdfBranding.default(),
    )
    val result = generator.generateFromHtml(
        html = html,
        output = PdfOutput.ByteArrayOutput,
        pageConfig = PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(20)),
    )
    return (result as? PdfResult.Success)?.bytes
}
```

## Supported Markdown features

| Feature | Status |
|---------|--------|
| Headings (h1-h6) | ✅ |
| Bold + italic | ✅ |
| Code blocks (fenced) | ✅ — monospace, gray background |
| Inline code | ✅ |
| Lists (ordered, unordered, nested) | ✅ |
| GFM tables | ✅ |
| Hyperlinks | ✅ |
| Images | ✅ URL only (no embedded base64) |
| Blockquotes | ✅ — colored left border |
| Strikethrough | ✅ |
| HTML passthrough | ⚠️ sanitized (no `<script>`, `<iframe>`, `on*=`) |
| Mermaid / math | ❌ not supported (v2) |

## Branded version

Wrap with your brand:

```kotlin
val branding = PdfBranding(
    logo = PdfLogo.Svg(myLogoBytes),
    poweredByText = "Sample Co",
    theme = PdfTheme(
        accentColorHex = "#0066CC",
        fontFamily = "Roboto, sans-serif",
    ),
)
val html = MarkdownPdfAdapter.markdownToHtml(markdown, branding)
```

## Long-form report tips

For multi-section reports:
- Use `<div style="page-break-before: always"></div>` inline to force page breaks
- Repeat the table header on each new page automatically when overflow occurs
- Generate ToC from headings manually (post-parse pass — v2 feature on the roadmap)
