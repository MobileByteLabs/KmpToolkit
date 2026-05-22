# Cookbook — Invoice

End-to-end recipe for generating an invoice PDF.

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
suspend fun generateInvoicePdf(): ByteArray? {
    // 1. Build the data
    val invoice = InvoiceData(
        invoiceNumber = "INV-2026-0042",
        invoiceDate = LocalDate(2026, 5, 22),
        dueDate = LocalDate(2026, 6, 21),
        billFrom = PartyInfo(
            name = "Sample Co Ltd",
            addressLines = listOf("123 Sample Street", "Sampleville, SV 12345"),
            email = "billing@sampleco.example",
            taxId = "TAX-001-XYZ",
        ),
        billTo = PartyInfo(
            name = "Acme Corp",
            addressLines = listOf("456 Acme Avenue", "Anvil City, AC 67890"),
        ),
        lineItems = listOf(
            InvoiceLineItem("Integration",  "1",  "$2,500.00", "$2,500.00"),
            InvoiceLineItem("Custom branding", "1", "$500.00",  "$500.00"),
            InvoiceLineItem("Support hours", "10", "$150.00",  "$1,500.00"),
        ),
        subtotal = "$4,500.00",
        tax = "$450.00 (10%)",
        total = "$4,950.00",
        terms = "Net 30. Late fee 1.5% per month.",
    )

    // 2. Inject your branding (logo + colors)
    val branding = PdfBranding(
        logo = PdfLogo.Svg(MyLogoSvgBytes),
        poweredByText = "Sample Co",
        theme = PdfTheme(accentColorHex = "#0066CC", headerColorHex = "#003D7A"),
    )

    // 3. Compose the template → HTML
    val template = InvoiceTemplate(branding, invoice)
    val html = template.generateHtml()

    // 4. Generate PDF bytes
    val generator = createPdfGenerator(context = appContext) // Android
    // val generator = PdfGenerator()                          // JVM
    val result = generator.generateFromHtml(
        html = html,
        output = PdfOutput.ByteArrayOutput,
        pageConfig = PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(15)),
    )
    return when (result) {
        is PdfResult.Success -> result.bytes
        is PdfResult.Failure -> { logError(result.error); null }
    }
}
```

## Showing progress

```kotlin
viewModelScope.launch {
    generator.progressFlow().collect { event ->
        when (event) {
            is PdfProgressEvent.Started -> uiState.update { it.copy(progress = 0f) }
            is PdfProgressEvent.PageRendered ->
                uiState.update { it.copy(progress = event.pageNum.toFloat() / (event.total ?: 10)) }
            is PdfProgressEvent.Complete -> uiState.update { it.copy(progress = 1f, doneByteCount = event.byteCount) }
            is PdfProgressEvent.Failed -> showError(event.error)
            else -> Unit
        }
    }
}
val result = generator.generate(doc, PdfOutput.ByteArrayOutput)
```

## Sharing on Android

```kotlin
val result = generator.generate(doc, PdfOutput.Share)
// presents native share sheet via FileProvider — make sure your app declares the
// `${applicationId}.fileprovider` authority in AndroidManifest.xml.
```
