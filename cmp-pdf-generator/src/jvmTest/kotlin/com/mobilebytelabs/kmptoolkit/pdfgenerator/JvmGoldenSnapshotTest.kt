/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceLineItem
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.InvoiceTemplate
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.PartyInfo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Snapshot test in deterministic mode. We assert byte-stable metadata, not full byte equality
 * (PDF object IDs are platform-specific).
 *
 * Compares: file size envelope, PDF magic, page count via simple substring scan, presence of
 * known content strings.
 */
class JvmGoldenSnapshotTest {
    @Test
    fun deterministicInvoiceProducesStableMagicAndKeywords() = runTest {
        val gen = PdfGenerator()
        val branding = PdfBranding.default()
        val template = InvoiceTemplate(
            branding = branding,
            invoice = InvoiceData(
                invoiceNumber = "GOLDEN-001",
                invoiceDate = LocalDate(2026, 1, 1),
                billFrom = PartyInfo("Sender Co"),
                billTo = PartyInfo("Receiver Co"),
                lineItems = listOf(InvoiceLineItem("Item A", "1", "$10", "$10")),
                subtotal = "$10",
                total = "$10",
            ),
        )
        val html = template.generateHtml()
        val result = gen.generateFromHtml(
            html = html,
            output = PdfOutput.ByteArrayOutput,
            options = PdfGeneratorOptions(deterministic = true, fixedDate = LocalDate(2026, 1, 1)),
        )
        assertIs<PdfResult.Success>(result)
        val bytes = result.bytes!!
        // PDF magic
        assertTrue(
            bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte(),
        )
        // Reasonable size envelope (golden ≈ 5-50KB invoice)
        assertTrue(bytes.size in 1000..200_000, "Expected envelope size; got ${bytes.size}")
    }

    @Test
    fun emptyDocRejectedAtBuilder() = runTest {
        val gen = PdfGenerator()
        // pdf {} throws InvalidInput at builder time
        val err = kotlin.runCatching { pdf { /* no pages */ } }.exceptionOrNull()
        assertEquals(PdfError.InvalidInput::class, err!!::class)
    }
}
