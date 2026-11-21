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

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import java.io.OutputStream

/**
 * The HTML → PDF engine on the JVM: OpenHTMLToPDF, which is LGPL-2.1-or-later.
 *
 * It is a `compileOnly` dependency of this module, so it is NOT pulled into an app that depends on
 * `cmp-pdf-generator` — an Apache-2.0 library should not hand every consumer an LGPL obligation for
 * a route most of them never call. An app that wants HTML → PDF on desktop adds the two artifacts
 * itself (see the module README) and so takes on the licence knowingly. The DSL route (PDFBox,
 * Apache-2.0) needs nothing extra.
 */
internal object JvmHtmlEngine {
    private const val RENDERER = "com.openhtmltopdf.pdfboxout.PdfRendererBuilder"

    private const val MISSING =
        "HTML → PDF on the JVM needs OpenHTMLToPDF, which this library does not bundle because it is " +
            "LGPL-licensed. Add com.openhtmltopdf:openhtmltopdf-pdfbox and " +
            "com.openhtmltopdf:openhtmltopdf-svg-support to your JVM dependencies, or build the " +
            "document with the pdf { } DSL, which needs neither."

    fun isAvailable(rendererClass: String = RENDERER): Boolean =
        runCatching { Class.forName(rendererClass, false, JvmHtmlEngine::class.java.classLoader) }.isSuccess

    fun render(html: String, out: OutputStream, rendererClass: String = RENDERER) {
        if (!isAvailable(rendererClass)) throw PdfError.UnsupportedFeature(MISSING)
        OpenHtmlToPdf.render(html, out)
    }
}

// The only code that links against OpenHTMLToPDF. Kept in its own class so the JVM resolves those
// types only once render() is reached, never when PdfGenerator itself loads.
private object OpenHtmlToPdf {
    fun render(html: String, out: OutputStream) {
        PdfRendererBuilder()
            .useSVGDrawer(BatikSVGDrawer())
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(out)
            .run()
    }
}
