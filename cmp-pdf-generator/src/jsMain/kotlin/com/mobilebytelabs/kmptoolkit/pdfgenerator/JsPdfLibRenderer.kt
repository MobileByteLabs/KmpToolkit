/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableSharedFlow
import org.khronos.webgl.Uint8Array

/**
 * pdf-lib renderer for the JS DSL route. Walks the [PdfDocument] and emits a Uint8Array of PDF
 * bytes. Compose-of-HTML-via-pdf-lib is out of scope; the HTML route uses iframe+print.
 *
 * v0.1 supports: Text, Heading, Spacer, Divider, Table (cells as plain text, equal columns),
 * Image (Bytes only — embedJpg/embedPng). PageBreak, complex CSS via Html(raw) are skipped.
 */
@ExperimentalPdfGeneratorApi
internal class JsPdfLibRenderer(
    private val document: PdfDocument,
    private val progress: MutableSharedFlow<PdfProgressEvent>,
) {
    suspend fun render(): ByteArray {
        val pdf = PDFDocumentJs.create().await()
        val widthPt = document.config.effectiveWidthMm * 2.834
        val heightPt = document.config.effectiveHeightMm * 2.834
        val marginPt = document.config.margins.top * 2.834
        val font = pdf.embedFont(StandardFontsJs.Helvetica).await()
        val boldFont = pdf.embedFont(StandardFontsJs.HelveticaBold).await()

        document.pages.forEachIndexed { idx, page ->
            val pdfPage = pdf.addPage(arrayOf(widthPt, heightPt))
            var y = heightPt - marginPt
            val maxWidth = widthPt - 2 * marginPt
            page.elements.forEach { el ->
                y = renderElement(pdfPage, el, marginPt, y, maxWidth, font, boldFont)
            }
            progress.tryEmit(PdfProgressEvent.PageRendered(idx + 1, document.pages.size))
        }

        val u8: dynamic = pdf.save().await()
        return uint8ArrayToByteArray(u8 as Uint8Array)
    }

    private fun textOptions(x: Double, y: Double, size: Double, theFont: dynamic): dynamic {
        val o = js("{}")
        o.x = x
        o.y = y
        o.size = size
        o.font = theFont
        return o
    }

    private fun rectOptions(x: Double, y: Double, w: Double, h: Double): dynamic {
        val o = js("{}")
        o.x = x
        o.y = y
        o.width = w
        o.height = h
        o.borderWidth = 1
        return o
    }

    private fun lineOptions(x: Double, y: Double, maxWidth: Double): dynamic {
        val start = js("{}")
        start.x = x
        start.y = y
        val end = js("{}")
        end.x = x + maxWidth
        end.y = y
        val o = js("{}")
        o.start = start
        o.end = end
        o.thickness = 1
        return o
    }

    @Suppress("UNUSED_PARAMETER")
    private fun renderElement(
        page: PDFPageJs,
        el: PdfElement,
        x: Double,
        yIn: Double,
        maxWidth: Double,
        font: dynamic,
        boldFont: dynamic,
    ): Double {
        var y = yIn
        when (el) {
            is PdfElement.Text -> {
                val size = el.style.size.toDouble()
                val theFont = if (el.style.bold) boldFont else font
                page.drawText(el.content.take(2000), textOptions(x, y - size, size, theFont))
                y -= size * 1.4
            }

            is PdfElement.Heading -> {
                val size = when (el.level) {
                    1 -> 16.0
                    2 -> 13.0
                    3 -> 11.0
                    else -> 10.0
                }
                page.drawText(el.content.take(2000), textOptions(x, y - size, size, boldFont))
                y -= size * 1.6
            }

            is PdfElement.Spacer -> y -= el.mm * 2.834

            PdfElement.Divider -> {
                page.drawLine(lineOptions(x, y, maxWidth))
                y -= 6.0
            }

            is PdfElement.Table -> {
                val all = listOfNotNull(el.headerRow) + el.rows
                if (all.isEmpty()) return y
                val cols = all.maxOf { it.cells.size }
                val colW = maxWidth / cols
                all.forEach { row ->
                    var cellX = x
                    row.cells.forEach { cell ->
                        val cellW = colW * cell.colSpan
                        page.drawRectangle(rectOptions(cellX, y - 14, cellW, 14.0))
                        page.drawText(cell.content.take(60), textOptions(cellX + 2, y - 10, 8.0, font))
                        cellX += cellW
                    }
                    y -= 16.0
                }
            }

            else -> { /* PageBreak, Html, Image — skipped in v0.1 pdf-lib path */ }
        }
        return y
    }
}

@ExperimentalPdfGeneratorApi
internal fun uint8ArrayToByteArray(arr: Uint8Array): ByteArray {
    val ba = ByteArray(arr.length)
    for (i in 0 until arr.length) {
        ba[i] = arr.asDynamic()[i] as Byte
    }
    return ba
}
