/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator.templates

import com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi
import com.mobilebytelabs.kmptoolkit.pdfgenerator.HtmlTemplateGenerator
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfBranding
import kotlinx.datetime.LocalDate
import kotlinx.html.BODY
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

// =========================================================================================
// ReportTemplate
// =========================================================================================

/** A section in a [ReportData]. Supports nested subsections via `subsections`. */
@ExperimentalPdfGeneratorApi
public data class ReportSection(
    public val heading: String,
    public val body: String,
    public val subsections: List<ReportSection> = emptyList(),
)

@ExperimentalPdfGeneratorApi
public data class ReportData(
    public val title: String,
    public val subtitle: String? = null,
    public val author: String? = null,
    public val date: LocalDate,
    public val sections: List<ReportSection>,
    public val appendix: String? = null,
)

@ExperimentalPdfGeneratorApi
public class ReportTemplate(
    branding: PdfBranding,
    public val report: ReportData,
) : HtmlTemplateGenerator(branding) {

    override fun getTitle(): String = report.title

    override fun BODY.generateBody() {
        h1 { +report.title }
        report.subtitle?.let { p { +it } }
        p {
            report.author?.let { +"By $it · " }
            +branding.dateFormatter(report.date)
        }
        report.sections.forEachIndexed { idx, section ->
            renderSection(section, level = 2, index = "${idx + 1}.")
        }
        report.appendix?.let {
            h2 { +"Appendix" }
            p { +it }
        }
    }

    private fun BODY.renderSection(section: ReportSection, level: Int, index: String) {
        when (level) {
            2 -> h2 { +"$index ${section.heading}" }
            else -> h3 { +"$index ${section.heading}" }
        }
        p { +section.body }
        section.subsections.forEachIndexed { i, sub ->
            renderSection(sub, level + 1, "$index${i + 1}.")
        }
    }
}

// =========================================================================================
// ReceiptTemplate (thermal-printer-style narrow layout)
// =========================================================================================

@ExperimentalPdfGeneratorApi
public data class ReceiptLineItem(
    public val description: String,
    public val amount: String,
)

@ExperimentalPdfGeneratorApi
public data class ReceiptData(
    public val merchantName: String,
    public val merchantAddress: String? = null,
    public val receiptNumber: String,
    public val date: LocalDate,
    public val items: List<ReceiptLineItem>,
    public val subtotal: String,
    public val tax: String? = null,
    public val total: String,
    public val paymentMethod: String? = null,
    public val footer: String? = null,
)

@ExperimentalPdfGeneratorApi
public class ReceiptTemplate(
    branding: PdfBranding,
    public val receipt: ReceiptData,
) : HtmlTemplateGenerator(branding) {

    override fun getTitle(): String = "Receipt ${receipt.receiptNumber}"

    override fun getAdditionalStyles(): String = """
        body { max-width: 80mm; }
        .receipt-line { display: flex; justify-content: space-between; }
    """.trimIndent()

    override fun BODY.generateBody() {
        div {
            attributes["class"] = "center"
            h2 { +receipt.merchantName }
            receipt.merchantAddress?.let { p { +it } }
        }
        p { +"Receipt #${receipt.receiptNumber}" }
        p { +branding.dateFormatter(receipt.date) }
        table {
            tbody {
                receipt.items.forEach { item ->
                    tr {
                        td { +item.description }
                        td { attributes["class"] = "right"; +item.amount }
                    }
                }
                tr {
                    td { +"Subtotal" }
                    td { attributes["class"] = "right"; +receipt.subtotal }
                }
                receipt.tax?.let { taxAmt ->
                    tr {
                        td { +"Tax" }
                        td { attributes["class"] = "right"; +taxAmt }
                    }
                }
                tr {
                    td { attributes["style"] = "font-weight:bold"; +"TOTAL" }
                    td {
                        attributes["class"] = "right"
                        attributes["style"] = "font-weight:bold"
                        +receipt.total
                    }
                }
            }
        }
        receipt.paymentMethod?.let { p { +"Paid: $it" } }
        receipt.footer?.let { div { attributes["class"] = "center"; p { +it } } }
    }
}

