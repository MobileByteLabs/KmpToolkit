/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class, kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

/** Convert NSData → ByteArray. */
@ExperimentalPdfGeneratorApi
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val ba = ByteArray(size)
    if (size > 0) {
        ba.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, size.toULong())
        }
    }
    return ba
}

/** Convert ByteArray → NSData. */
@ExperimentalPdfGeneratorApi
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

/** Inject `@page` CSS placeholder for Apple HTML renderers. */
@ExperimentalPdfGeneratorApi
internal fun String.injectPageConfigCss(pageConfig: PageConfig): String {
    val orientation = if (pageConfig.orientation == Orientation.LANDSCAPE) "landscape" else "portrait"
    val sizeKw = when (pageConfig.size) {
        PageSize.A4 -> "A4"
        PageSize.LETTER -> "letter"
        PageSize.LEGAL -> "legal"
        else -> "A4"
    }
    val pageCss = "@page { size: $sizeKw $orientation; margin: ${pageConfig.margins.top}mm ${pageConfig.margins.right}mm ${pageConfig.margins.bottom}mm ${pageConfig.margins.left}mm; }"
    return replace("/* PAGE_CONFIG_PLACEHOLDER */", pageCss)
}
