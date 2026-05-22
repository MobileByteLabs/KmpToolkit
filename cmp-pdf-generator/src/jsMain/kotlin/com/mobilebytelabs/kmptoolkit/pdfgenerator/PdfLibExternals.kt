/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)
@file:JsModule("pdf-lib")
@file:JsNonModule

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.js.Promise

/**
 * JS interop declarations for `pdf-lib` (npm). Used by the JS impl for the DSL / ByteArray route.
 * Consumer apps must have `pdf-lib` resolvable in the npm tree — Gradle handles this when
 * `kotlin.js { browser() }` or `nodejs()` is configured.
 */
@JsName("PDFDocument")
external class PDFDocumentJs {
    fun addPage(size: Array<Double>? = definedExternally): PDFPageJs

    fun embedFont(fontBytes: dynamic): Promise<dynamic>

    fun embedJpg(bytes: dynamic): Promise<dynamic>

    fun embedPng(bytes: dynamic): Promise<dynamic>

    // Returns Uint8Array
    fun save(): Promise<dynamic>

    companion object {
        fun create(): Promise<PDFDocumentJs>
    }
}

@JsName("PDFPage")
external class PDFPageJs {
    fun drawText(text: String, options: dynamic = definedExternally)

    fun drawImage(image: dynamic, options: dynamic = definedExternally)

    fun drawLine(options: dynamic)

    fun drawRectangle(options: dynamic)

    fun getSize(): dynamic
}

@JsName("StandardFonts")
external object StandardFontsJs {
    val Helvetica: dynamic
    val HelveticaBold: dynamic
    val HelveticaOblique: dynamic
    val TimesRoman: dynamic
    val Courier: dynamic
}

@JsName("rgb")
external fun rgbJs(r: Double, g: Double, b: Double): dynamic
