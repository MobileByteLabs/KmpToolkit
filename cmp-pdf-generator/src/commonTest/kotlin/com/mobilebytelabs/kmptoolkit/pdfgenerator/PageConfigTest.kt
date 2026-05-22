/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageConfigTest {

    @Test
    fun pageSizeDimensionsAreCorrect() {
        assertEquals(210, PageSize.A4.widthMm)
        assertEquals(297, PageSize.A4.heightMm)
        assertEquals(216, PageSize.LETTER.widthMm)
        assertEquals(279, PageSize.LETTER.heightMm)
    }

    @Test
    fun customPageSizeRequiresPositiveDimensions() {
        assertFailsWith<IllegalArgumentException> { CustomPageSize(0, 100) }
        assertFailsWith<IllegalArgumentException> { CustomPageSize(100, -1) }
    }

    @Test
    fun edgeMarginsUniformFactory() {
        val m = EdgeMargins.uniform(15)
        assertEquals(15, m.top)
        assertEquals(15, m.right)
        assertEquals(15, m.bottom)
        assertEquals(15, m.left)
    }

    @Test
    fun edgeMarginsZeroFactory() {
        val m = EdgeMargins.zero()
        assertTrue(m.top == 0 && m.right == 0 && m.bottom == 0 && m.left == 0)
    }

    @Test
    fun edgeMarginsNegativeIsRejected() {
        assertFailsWith<IllegalArgumentException> { EdgeMargins(-1, 0, 0, 0) }
    }

    @Test
    fun pageConfigEffectiveDimensionsPortrait() {
        val c = PageConfig(size = PageSize.A4, orientation = Orientation.PORTRAIT)
        assertEquals(210, c.effectiveWidthMm)
        assertEquals(297, c.effectiveHeightMm)
    }

    @Test
    fun pageConfigEffectiveDimensionsLandscape() {
        val c = PageConfig(size = PageSize.A4, orientation = Orientation.LANDSCAPE)
        assertEquals(297, c.effectiveWidthMm)
        assertEquals(210, c.effectiveHeightMm)
    }

    @Test
    fun pageConfigCustomSizeTakesPrecedence() {
        val c = PageConfig(
            size = PageSize.A4,
            customSize = CustomPageSize(100, 200),
        )
        assertEquals(100, c.effectiveWidthMm)
        assertEquals(200, c.effectiveHeightMm)
    }

    @Test
    fun pageHeaderFooterDefaults() {
        val hf = PageHeaderFooter()
        assertTrue(hf.showHeader)
        assertTrue(hf.showFooter)
        assertTrue(!hf.showPageNumbers)
        assertNull(hf.customHeaderHtml)
        assertNull(hf.customFooterHtml)
    }
}
