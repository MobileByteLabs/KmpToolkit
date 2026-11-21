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

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The JVM HTML engine (OpenHTMLToPDF, LGPL) is opt-in: present → render, absent → a typed error. */
class JvmHtmlEngineTest {

    @Test
    fun engineOnTheClasspathIsDetected() {
        // jvmTest adds the engine, standing in for an app that opted in.
        assertTrue(JvmHtmlEngine.isAvailable())
    }

    @Test
    fun withoutTheEngineTheHtmlRouteFailsNamingWhatToAdd() {
        assertFalse(JvmHtmlEngine.isAvailable("no.such.Renderer"))
        val error = assertFailsWith<PdfError.UnsupportedFeature> {
            JvmHtmlEngine.render("<html/>", ByteArrayOutputStream(), rendererClass = "no.such.Renderer")
        }
        assertTrue("com.openhtmltopdf:openhtmltopdf-pdfbox" in error.feature)
        assertTrue("com.openhtmltopdf:openhtmltopdf-svg-support" in error.feature)
    }
}
