/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PdfBrandingTest {
    @Test
    fun noneFactoryHasNoLogoNoFooter() {
        val b = PdfBranding.none()
        assertEquals(PdfLogo.None, b.logo)
        assertNull(b.poweredByText)
    }

    @Test
    fun defaultFactoryHasPoweredBy() {
        val b = PdfBranding.default()
        assertNotNull(b.poweredByText)
        assertTrue(b.poweredByText!!.contains("KmpToolkit"))
    }

    @Test
    fun mifosDefaultFactoryUsesMifosColors() {
        val b = PdfBranding.mifosDefault()
        assertEquals("#33618D", b.theme.accentColorHex)
        assertEquals("#1976d2", b.theme.headerColorHex)
        assertEquals("Powered by Mifos", b.poweredByText)
    }

    @Test
    fun defaultDateFormatProducesDdMmYyyy() {
        val date = LocalDate(2026, 5, 22)
        assertEquals("22/05/2026", defaultDateFormat(date))
    }

    @Test
    fun pdfThemeRequiresPositiveFontScale() {
        assertFailsWith<IllegalArgumentException> { PdfTheme(fontScale = 0f) }
        assertFailsWith<IllegalArgumentException> { PdfTheme(fontScale = -1f) }
    }

    @Test
    fun watermarkRequiresOpacityInRange() {
        assertFailsWith<IllegalArgumentException> { Watermark(text = "X", opacity = 1.5f) }
        assertFailsWith<IllegalArgumentException> { Watermark(text = "X", opacity = -0.1f) }
    }

    @Test
    fun watermarkRequiresTextOrImage() {
        assertFailsWith<IllegalArgumentException> { Watermark() }
    }

    @Test
    fun svgLogoEqualsByContent() {
        val a = PdfLogo.Svg(byteArrayOf(1, 2, 3))
        val b = PdfLogo.Svg(byteArrayOf(1, 2, 3))
        assertEquals(a, b)
    }
}
