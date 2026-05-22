/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.flow.MutableSharedFlow
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream

@ExperimentalPdfGeneratorApi
public fun createPdfGenerator(): PdfGenerator = PdfGenerator()

/**
 * Apache PDFBox direct renderer for the DSL route. v0.1 produces a structured PDF with
 * simple text + tables + spacers + page breaks. Images and complex CSS belong on the HTML route.
 */
@ExperimentalPdfGeneratorApi
internal class JvmNativePdfRenderer(
    private val document: PdfDocument,
    private val options: PdfGeneratorOptions,
    private val progress: MutableSharedFlow<PdfProgressEvent>,
) {
    private val pdfDoc = PDDocument()
    private val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    fun render(): ByteArray {
        val widthPt = document.config.effectiveWidthMm * 2.834f
        val heightPt = document.config.effectiveHeightMm * 2.834f
        val mediaBox = PDRectangle(widthPt, heightPt)
        val marginPt = document.config.margins.top * 2.834f

        document.pages.forEachIndexed { pageIdx, page ->
            val pdPage = PDPage(mediaBox)
            pdfDoc.addPage(pdPage)
            var y = heightPt - marginPt
            val maxWidth = widthPt - 2 * marginPt
            PDPageContentStream(pdfDoc, pdPage).use { cs ->
                page.elements.forEach { el ->
                    y = renderElement(cs, el, marginPt, y, maxWidth)
                }
            }
            progress.tryEmit(PdfProgressEvent.PageRendered(pageIdx + 1, document.pages.size))
        }

        val out = ByteArrayOutputStream()
        pdfDoc.save(out)
        pdfDoc.close()
        return out.toByteArray()
    }

    private fun renderElement(cs: PDPageContentStream, el: PdfElement, x: Float, yIn: Float, maxWidth: Float): Float {
        var y = yIn
        when (el) {
            is PdfElement.Text -> {
                val size = el.style.size.toFloat()
                cs.beginText()
                cs.setFont(if (el.style.bold) fontBold else font, size)
                cs.newLineAtOffset(x, y - size)
                cs.showText(el.content.take(2000))
                cs.endText()
                y -= size * 1.4f
            }
            is PdfElement.Heading -> {
                val size = when (el.level) { 1 -> 16f; 2 -> 13f; 3 -> 11f; else -> 10f }
                cs.beginText()
                cs.setFont(fontBold, size)
                cs.newLineAtOffset(x, y - size)
                cs.showText(el.content.take(2000))
                cs.endText()
                y -= size * 1.6f
            }
            is PdfElement.Spacer -> y -= el.mm * 2.834f
            PdfElement.Divider -> {
                cs.moveTo(x, y); cs.lineTo(x + maxWidth, y); cs.stroke()
                y -= 6f
            }
            is PdfElement.Table -> {
                val allRows = listOfNotNull(el.headerRow) + el.rows
                if (allRows.isEmpty()) return y
                val cols = allRows.maxOf { it.cells.size }
                val colWidth = maxWidth / cols
                allRows.forEach { row ->
                    var cellX = x
                    row.cells.forEach { cell ->
                        val cellW = colWidth * cell.colSpan
                        cs.addRect(cellX, y - 14, cellW, 14)
                        cs.stroke()
                        cs.beginText()
                        cs.setFont(font, 8f)
                        cs.newLineAtOffset(cellX + 2, y - 10)
                        cs.showText(cell.content.take(60))
                        cs.endText()
                        cellX += cellW
                    }
                    y -= 16f
                }
            }
            is PdfElement.Image -> {
                // v0.1: image embedding via PDFBox is non-trivial — emit placeholder
                cs.beginText()
                cs.setFont(font, 6f)
                cs.newLineAtOffset(x, y - 6)
                cs.showText("[image]")
                cs.endText()
                y -= 14f
            }
            PdfElement.PageBreak -> y = 0f
            is PdfElement.Html -> {
                cs.beginText()
                cs.setFont(font, 6f)
                cs.newLineAtOffset(x, y - 6)
                cs.showText("[HTML content elided — use HTML route]")
                cs.endText()
                y -= 12f
            }
        }
        return y
    }
}
