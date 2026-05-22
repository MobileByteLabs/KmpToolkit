# Cookbook — Receipt

80mm thermal-printer-style receipt with merchant header, line items, totals, payment method, and footer.

```kotlin
@OptIn(ExperimentalPdfGeneratorApi::class)
suspend fun generateReceiptPdf(generator: PdfGenerator, dest: PdfOutput = PdfOutput.Share) {
    val receipt = ReceiptData(
        merchantName    = "Sample Coffee Co",
        merchantAddress = "123 Bean Street, Caffeinated City",
        receiptNumber   = "R-0042",
        date            = LocalDate(2026, 5, 22),
        items = listOf(
            ReceiptLineItem("Espresso (large)", "$4.50"),
            ReceiptLineItem("Croissant",        "$3.25"),
            ReceiptLineItem("Tip",              "$0.75"),
        ),
        subtotal       = "$8.50",
        tax            = "$0.85",
        total          = "$9.35",
        paymentMethod  = "Card ****4242",
        footer         = "Thank you for visiting!",
    )

    val branding = PdfBranding.default()
    val template = ReceiptTemplate(branding, receipt)
    val html = template.generateHtml()

    val result = generator.generateFromHtml(
        html = html,
        output = dest,
        pageConfig = PageConfig(
            customSize = CustomPageSize(widthMm = 80, heightMm = 297), // narrow + tall
            margins = EdgeMargins.uniform(4),
        ),
    )

    when (result) {
        is PdfResult.Success -> println("Receipt PDF: ${result.byteCount} bytes")
        is PdfResult.Failure -> println("Failed: ${result.error.message}")
    }
}
```

## Print directly to a thermal printer

```kotlin
generator.generateFromHtml(
    html = html,
    output = PdfOutput.Print, // pops platform print dialog
    pageConfig = PageConfig(customSize = CustomPageSize(80, 297)),
)
```

Android: the system print dialog routes to any installed Bluetooth thermal printer.
iOS: `UIPrintInteractionController` includes AirPrint compatibility.
JVM: standard print queue picker.
