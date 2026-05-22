/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfBranding
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceLineItem
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.PartyInfo
import kotlinx.datetime.LocalDate

/** Sample invoice fixture for demo + golden snapshot tests. */
object InvoiceFixture {
    val sampleInvoice =
        InvoiceData(
            invoiceNumber = "INV-2026-0042",
            invoiceDate = LocalDate(2026, 5, 22),
            dueDate = LocalDate(2026, 6, 21),
            billFrom =
                PartyInfo(
                    name = "Sample Co Ltd",
                    addressLines = listOf("123 Sample Street", "Sampleville, SV 12345"),
                    email = "billing@sampleco.example",
                    taxId = "TAX-001-XYZ",
                ),
            billTo =
                PartyInfo(
                    name = "Acme Corp",
                    addressLines = listOf("456 Acme Avenue", "Anvil City, AC 67890"),
                    email = "ap@acmecorp.example",
                ),
            lineItems =
                listOf(
                    InvoiceLineItem("KMP PDF Generator integration", "1", "$2,500.00", "$2,500.00"),
                    InvoiceLineItem("Custom Branding Setup", "1", "$500.00", "$500.00"),
                    InvoiceLineItem("Support hours", "10", "$150.00", "$1,500.00"),
                ),
            subtotal = "$4,500.00",
            tax = "$450.00 (10%)",
            total = "$4,950.00",
            notes = "Thank you for your business.",
            terms = "Net 30. Late fee 1.5% per month.",
        )

    fun template(branding: PdfBranding = PdfBranding.default()) = InvoiceTemplate(branding, sampleInvoice)
}
