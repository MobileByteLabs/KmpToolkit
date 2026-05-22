/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceLineItem
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.LetterData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.LetterTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.PartyInfo
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptLineItem
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReportData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReportSection
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReportTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.StatementData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.StatementTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.StatementTransaction
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class TemplateTests {
    private val party = PartyInfo("Acme", listOf("123 Main"), email = "x@acme.io")

    @Test
    fun invoiceTemplateRendersTotalsAndLineItems() = runTest {
        val data =
            InvoiceData(
                invoiceNumber = "INV-1",
                invoiceDate = LocalDate(2026, 1, 1),
                billFrom = party,
                billTo = party,
                lineItems = listOf(InvoiceLineItem("Widget", "1", "$10", "$10")),
                subtotal = "$10",
                tax = "$1",
                total = "$11",
            )
        val html = InvoiceTemplate(PdfBranding.default(), data).generateHtml()
        assertTrue(html.contains("INV-1"))
        assertTrue(html.contains("Widget"))
        assertTrue(html.contains("$11"))
        assertTrue(html.contains("Subtotal"))
        assertTrue(html.contains("Tax"))
    }

    @Test
    fun receiptTemplateRendersThermalLayout() = runTest {
        val data =
            ReceiptData(
                merchantName = "Sample Co",
                receiptNumber = "RCP-1",
                date = LocalDate(2026, 1, 1),
                items = listOf(ReceiptLineItem("Coffee", "$4")),
                subtotal = "$4",
                total = "$4",
            )
        val html = ReceiptTemplate(PdfBranding.none(), data).generateHtml()
        assertTrue(html.contains("Sample Co"))
        assertTrue(html.contains("max-width: 80mm"))
        assertTrue(html.contains("TOTAL"))
    }

    @Test
    fun reportTemplateRendersNestedSections() = runTest {
        val data =
            ReportData(
                title = "Q1 Report",
                date = LocalDate(2026, 1, 1),
                sections =
                    listOf(
                        ReportSection("Intro", "Body", subsections = listOf(ReportSection("Sub", "Inner"))),
                        ReportSection("Conclusion", "End"),
                    ),
            )
        val html = ReportTemplate(PdfBranding.default(), data).generateHtml()
        assertTrue(html.contains("Q1 Report"))
        assertTrue(html.contains("1. Intro"))
        assertTrue(html.contains("1.1. Sub"))
        assertTrue(html.contains("2. Conclusion"))
    }

    @Test
    fun statementTemplateRendersTransactionTable() = runTest {
        val data =
            StatementData(
                accountHolder = party,
                accountNumber = "ACC-1",
                periodStart = LocalDate(2026, 1, 1),
                periodEnd = LocalDate(2026, 1, 31),
                openingBalance = "$100",
                closingBalance = "$150",
                transactions =
                    listOf(
                        StatementTransaction(LocalDate(2026, 1, 5), "Deposit", credit = "$50", balance = "$150"),
                    ),
            )
        val html = StatementTemplate(PdfBranding.default(), data).generateHtml()
        assertTrue(html.contains("ACC-1"))
        assertTrue(html.contains("Deposit"))
        assertTrue(html.contains("$150"))
    }

    @Test
    fun letterTemplateRendersFormalLayout() = runTest {
        val data =
            LetterData(
                sender = party,
                recipient = party,
                date = LocalDate(2026, 1, 1),
                subject = "Hello",
                bodyParagraphs = listOf("Para one.", "Para two."),
                signatureName = "John Doe",
            )
        val html = LetterTemplate(PdfBranding.default(), data).generateHtml()
        assertTrue(html.contains("Hello"))
        assertTrue(html.contains("Para one"))
        assertTrue(html.contains("John Doe"))
        assertTrue(html.contains("Dear Sir/Madam"))
    }
}
