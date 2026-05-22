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
 * Standard page sizes in millimeters.
 */
@ExperimentalPdfGeneratorApi
public enum class PageSize(
    public val widthMm: Int,
    public val heightMm: Int,
) {
    A3(297, 420),
    A4(210, 297),
    A5(148, 210),
    B5(176, 250),
    LETTER(216, 279),
    LEGAL(216, 356),
    TABLOID(279, 432),
    STATEMENT(140, 216),
}

/**
 * Custom page size (mm). Use when none of the standard [PageSize] values fit.
 */
@ExperimentalPdfGeneratorApi
public data class CustomPageSize(
    public val widthMm: Int,
    public val heightMm: Int,
) {
    init {
        require(widthMm > 0) { "widthMm must be > 0" }
        require(heightMm > 0) { "heightMm must be > 0" }
    }
}

/**
 * Page orientation.
 */
@ExperimentalPdfGeneratorApi
public enum class Orientation { PORTRAIT, LANDSCAPE }

/**
 * Per-edge margins in millimeters.
 */
@ExperimentalPdfGeneratorApi
public data class EdgeMargins(
    public val top: Int,
    public val right: Int,
    public val bottom: Int,
    public val left: Int,
) {
    init {
        require(top >= 0) { "top must be >= 0" }
        require(right >= 0) { "right must be >= 0" }
        require(bottom >= 0) { "bottom must be >= 0" }
        require(left >= 0) { "left must be >= 0" }
    }

    public companion object {
        /** Same margin on all four edges. */
        public fun uniform(mm: Int): EdgeMargins = EdgeMargins(mm, mm, mm, mm)

        /** Zero margins on all edges (edge-to-edge content). */
        public fun zero(): EdgeMargins = EdgeMargins(0, 0, 0, 0)
    }
}

/**
 * Per-page header / footer configuration.
 *
 * @param customHeaderHtml If non-null, replaces the default branding header.
 * @param customFooterHtml If non-null, replaces the default "powered by" footer.
 */
@ExperimentalPdfGeneratorApi
public data class PageHeaderFooter(
    public val showHeader: Boolean = true,
    public val showFooter: Boolean = true,
    public val showPageNumbers: Boolean = false,
    public val customHeaderHtml: String? = null,
    public val customFooterHtml: String? = null,
)

/**
 * Page configuration — size, orientation, margins, optional per-page header/footer.
 *
 * @param size Standard size. Ignored when [customSize] is non-null.
 * @param customSize Custom dimensions. Takes precedence over [size].
 * @param orientation Portrait or landscape.
 * @param margins Per-edge margins.
 * @param headerFooter Optional per-page header/footer behavior.
 */
@ExperimentalPdfGeneratorApi
public data class PageConfig(
    public val size: PageSize = PageSize.A4,
    public val customSize: CustomPageSize? = null,
    public val orientation: Orientation = Orientation.PORTRAIT,
    public val margins: EdgeMargins = EdgeMargins.uniform(8),
    public val headerFooter: PageHeaderFooter? = null,
) {
    /** Effective width in mm — honors customSize first, else size. */
    public val effectiveWidthMm: Int
        get() =
            (customSize?.widthMm ?: size.widthMm).let {
                if (orientation == Orientation.LANDSCAPE) (customSize?.heightMm ?: size.heightMm) else it
            }

    /** Effective height in mm. */
    public val effectiveHeightMm: Int
        get() =
            (customSize?.heightMm ?: size.heightMm).let {
                if (orientation == Orientation.LANDSCAPE) (customSize?.widthMm ?: size.widthMm) else it
            }
}
