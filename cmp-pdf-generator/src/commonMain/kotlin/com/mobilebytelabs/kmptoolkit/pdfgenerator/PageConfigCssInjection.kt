/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

/**
 * Substitute the `/* PAGE_CONFIG_PLACEHOLDER */` token in an HTML template's CSS with a real
 * `@page` rule derived from [pageConfig]. Used by every Tier-1 platform implementation.
 */
@ExperimentalPdfGeneratorApi
internal fun String.injectPageConfigCss(pageConfig: PageConfig): String {
    val orientation = if (pageConfig.orientation == Orientation.LANDSCAPE) "landscape" else "portrait"
    val sizeKw = when (pageConfig.size) {
        PageSize.A4 -> "A4"
        PageSize.A3 -> "A3"
        PageSize.A5 -> "A5"
        PageSize.B5 -> "B5"
        PageSize.LETTER -> "letter"
        PageSize.LEGAL -> "legal"
        PageSize.TABLOID -> "tabloid"
        PageSize.STATEMENT -> "statement"
    }
    val m = pageConfig.margins
    val pageCss = buildString {
        append("@page { size: ")
        append(sizeKw)
        append(' ')
        append(orientation)
        append("; margin: ")
        append(m.top).append("mm ")
        append(m.right).append("mm ")
        append(m.bottom).append("mm ")
        append(m.left).append("mm; }")
    }
    return replace("/* PAGE_CONFIG_PLACEHOLDER */", pageCss)
}
