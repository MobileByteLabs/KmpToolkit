/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PdfErrorTest {

    @Test
    fun toPdfErrorIdentityOnPdfError() {
        val original: PdfError = PdfError.InvalidInput("test")
        assertEquals(original, original.toPdfError())
    }

    @Test
    fun toPdfErrorMapsCancellationException() {
        val cancelled: Throwable = CancellationException("cancelled")
        assertEquals(PdfError.CancellationError, cancelled.toPdfError())
    }

    @Test
    fun toPdfErrorWrapsGenericThrowable() {
        val ex: Throwable = IllegalStateException("boom")
        val wrapped = ex.toPdfError()
        assertIs<PdfError.EngineFailure>(wrapped)
        assertEquals(ex, wrapped.cause)
    }

    @Test
    fun unsupportedPlatformMessageContainsPlatformName() {
        val err = PdfError.UnsupportedPlatform("tvOS")
        assertTrue(err.message!!.contains("tvOS"))
        assertTrue(err.message!!.contains("pdf-tier3-tvOS"))
    }

    @Test
    fun engineFailureMessageContainsCauseInfo() {
        val cause = IllegalStateException("under-water")
        val err = PdfError.EngineFailure(cause)
        assertTrue(err.message!!.contains("under-water") || err.message!!.contains("IllegalStateException"))
    }
}
