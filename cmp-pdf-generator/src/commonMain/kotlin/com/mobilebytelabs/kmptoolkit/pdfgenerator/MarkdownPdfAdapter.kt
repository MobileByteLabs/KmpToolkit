/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

/**
 * Convert Markdown to a PDF-ready HTML string.
 *
 * Uses `org.intellij.markdown` with GFM (GitHub-Flavored Markdown) flavor. Supports:
 *  - headings (h1-h6)
 *  - paragraphs, bold, italic
 *  - lists (ordered + unordered, nested)
 *  - code blocks (rendered as `<pre>` with monospace styling)
 *  - GFM tables
 *  - hyperlinks
 *  - images (URL only)
 *
 * The output is wrapped in a [HtmlTemplateGenerator] for chrome (logo / footer).
 */
@ExperimentalPdfGeneratorApi
public object MarkdownPdfAdapter {
    /**
     * Compile Markdown to a complete HTML document, ready for `PdfGenerator.generateFromHtml`.
     *
     * @param markdown Source Markdown.
     * @param branding Branding bundle (for header/footer chrome).
     */
    public suspend fun markdownToHtml(
        markdown: String,
        branding: PdfBranding,
    ): String {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        val inner = HtmlGenerator(markdown, parsedTree, flavour).generateHtml()
        return MarkdownTemplate(branding, inner).generateHtml()
    }
}

/**
 * `HtmlTemplateGenerator` that wraps a Markdown-compiled HTML fragment in the standard chrome.
 */
@ExperimentalPdfGeneratorApi
private class MarkdownTemplate(
    branding: PdfBranding,
    private val innerHtml: String,
) : HtmlTemplateGenerator(branding) {
    override fun getTitle(): String = "Markdown document"

    override fun getAdditionalStyles(): String =
        """
        pre {
            background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;
            font-family: 'Roboto Mono', 'Courier New', monospace; font-size: 7pt;
        }
        code {
            background: #f5f5f5; padding: 1px 4px; border-radius: 3px;
            font-family: 'Roboto Mono', 'Courier New', monospace;
        }
        blockquote {
            border-left: 4px solid ${branding.theme.accentColorHex};
            margin: 10px 0; padding-left: 12px; color: #555;
        }
        ul, ol { margin: 8px 0 8px 20px; }
        li { margin-bottom: 4px; }
        """.trimIndent()

    override fun kotlinx.html.BODY.generateBody() {
        // The compiled HTML already contains a `<body>` from HtmlGenerator — strip it and inject raw.
        kotlinx.html.div {
            attributes["class"] = "container markdown-body"
            kotlinx.html.unsafe {
                +stripBodyTags(innerHtml)
            }
        }
    }

    private fun stripBodyTags(html: String): String =
        html
            .replace(Regex("^<body[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</body>\$", RegexOption.IGNORE_CASE), "")
}
