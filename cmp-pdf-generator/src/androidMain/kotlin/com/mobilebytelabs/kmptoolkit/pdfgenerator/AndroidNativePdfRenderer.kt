/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.ByteArrayOutputStream
import android.graphics.pdf.PdfDocument as NativePdfDocument

/**
 * DSL → Android-native `PdfDocument`. Used when the document has no `Html` elements and is
 * simple enough to render via direct Canvas calls.
 *
 * Limitations (v0.1):
 *  - Text wrapping is naive (clip at width; no word-boundary)
 *  - Tables are rendered with fixed equal column widths
 *  - Images: only `ImageSource.Bytes` and `ImageSource.DataUri` supported synchronously
 *  - Headings use larger fixed sizes (1 → 14pt, 2 → 11pt, 3+ → 10pt)
 *
 * For pixel-perfect output use the HTML route (declare any `Html(raw)` element).
 */
@ExperimentalPdfGeneratorApi
internal class AndroidNativePdfRenderer(
    private val document: PdfDocument,
    private val options: PdfGeneratorOptions,
    private val progress: MutableSharedFlow<PdfProgressEvent>,
) {
    private val pdf = NativePdfDocument()
    private val paint =
        Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#333333")
        }

    fun render(): ByteArray {
        val widthMm = document.config.effectiveWidthMm
        val heightMm = document.config.effectiveHeightMm
        val widthPt = (widthMm * 2.834).toInt() // mm → points
        val heightPt = (heightMm * 2.834).toInt()
        val marginPt = (document.config.margins.top * 2.834).toInt()

        document.pages.forEachIndexed { pageIdx, page ->
            val pageInfo = NativePdfDocument.PageInfo.Builder(widthPt, heightPt, pageIdx + 1).create()
            val pdfPage = pdf.startPage(pageInfo)
            val canvas = pdfPage.canvas
            var y = marginPt.toFloat()
            val x = marginPt.toFloat()
            val maxWidth = widthPt - 2 * marginPt
            page.elements.forEach { el ->
                y = renderElement(canvas, el, x, y, maxWidth.toFloat())
            }
            pdf.finishPage(pdfPage)
            progress.tryEmit(PdfProgressEvent.PageRendered(pageIdx + 1, document.pages.size))
        }

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    private fun renderElement(canvas: Canvas, el: PdfElement, x: Float, yIn: Float, maxWidth: Float): Float {
        var y = yIn
        when (el) {
            is PdfElement.Text -> {
                paint.textSize = el.style.size.toFloat()
                paint.isFakeBoldText = el.style.bold
                el.style.colorHex?.let { paint.color = Color.parseColor(it) }
                canvas.drawText(el.content, x, y + el.style.size, paint)
                y += el.style.size * 1.4f
            }

            is PdfElement.Heading -> {
                val size =
                    when (el.level) {
                        1 -> 16
                        2 -> 13
                        3 -> 11
                        else -> 10
                    }
                paint.textSize = size.toFloat()
                paint.isFakeBoldText = true
                canvas.drawText(el.content, x, y + size, paint)
                paint.isFakeBoldText = false
                y += size * 1.6f
            }

            is PdfElement.Image -> {
                val bytes =
                    when (val src = el.source) {
                        is ImageSource.Bytes -> src.bytes
                        is ImageSource.DataUri -> Base64.decode(src.uri.substringAfter(","), Base64.DEFAULT)
                        else -> null
                    }
                if (bytes != null) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val w = el.widthMm?.let { (it * 2.834).toFloat() } ?: bmp.width.toFloat()
                        val h = el.heightMm?.let { (it * 2.834).toFloat() } ?: bmp.height.toFloat()
                        canvas.drawBitmap(bmp, null, android.graphics.RectF(x, y, x + w, y + h), null)
                        y += h + 6f
                    }
                }
            }

            is PdfElement.Table -> {
                val allRows = listOfNotNull(el.headerRow) + el.rows
                if (allRows.isEmpty()) return y
                val cols = allRows.maxOf { it.cells.size }
                val colWidth = maxWidth / cols
                paint.textSize = 8f
                allRows.forEach { row ->
                    var cellX = x
                    row.cells.forEach { cell ->
                        val cellW = colWidth * cell.colSpan
                        canvas.drawRect(cellX, y, cellX + cellW, y + 14, paint.apply { style = Paint.Style.STROKE })
                        paint.style = Paint.Style.FILL
                        canvas.drawText(cell.content.take(40), cellX + 2, y + 10, paint)
                        cellX += cellW
                    }
                    y += 16
                }
                paint.style = Paint.Style.FILL
            }

            is PdfElement.Spacer -> {
                y += el.mm * 2.834f
            }

            PdfElement.Divider -> {
                canvas.drawLine(x, y + 2, x + maxWidth, y + 2, paint)
                y += 8f
            }

            // signals new page; v0.1 caller honors page breaks only at element boundaries
            PdfElement.PageBreak -> {
                y = Float.MAX_VALUE
            }

            is PdfElement.Html -> {
                // Native renderer cannot render HTML — emit a marker only.
                paint.textSize = 6f
                canvas.drawText("[HTML content elided — use HTML route]", x, y + 6, paint)
                y += 12f
            }
        }
        return y
    }
}
