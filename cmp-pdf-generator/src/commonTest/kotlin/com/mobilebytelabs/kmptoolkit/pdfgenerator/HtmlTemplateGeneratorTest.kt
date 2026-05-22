/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.html.BODY
import kotlinx.html.h1
import kotlinx.html.p

class HtmlTemplateGeneratorTest {
    private class TestTemplate(branding: PdfBranding) : HtmlTemplateGenerator(branding) {
        override fun getTitle() = "Test"

        override fun BODY.generateBody() {
            h1 { +"Hello" }
            p { +"World" }
        }
    }

    @Test
    fun emitsXhtmlStrictDoctype() = runTest {
        val html = TestTemplate(PdfBranding.none()).generateHtml()
        assertTrue(html.startsWith("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""))
        assertTrue(html.contains("<html xmlns=\"http://www.w3.org/1999/xhtml\">"))
    }

    @Test
    fun emitsTitleFromGetTitle() = runTest {
        val html = TestTemplate(PdfBranding.none()).generateHtml()
        assertTrue(html.contains("<title>Test</title>"))
    }

    @Test
    fun pageConfigPlaceholderPresent() = runTest {
        val html = TestTemplate(PdfBranding.none()).generateHtml()
        assertTrue(html.contains("/* PAGE_CONFIG_PLACEHOLDER */"))
    }

    @Test
    fun noLogoNoFooterWithBrandingNone() = runTest {
        val html = TestTemplate(PdfBranding.none()).generateHtml()
        assertFalse(html.contains("<img"), "PdfBranding.none should not emit logo img")
        assertFalse(html.contains("powered-by"), "PdfBranding.none should not emit footer")
    }

    @Test
    fun footerEmittedWhenPoweredByPresent() = runTest {
        val html = TestTemplate(PdfBranding.default()).generateHtml()
        assertTrue(html.contains("powered-by"))
        assertTrue(html.contains("KmpToolkit"))
    }

    @Test
    fun deBrandedNoMifosReferencesByDefault() = runTest {
        val html = TestTemplate(PdfBranding.none()).generateHtml()
        assertFalse(html.contains("mifos", ignoreCase = true), "default output must contain no 'mifos' references")
        assertFalse(html.contains("ic_icon_mifos"), "no mifos logo refs")
    }

    @Test
    fun mifosDefaultBrandingReintroducesMifosColors() = runTest {
        val html = TestTemplate(PdfBranding.mifosDefault()).generateHtml()
        assertTrue(html.contains("#33618D"))
        assertTrue(html.contains("Powered by Mifos"))
    }

    @Test
    fun watermarkInjectsCss() = runTest {
        val branding =
            PdfBranding.default().copy(
                watermark = Watermark(text = "DRAFT", opacity = 0.2f, rotationDeg = -30),
            )
        val html = TestTemplate(branding).generateHtml()
        assertTrue(html.contains("DRAFT"))
        assertTrue(html.contains("rotate(-30deg)"))
    }
}
