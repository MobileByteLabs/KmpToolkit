/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertEquals

class PdfFileNameTest {
    @Test
    fun appendsPdfExtensionWhenMissing() {
        assertEquals("invoice.pdf", ensurePdfFileName("invoice"))
    }

    @Test
    fun preservesExistingPdfExtension() {
        assertEquals("invoice.pdf", ensurePdfFileName("invoice.pdf"))
    }

    @Test
    fun doesNotDoubleExtensionForUppercase() {
        assertEquals("INVOICE.PDF", ensurePdfFileName("INVOICE.PDF"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("invoice.pdf", ensurePdfFileName("  invoice  "))
    }

    @Test
    fun blankFallsBackToDocument() {
        assertEquals("document.pdf", ensurePdfFileName("   "))
    }

    @Test
    fun emptyFallsBackToDocument() {
        assertEquals("document.pdf", ensurePdfFileName(""))
    }

    @Test
    fun defaultConstantIsNormalized() {
        assertEquals(DEFAULT_PDF_FILE_NAME, ensurePdfFileName(DEFAULT_PDF_FILE_NAME))
    }

    @Test
    fun keepsNestedDotsBeforeExtension() {
        assertEquals("report.2026.q1.pdf", ensurePdfFileName("report.2026.q1"))
    }
}
