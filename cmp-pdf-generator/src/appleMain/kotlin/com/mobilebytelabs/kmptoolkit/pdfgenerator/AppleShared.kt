/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    ExperimentalPdfGeneratorApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

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
internal fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

// injectPageConfigCss moved to commonMain (PageConfigCssInjection.kt) — internal visibility
