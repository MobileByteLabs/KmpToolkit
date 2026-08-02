/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(
    ExperimentalPdfGeneratorApi::class,
    kotlin.io.encoding.ExperimentalEncodingApi::class,
)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.html.BODY
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.img
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlin.io.encoding.Base64
import kotlin.time.Clock

/**
 * Base class for HTML-based PDF templates.
 *
 * Subclass to provide [getTitle] and [BODY.generateBody]; the base supplies common chrome
 * (header logo, generation date, footer with "powered by", consistent table/heading CSS,
 * `@page` CSS placeholder for platform engines to fill in).
 *
 * Fully de-branded — all logo / footer text / colors / fonts come from [branding].
 *
 * @param branding Injected branding bundle. Use [PdfBranding.none] to omit logo + footer.
 */
@ExperimentalPdfGeneratorApi
public abstract class HtmlTemplateGenerator(protected val branding: PdfBranding) {
    /**
     * Generate the complete XHTML 1.0-strict document, ready to feed the platform's
     * HTML-to-PDF engine.
     */
    public suspend fun generateHtml(): String {
        val logoUri = getLogoDataUri()
        val poweredBy = getPoweredByText()
        val generationDateText = getGenerationDateText()
        val theme = branding.theme

        val bodyContent =
            createHTML(xhtmlCompatible = true).body {
                // Optional header — only when logo OR date is present
                if (logoUri != null) {
                    renderHeader(logoUri, generationDateText, theme.accentColorHex)
                }

                generateBody()

                // Optional footer — only when poweredByText is non-null
                if (poweredBy != null) {
                    renderFooter(logoUri, poweredBy, theme.accentColorHex)
                }
            }

        return buildString {
            append("<!DOCTYPE html PUBLIC ")
            append("\"-//W3C//DTD XHTML 1.0 Strict//EN\" ")
            append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
            append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n")
            append("<head>\n")
            append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n")
            append("    <title>").append(getTitle()).append("</title>\n")
            append("    <style type=\"text/css\">\n")
            append(getCommonStyles())
            append("\n")
            append(getAdditionalStyles())
            append("    </style>\n")
            append("</head>\n")
            append(bodyContent)
            append("</html>\n")
        }
    }

    /** Title for the `<title>` element. */
    protected abstract fun getTitle(): String

    /** Subclass-provided body content. */
    protected abstract fun BODY.generateBody()

    /** Subclass-overridable CSS appended after [getCommonStyles]. */
    protected open fun getAdditionalStyles(): String = ""

    /** "Powered by" text from branding. Null = no footer. */
    protected fun getPoweredByText(): String? = branding.poweredByText

    /**
     * Convert the configured logo to a `data:image/...;base64,...` URI.
     * Returns null when [PdfBranding.logo] is [PdfLogo.None].
     */
    protected fun getLogoDataUri(): String? = when (val logo = branding.logo) {
        is PdfLogo.None -> null
        is PdfLogo.DataUri -> logo.uri
        is PdfLogo.Svg -> "data:image/svg+xml;base64," + Base64.encode(logo.bytes)
        is PdfLogo.Png -> "data:image/png;base64," + Base64.encode(logo.bytes)
    }

    /** Today's date, formatted via [PdfBranding.dateFormatter]. */
    protected fun getGenerationDateText(): String {
        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        return "Generated: " + branding.dateFormatter(today)
    }

