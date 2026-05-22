/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfDocumentDslTest {
    @Test
    fun emptyDocumentRejected() {
        assertFailsWith<PdfError.InvalidInput> {
            pdf { /* no pages */ }
        }
    }

    @Test
    fun singlePageDocBuilds() {
        val doc =
            pdf {
                page {
                    heading(1, "Hello")
                    text("World")
                }
            }
        assertEquals(1, doc.pages.size)
        assertEquals(2, doc.pages[0].elements.size)
        assertIs<PdfElement.Heading>(doc.pages[0].elements[0])
        assertIs<PdfElement.Text>(doc.pages[0].elements[1])
    }

    @Test
    fun pageConfigAndBrandingPropagate() {
        val config = PageConfig(size = PageSize.A3, orientation = Orientation.LANDSCAPE)
        val branding = PdfBranding.mifosDefault()
        val doc =
            pdf {
                pageConfig(config)
                branding(branding)
                page { text("hi") }
            }
        assertEquals(config, doc.config)
        assertEquals(branding, doc.branding)
    }

    @Test
    fun multiPageDocBuilds() {
        val doc =
            pdf {
                page { text("Page 1") }
                page { text("Page 2") }
                page { text("Page 3") }
            }
        assertEquals(3, doc.pages.size)
    }

    @Test
    fun tableBuilderProducesHeaderAndRows() {
        val doc =
            pdf {
                page {
                    table {
                        header {
                            cell("A")
                            cell("B")
                        }
                        row {
                            cell("1")
                            cell("2")
                        }
                        row {
                            cell("3")
                            cell("4")
                        }
                    }
                }
            }
        val table = doc.pages[0].elements[0] as PdfElement.Table
        assertNotNull(table.headerRow)
        assertEquals(2, table.headerRow!!.cells.size)
        assertEquals(2, table.rows.size)
        assertEquals("A", table.headerRow!!.cells[0].content)
    }

    @Test
    fun headingLevelOutOfRangeRejected() {
        assertFailsWith<IllegalArgumentException> {
            pdf { page { heading(7, "too deep") } }
        }
        assertFailsWith<IllegalArgumentException> {
            pdf { page { heading(0, "too shallow") } }
        }
    }

    @Test
    fun spacerRejectsNegative() {
        assertFailsWith<IllegalArgumentException> {
            pdf { page { spacer(-1) } }
        }
    }

    @Test
    fun htmlCompilerProducesXhtml() =
        runTest {
            val doc =
                pdf {
                    branding(PdfBranding.none())
                    page {
                        heading(1, "Title")
                        text("Body")
                    }
                }
            val html = doc.toHtml()
            assertTrue(html.startsWith("<!DOCTYPE html"))
            assertTrue(html.contains("<title>"))
            assertTrue(html.contains("Title"))
            assertTrue(html.contains("Body"))
        }

    @Test
    fun mixedElementsRoundTripHtml() =
        runTest {
            val doc =
                pdf {
                    branding(PdfBranding.none())
                    page {
                        heading(2, "Section")
                        text("Para 1")
                        divider()
                        table {
                            header { cell("Col A") }
                            row { cell("Cell A") }
                        }
                        spacer(10)
                        pageBreak()
                        text("After break")
                    }
                }
            val html = doc.toHtml()
            assertTrue(html.contains("<hr"))
            assertTrue(html.contains("page-break-before"))
            assertTrue(html.contains("<h2>"))
            assertTrue(html.contains("Cell A"))
        }
}
