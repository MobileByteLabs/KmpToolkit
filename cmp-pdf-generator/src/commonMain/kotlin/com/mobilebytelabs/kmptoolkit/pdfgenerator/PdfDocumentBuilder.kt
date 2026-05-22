/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator

/**
 * DSL scope marker — prevents accidental nesting of outer-scope calls inside inner builders.
 */
@DslMarker
public annotation class PdfDsl

/**
 * Document-level builder. See [pdf] for the entry point.
 */
@ExperimentalPdfGeneratorApi
@PdfDsl
public class PdfDocumentBuilder internal constructor() {
    private val pages = mutableListOf<PdfPage>()
    private var pageConfig: PageConfig = PageConfig()
    private var branding: PdfBranding = PdfBranding.none()

    public fun pageConfig(config: PageConfig) {
        this.pageConfig = config
    }

    public fun branding(b: PdfBranding) {
        this.branding = b
    }

    public fun page(block: PdfPageBuilder.() -> Unit) {
        val builder = PdfPageBuilder()
        builder.block()
        pages.add(builder.build())
    }

    internal fun build(): PdfDocument {
        if (pages.isEmpty()) {
            throw PdfError.InvalidInput("pdf { … } must declare at least one page { … }")
        }
        return PdfDocument(pages = pages.toList(), config = pageConfig, branding = branding)
    }
}

/**
 * Page-level builder.
 */
@ExperimentalPdfGeneratorApi
@PdfDsl
public class PdfPageBuilder internal constructor() {
    private val elements = mutableListOf<PdfElement>()

    public fun text(content: String, style: TextStyle = TextStyle()) {
        elements.add(PdfElement.Text(content, style))
    }

    public fun heading(level: Int, content: String) {
        elements.add(PdfElement.Heading(level, content))
    }

    public fun image(source: ImageSource, widthMm: Int? = null, heightMm: Int? = null) {
        elements.add(PdfElement.Image(source, widthMm, heightMm))
    }

    public fun table(block: TableBuilder.() -> Unit) {
        val builder = TableBuilder()
        builder.block()
        elements.add(builder.build())
    }

    public fun spacer(mm: Int) {
        elements.add(PdfElement.Spacer(mm))
    }

    public fun divider() {
        elements.add(PdfElement.Divider)
    }

    public fun pageBreak() {
        elements.add(PdfElement.PageBreak)
    }

    public fun html(raw: String) {
        elements.add(PdfElement.Html(raw))
    }

    internal fun build(): PdfPage = PdfPage(elements.toList())
}

/**
 * Table builder — header() once, row() many times.
 */
@ExperimentalPdfGeneratorApi
@PdfDsl
public class TableBuilder internal constructor() {
    private var headerRow: TableRow? = null
    private val rows = mutableListOf<TableRow>()

    public fun header(block: TableRowBuilder.() -> Unit) {
        val b = TableRowBuilder()
        b.block()
        headerRow = b.build()
    }

    public fun row(block: TableRowBuilder.() -> Unit) {
        val b = TableRowBuilder()
        b.block()
        rows.add(b.build())
    }

    internal fun build(): PdfElement.Table = PdfElement.Table(rows = rows.toList(), headerRow = headerRow)
}

@ExperimentalPdfGeneratorApi
@PdfDsl
public class TableRowBuilder internal constructor() {
    private val cells = mutableListOf<TableCell>()

    public fun cell(content: String, style: TextStyle = TextStyle(), colSpan: Int = 1) {
        cells.add(TableCell(content, style, colSpan))
    }

    internal fun build(): TableRow = TableRow(cells.toList())
}

/**
 * DSL entry point — builds a [PdfDocument].
 *
 * ```
 * val doc = pdf {
 *     pageConfig(PageConfig(size = PageSize.A4))
 *     branding(PdfBranding.none())
 *     page {
 *         heading(1, "Hello")
 *         text("World")
 *         table {
 *             header { cell("Item"); cell("Total") }
 *             row { cell("Widget"); cell("$30") }
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalPdfGeneratorApi
public fun pdf(block: PdfDocumentBuilder.() -> Unit): PdfDocument {
    val builder = PdfDocumentBuilder()
    builder.block()
    return builder.build()
}