// =========================================================================================
// StatementTemplate (periodic financial statement)
// =========================================================================================

@ExperimentalPdfGeneratorApi
public data class StatementTransaction(
    public val date: LocalDate,
    public val description: String,
    public val debit: String? = null,
    public val credit: String? = null,
    public val balance: String,
)

@ExperimentalPdfGeneratorApi
public data class StatementData(
    public val accountHolder: PartyInfo,
    public val accountNumber: String,
    public val periodStart: LocalDate,
    public val periodEnd: LocalDate,
    public val openingBalance: String,
    public val closingBalance: String,
    public val transactions: List<StatementTransaction>,
)

@ExperimentalPdfGeneratorApi
public class StatementTemplate(
    branding: PdfBranding,
    public val statement: StatementData,
) : HtmlTemplateGenerator(branding) {

    override fun getTitle(): String = "Account Statement"

    override fun BODY.generateBody() {
        h1 { +"Account Statement" }
        p { +"Account: ${statement.accountNumber}" }
        p {
            val start = branding.dateFormatter(statement.periodStart)
            val end = branding.dateFormatter(statement.periodEnd)
            +"Period: $start – $end"
        }
        p { +"Account holder: ${statement.accountHolder.name}" }
        p { +"Opening balance: ${statement.openingBalance}" }

        table {
            thead {
                tr {
                    th { +"Date" }
                    th { +"Description" }
                    th { attributes["class"] = "right"; +"Debit" }
                    th { attributes["class"] = "right"; +"Credit" }
                    th { attributes["class"] = "right"; +"Balance" }
                }
            }
            tbody {
                statement.transactions.forEach { tx ->
                    tr {
                        td { +branding.dateFormatter(tx.date) }
                        td { +tx.description }
                        td { attributes["class"] = "right"; +(tx.debit ?: "") }
                        td { attributes["class"] = "right"; +(tx.credit ?: "") }
                        td { attributes["class"] = "right"; +tx.balance }
                    }
                }
            }
        }
        p { +"Closing balance: ${statement.closingBalance}" }
    }
}

// =========================================================================================
// LetterTemplate (formal letter layout)
// =========================================================================================

@ExperimentalPdfGeneratorApi
public data class LetterData(
    public val sender: PartyInfo,
    public val recipient: PartyInfo,
    public val date: LocalDate,
    public val subject: String? = null,
    public val salutation: String = "Dear Sir/Madam,",
    public val bodyParagraphs: List<String>,
    public val closing: String = "Sincerely,",
    public val signatureName: String,
    public val signatureTitle: String? = null,
)

@ExperimentalPdfGeneratorApi
public class LetterTemplate(
    branding: PdfBranding,
    public val letter: LetterData,
) : HtmlTemplateGenerator(branding) {

    override fun getTitle(): String = letter.subject ?: "Letter"

    override fun BODY.generateBody() {
        // Sender block (right-aligned)
        div {
            attributes["class"] = "right"
            p { +letter.sender.name }
            letter.sender.addressLines.forEach { p { +it } }
        }
        p { attributes["class"] = "right"; +branding.dateFormatter(letter.date) }

        // Recipient block
        p { +letter.recipient.name }
        letter.recipient.addressLines.forEach { p { +it } }

        letter.subject?.let { p { attributes["style"] = "font-weight:bold"; +"Subject: $it" } }
        p { +letter.salutation }

        letter.bodyParagraphs.forEach { para ->
            p { attributes["style"] = "text-align: justify; margin-bottom: 10pt;"; +para }
        }

        p { +letter.closing }
        p { +letter.signatureName }
        letter.signatureTitle?.let { p { +it } }
    }
}
