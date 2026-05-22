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
 * Source of an image. Different platforms accept different forms — bytes work everywhere.
 */
@ExperimentalPdfGeneratorApi
public sealed class ImageSource {
    public data class Bytes(public val bytes: ByteArray) : ImageSource() {
        override fun equals(other: Any?): Boolean = other is Bytes && bytes.contentEquals(other.bytes)
        override fun hashCode(): Int = bytes.contentHashCode()
    }

    public data class Url(public val url: String) : ImageSource()
    public data class DataUri(public val uri: String) : ImageSource()
    public data class Resource(public val path: String) : ImageSource()
}

/** Text alignment. */
@ExperimentalPdfGeneratorApi
public enum class Alignment { LEFT, CENTER, RIGHT, JUSTIFY }

/**
 * Text style.
 *
 * @param size Font size in points. Default 8pt to match mifos-x reference.
 * @param colorHex Optional override; null = theme default.
 */
@ExperimentalPdfGeneratorApi
public data class TextStyle(
    public val bold: Boolean = false,
    public val italic: Boolean = false,
    public val size: Int = 8,
    public val colorHex: String? = null,
    public val alignment: Alignment = Alignment.LEFT,
)

/** A cell in a [PdfElement.Table] row. */
@ExperimentalPdfGeneratorApi
public data class TableCell(
    public val content: String,
    public val style: TextStyle = TextStyle(),
    public val colSpan: Int = 1,
) {
    init {
        require(colSpan >= 1) { "colSpan must be >= 1" }
    }
}

/** A row of cells. */
@ExperimentalPdfGeneratorApi
public data class TableRow(public val cells: List<TableCell>)

/**
 * One element on a [PdfPage]. Composed via the [PdfDocumentBuilder] DSL or hand-built.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfElement {
    /** Plain text paragraph. */
    public data class Text(public val content: String, public val style: TextStyle = TextStyle()) : PdfElement()

    /** Heading. [level] 1-6, like HTML `<h1>`-`<h6>`. */
    public data class Heading(public val level: Int, public val content: String) : PdfElement() {
        init {
            require(level in 1..6) { "Heading level must be 1..6" }
        }
    }

    /** Image. If [widthMm] or [heightMm] is null, the engine chooses based on intrinsic size. */
    public data class Image(
        public val source: ImageSource,
        public val widthMm: Int? = null,
        public val heightMm: Int? = null,
    ) : PdfElement()

    /** Table. The optional [headerRow] is repeated on each new page when content overflows. */
    public data class Table(
        public val rows: List<TableRow>,
        public val headerRow: TableRow? = null,
    ) : PdfElement()

    /** Vertical whitespace (mm). */
    public data class Spacer(public val mm: Int) : PdfElement() {
        init {
            require(mm >= 0) { "Spacer mm must be >= 0" }
        }
    }

    /** Horizontal divider rule. */
    public object Divider : PdfElement()

    /** Force a new page from this point. */
    public object PageBreak : PdfElement()

    /** Raw HTML passthrough. Engine-dependent — only the HTML route renders this faithfully. */
    public data class Html(public val raw: String) : PdfElement()
}

/** One page worth of elements. */
@ExperimentalPdfGeneratorApi
public data class PdfPage(public val elements: List<PdfElement>)

/**
 * The DSL output — a fully-described PDF document. Pass to [PdfGenerator.generate] for rendering.
 */
@ExperimentalPdfGeneratorApi
public data class PdfDocument(
    public val pages: List<PdfPage>,
    public val config: PageConfig,
    public val branding: PdfBranding,
) {
    init {
        require(pages.isNotEmpty()) { "PdfDocument must have at least one page" }
    }
}
