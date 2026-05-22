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
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tfoot
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

/** Generic party (bill-from / bill-to) — name, address lines, optional tax id. */
@ExperimentalPdfGeneratorApi
public data class PartyInfo(
    public val name: String,
    public val addressLines: List<String> = emptyList(),
    public val taxId: String? = null,
    public val email: String? = null,
    public val phone: String? = null,
)

/** One line on the invoice. */
@ExperimentalPdfGeneratorApi
public data class InvoiceLineItem(
    public val description: String,
    public val quantity: String,
    public val unitPrice: String,
    public val lineTotal: String,
)

/** All the data the invoice template needs. Currency formatting is consumer's responsibility. */
@ExperimentalPdfGeneratorApi
public data class InvoiceData(
    public val invoiceNumber: String,
    public val invoiceDate: LocalDate,
    public val dueDate: LocalDate? = null,
    public val billFrom: PartyInfo,
    public val billTo: PartyInfo,
    public val lineItems: List<InvoiceLineItem>,
    public val subtotal: String,
    public val tax: String? = null,
    public val total: String,
    public val notes: String? = null,
    public val terms: String? = null,
)

/**
 * Standard invoice HTML template — header with invoice #, bill-from / bill-to blocks,
 * line item table, totals, and optional notes / terms.
 */
@ExperimentalPdfGeneratorApi
public class InvoiceTemplate(
    branding: PdfBranding,
    public val invoice: InvoiceData,
) : HtmlTemplateGenerator(branding) {
    override fun getTitle(): String = "Invoice ${invoice.invoiceNumber}"

    override fun getAdditionalStyles(): String =
        """
        .invoice-meta { display: table; width: 100%; margin-bottom: 20px; }
        .party-block { display: inline-block; width: 48%; vertical-align: top; }
        .party-block.right { float: right; text-align: right; }
        .totals { width: 50%; margin-left: auto; margin-top: 20px; }
        .notes-section { margin-top: 30px; padding-top: 15px; border-top: 1px solid #e0e0e0; }
        """.trimIndent()

    override fun BODY.generateBody() {
        h1 { +"Invoice ${invoice.invoiceNumber}" }

        div {
            attributes["class"] = "invoice-meta"
            p {
                +"Issued: ${branding.dateFormatter(invoice.invoiceDate)}"
                invoice.dueDate?.let {
                    +" · Due: ${branding.dateFormatter(it)}"
                }
            }
        }

        div {
            attributes["class"] = "party-block"
            h2 { +"From" }
            renderParty(invoice.billFrom)
        }
        div {
            attributes["class"] = "party-block right"
            h2 { +"Bill to" }
            renderParty(invoice.billTo)
        }

        table {
            thead {
                tr {
                    th { +"Description" }
                    th {
                        attributes["class"] = "right"
                        +"Qty"
                    }
                    th {
                        attributes["class"] = "right"
                        +"Unit price"
                    }
                    th {
                        attributes["class"] = "right"
                        +"Total"
                    }
                }
            }
            tbody {
                invoice.lineItems.forEach { item ->
                    tr {
                        td { +item.description }
                        td {
                            attributes["class"] = "right"
                            +item.quantity
                        }
                        td {
                            attributes["class"] = "right"
                            +item.unitPrice
                        }
                        td {
                            attributes["class"] = "right"
                            +item.lineTotal
                        }
                    }
                }
            }
            tfoot {
                tr {
                    td {
                        attributes["colspan"] = "3"
                        attributes["class"] = "right"
                        +"Subtotal"
                    }
                    td {
                        attributes["class"] = "right"
                        +invoice.subtotal
                    }
                }
                invoice.tax?.let { taxAmt ->
                    tr {
                        td {
                            attributes["colspan"] = "3"
                            attributes["class"] = "right"
                            +"Tax"
                        }
                        td {
                            attributes["class"] = "right"
                            +taxAmt
                        }
                    }
                }
                tr {
                    td {
                        attributes["colspan"] = "3"
                        attributes["class"] = "right"
                        +"Total"
                    }
                    td {
                        attributes["class"] = "right"
                        +invoice.total
                    }
                }
            }
        }

        invoice.notes?.let { notes ->
            div {
                attributes["class"] = "notes-section"
                h2 { +"Notes" }
                p { +notes }
            }
        }
        invoice.terms?.let { terms ->
            div {
                attributes["class"] = "notes-section"
                h2 { +"Terms" }
                p { +terms }
            }
        }
    }
}

private fun kotlinx.html.FlowContent.renderParty(party: PartyInfo) {
    kotlinx.html.div {
        kotlinx.html.p { +party.name }
        party.addressLines.forEach { line -> kotlinx.html.p { +line } }
        party.email?.let { kotlinx.html.p { +it } }
        party.phone?.let { kotlinx.html.p { +it } }
        party.taxId?.let { kotlinx.html.p { +"Tax ID: $it" } }
    }
}
