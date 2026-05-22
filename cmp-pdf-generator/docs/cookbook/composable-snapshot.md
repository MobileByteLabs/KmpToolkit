# Cookbook — Composable Snapshot to PDF

Render a Compose UI to a bitmap and embed it as an image in a PDF page. Useful for charts, custom layouts, or any UI you've already built in Compose.

## Android

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
@Composable
fun ChartSnapshotButton() {
    val context = LocalContext.current
    val generator = remember { createPdfGenerator(context) }
    val scope = rememberCoroutineScope()
    val captureController = rememberGraphicsLayer()

    Column {
        // Your chart, rendered into a graphics layer
        Box(
            modifier = Modifier
                .drawWithContent {
                    captureController.record { this@drawWithContent.drawContent() }
                    drawLayer(captureController)
                }
        ) { MyChartComposable() }

        Button(onClick = {
            scope.launch {
                val bitmap = captureController.toImageBitmap()
                val pngBytes = bitmap.toAndroidBitmap().toPngBytes()
                val doc = pdf {
                    branding(PdfBranding.default())
                    page {
                        heading(1, "Chart")
                        image(ImageSource.Bytes(pngBytes), widthMm = 180)
                    }
                }
                generator.generate(doc, PdfOutput.Share)
            }
        }) { Text("Export chart to PDF") }
    }
}

private fun android.graphics.Bitmap.toPngBytes(): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    return out.toByteArray()
}
```

## iOS

`UIView.snapshotView` + `UIGraphicsImageRenderer` → PNG → embed as `ImageSource.Bytes`.

## Web

`html2canvas` (npm) → PNG data URL → `ImageSource.DataUri`.

## Why this exists

For the cases where:
- You've already invested in a Compose chart / custom UI
- You want pixel-fidelity with the on-screen rendering
- HTML/CSS can't reproduce the layout (custom shaders, dynamic graphics)

The trade-off is: the rendered image is a raster, not text — so it won't be selectable in the PDF and won't scale crisply if the user zooms in. For text-heavy content, prefer the DSL or HTML routes.