    /**
     * Default CSS — typography, layout, table styling, print rules, watermark + `@page`
     * placeholder. Colors come from `branding.theme`.
     */
    protected fun getCommonStyles(): String {
        val t = branding.theme
        val watermarkCss =
            branding.watermark?.let { wm ->
                val src =
                    when (wm.image) {
                        is ImageSource.Bytes -> "data:image/png;base64," + Base64.encode(wm.image.bytes)
                        is ImageSource.DataUri -> wm.image.uri
                        is ImageSource.Url -> wm.image.url
                        null -> null
                        else -> null
                    }
                """
                body::before {
                    content: ${if (wm.text != null) "\"${wm.text}\"" else "\"\""};
                    position: fixed;
                    top: 50%; left: 50%;
                    transform: translate(-50%, -50%) rotate(${wm.rotationDeg}deg);
                    font-size: 80pt;
                    color: #000;
                    opacity: ${wm.opacity};
                    z-index: 1000;
                    pointer-events: none;
                    ${watermarkBackgroundCss(src)}
                }
                """.trimIndent()
            } ?: ""

        return """
            * { margin: 0; padding: 0; box-sizing: border-box; }

            body {
                font-family: ${t.fontFamily};
                font-size: ${(8 * t.fontScale).toInt()}pt;
                line-height: 1.3;
                color: #333;
                background: #fff;
            }

            .container { padding: 10px; max-width: 100%; }

            .header {
                margin-bottom: 12px;
                padding-bottom: 10px;
                border-bottom: 2px solid ${t.accentColorHex};
            }

            h1 { color: ${t.headerColorHex}; font-size: ${(14 * t.fontScale).toInt()}pt;
                 font-weight: 500; margin-bottom: 10px; }
            h2 { color: ${t.headerColorHex}; font-size: ${(11 * t.fontScale).toInt()}pt;
                 font-weight: 500; margin-bottom: 8px; }
            h3 { color: ${t.headerColorHex}; font-size: ${(10 * t.fontScale).toInt()}pt;
                 font-weight: 500; margin-bottom: 6px; }

            table { width: 100%; border-collapse: collapse; margin-bottom: 15px;
                    font-size: ${(6 * t.fontScale).toInt()}pt; }
            th, td { border: 1px solid ${t.borderColorHex}; padding: 6px 8px; text-align: left; }
            thead th { background-color: #f5f5f5; font-weight: 600; color: #424242; }
            thead tr.header-group th { background-color: #e3f2fd; font-weight: 600;
                                       border-bottom: 2px solid ${t.headerColorHex}; }
            tbody tr:nth-child(even) { background-color: ${t.tableRowEvenHex}; }
            tfoot td { background-color: #e3f2fd; font-weight: 600;
                       border-top: 2px solid ${t.headerColorHex}; }

            .center { text-align: center !important; }
            .right { text-align: right !important; }
            .r-amount { text-align: right !important; }

            .footer { margin-top: 20px; padding-top: 15px; text-align: center;
                      font-size: ${(8 * t.fontScale).toInt()}pt; color: #666; }
            .powered-by { margin-top: 5px; font-style: italic; }

            a { color: ${t.accentColorHex}; text-decoration: none; }
            hr { border: none; border-top: 1px solid ${t.borderColorHex}; margin: 8px 0; }

            @media print {
                body { background: #fff; }
                .container { padding: 0; }
                table { page-break-inside: auto; }
                tr { page-break-inside: avoid; page-break-after: auto; }
            }

            $watermarkCss

            /* PAGE_CONFIG_PLACEHOLDER */
        """.trimIndent()
    }

    private fun BODY.renderHeader(logoUri: String, dateText: String, accentColor: String) {
        div {
            attributes["style"] = "display: table; width: 100%; " +
                "border-bottom: 2px solid $accentColor; margin-bottom: 20px; padding-bottom: 10px;"
            div {
                attributes["style"] = "display: table-cell; text-align: left; " +
                    "vertical-align: bottom; width: 50%;"
                img(src = logoUri, alt = "logo") {
                    attributes["style"] = "width:129px; height:36px;"
                }
            }
            div {
                attributes["style"] = "display: table-cell; text-align: right; " +
                    "vertical-align: bottom; width: 50%; color: #555; font-size: 8pt;"
                +dateText
            }
        }
    }

    private fun BODY.renderFooter(logoUri: String?, poweredBy: String, accentColor: String) {
        div("footer") {
            div("powered-by center") {
                span {
                    attributes["style"] = "color: $accentColor; font-weight: bold; " +
                        "vertical-align: middle; display: inline-block;"
                    +poweredBy
                }
                if (logoUri != null) {
                    img(src = logoUri, alt = "logo") {
                        attributes["style"] = "width:50px; height:17px; vertical-align:middle; " +
                            "margin:0 6px; display: inline-block;"
                    }
                }
            }
        }
    }

    private fun watermarkBackgroundCss(src: String?): String = if (src != null) {
        "background-image: url('$src'); background-size: contain; " +
            "background-repeat: no-repeat; width: 300pt; height: 300pt;"
    } else {
        ""
    }
}
