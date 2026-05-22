# Cookbook — Images to PDF

Bundle a sequence of images (photos, scans) into a single PDF — each image on its own page.

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
suspend fun imagesToPdf(generator: PdfGenerator, images: List<ByteArray>): ByteArray? {
    val doc = pdf {
        pageConfig(PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(10)))
        branding(PdfBranding.none())

        images.forEachIndexed { idx, bytes ->
            page {
                if (idx > 0) { /* page() already starts a new page */ }
                image(
                    source = ImageSource.Bytes(bytes),
                    widthMm = 190,
                    heightMm = 277,
                )
            }
        }
    }
    val r = generator.generate(doc, PdfOutput.ByteArrayOutput)
    return (r as? PdfResult.Success)?.bytes
}
```

## Mixed image + caption

```kotlin
val doc = pdf {
    pageConfig(PageConfig(size = PageSize.A4))
    branding(PdfBranding.none())
    photos.forEach { (caption, bytes) ->
        page {
            image(ImageSource.Bytes(bytes), widthMm = 180, heightMm = 180)
            spacer(4)
            text(caption, TextStyle(italic = true, alignment = Alignment.CENTER))
        }
    }
}
```

## Image source types

| Source | Where it works |
|--------|----------------|
| `ImageSource.Bytes(bytes)` | All Tier-1 platforms |
| `ImageSource.DataUri(uri)` | All Tier-1 platforms (parsed to bytes) |
| `ImageSource.Url(url)` | HTML route only — JVM/Android can fetch over HTTP; iOS/web via the embedded engine |
| `ImageSource.Resource(path)` | HTML route only — resolves relative to the engine's base URL |

## Performance note

Each image is embedded uncompressed in v0.1. For a long scan-to-PDF workflow (>50 pages, full-resolution photos), expect 5-50 MB output PDFs. JPEG compression of source images keeps output sizes manageable.
