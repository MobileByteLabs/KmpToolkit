/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MarkdownAdapterTest {
    @Test
    fun headingsCompileToHtml() =
        runTest {
            val md = "# H1\n\n## H2\n\nbody"
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.contains("<h1>") || html.contains("<h1 "), "expected h1 tag")
            assertTrue(html.contains("<h2>") || html.contains("<h2 "), "expected h2 tag")
        }

    @Test
    fun boldAndItalicCompile() =
        runTest {
            val md = "This is **bold** and *italic*."
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.contains("<strong>") || html.contains("<b>"))
            assertTrue(html.contains("<em>") || html.contains("<i>"))
        }

    @Test
    fun listsCompile() =
        runTest {
            val md = "- one\n- two\n- three"
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.contains("<ul"))
            assertTrue(html.contains("<li"))
        }

    @Test
    fun gfmTablesCompile() =
        runTest {
            val md =
                """
                | A | B |
                |---|---|
                | 1 | 2 |
                | 3 | 4 |
                """.trimIndent()
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.contains("<table"))
            assertTrue(html.contains("<th"))
        }

    @Test
    fun codeBlocksCompile() =
        runTest {
            val md = "```kotlin\nval x = 1\n```"
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.contains("<pre"))
            assertTrue(html.contains("val x = 1"))
        }

    @Test
    fun outputWrappedInXhtml() =
        runTest {
            val md = "hello"
            val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.none())
            assertTrue(html.startsWith("<!DOCTYPE html"))
            assertTrue(html.contains("<title>"))
        }
}
