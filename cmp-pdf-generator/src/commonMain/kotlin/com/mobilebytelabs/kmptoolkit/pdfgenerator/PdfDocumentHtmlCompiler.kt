/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.h5
import kotlinx.html.h6
import kotlinx.html.hr
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe
import kotlin.io.encoding.Base64

/**
 * Compile a [PdfDocument] DSL tree to an XHTML string ready to feed an HTML-to-PDF engine.
 *
 * Output emits each page as `<div class="page">` separated by `page-break-before: always`.
 * Branding (logo, theme, footer) is handled by [PdfDocumentHtmlTemplate] which delegates to
 * [HtmlTemplateGenerator].
 */
@ExperimentalPdfGeneratorApi
public suspend fun PdfDocument.toHtml(): String = PdfDocumentHtmlTemplate(this).generateHtml()

/**
 * `HtmlTemplateGenerator` that renders a [PdfDocument]'s DSL elements as HTML body content.
 * Public so consumers can subclass / inspect the output.
 */
@ExperimentalPdfGeneratorApi
public open class PdfDocumentHtmlTemplate(
    protected val document: PdfDocument,
) : HtmlTemplateGenerator(document.branding) {
    override fun getTitle(): String = "PDF Document"

    override fun BODY.generateBody() {
        document.pages.forEachIndexed { pageIdx, page ->
            div {
                attributes["class"] = "page"
                if (pageIdx > 0) {
                    attributes["style"] = "page-break-before: always;"
                }
                page.elements.forEach { renderElement(it) }
            }
        }
    }
}

private fun FlowContent.renderElement(el: PdfElement) {
    when (el) {
        is PdfElement.Text -> {
            p {
                attributes["style"] = el.style.toCssStyle()
                +el.content
            }
        }

        is PdfElement.Heading -> {
            when (el.level) {
                1 -> h1 { +el.content }
                2 -> h2 { +el.content }
                3 -> h3 { +el.content }
                4 -> h4 { +el.content }
                5 -> h5 { +el.content }
                else -> h6 { +el.content }
            }
        }

        is PdfElement.Image -> {
            img(src = el.source.toSrc(), alt = "image") {
                val styleParts = mutableListOf<String>()
                el.widthMm?.let { styleParts.add("width:${it}mm") }
                el.heightMm?.let { styleParts.add("height:${it}mm") }
                if (styleParts.isNotEmpty()) attributes["style"] = styleParts.joinToString("; ")
            }
        }

        is PdfElement.Table -> {
            table {
                el.headerRow?.let { hr0 ->
                    thead {
                        tr {
                            hr0.cells.forEach { cell ->
                                th {
                                    attributes["style"] = cell.style.toCssStyle()
                                    if (cell.colSpan > 1) attributes["colspan"] = cell.colSpan.toString()
                                    +cell.content
                                }
                            }
                        }
                    }
                }
                tbody {
                    el.rows.forEach { row ->
                        tr {
                            row.cells.forEach { cell ->
                                td {
                                    attributes["style"] = cell.style.toCssStyle()
                                    if (cell.colSpan > 1) attributes["colspan"] = cell.colSpan.toString()
                                    +cell.content
                                }
                            }
                        }
                    }
                }
            }
        }

        is PdfElement.Spacer -> {
            div {
                attributes["style"] = "height: ${el.mm}mm;"
            }
        }

        is PdfElement.Divider -> {
            hr {}
        }

        is PdfElement.PageBreak -> {
            div {
                attributes["style"] = "page-break-before: always;"
            }
        }

        is PdfElement.Html -> {
            div {
                unsafe { +sanitize(el.raw) }
            }
        }
    }
}

internal fun TextStyle.toCssStyle(): String {
    val parts = mutableListOf<String>()
    if (bold) parts.add("font-weight: bold")
    if (italic) parts.add("font-style: italic")
    parts.add("font-size: ${size}pt")
    colorHex?.let { parts.add("color: $it") }
    parts.add("text-align: ${alignment.name.lowercase()}")
    return parts.joinToString("; ")
}

internal fun ImageSource.toSrc(): String =
    when (this) {
        is ImageSource.Bytes -> "data:image/png;base64," + Base64.encode(bytes)
        is ImageSource.DataUri -> uri
        is ImageSource.Url -> url
        is ImageSource.Resource -> "/$path"
    }

/**
 * Minimal sanitizer for `PdfElement.Html(raw)` — strips `<script>`, `<iframe>`, and inline `on*`
 * handlers. NOT a full HTML sanitizer; `PdfElement.Html` is marked for "trusted input" usage.
 */
private fun sanitize(raw: String): String =
    raw
        .replace(Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        .replace(Regex("<iframe\\b[^>]*>.*?</iframe>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        .replace(Regex("\\son\\w+\\s*=\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\son\\w+\\s*=\\s*'[^']*'", RegexOption.IGNORE_CASE), "")
